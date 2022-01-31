/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.security.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoints;
import org.springframework.boot.security.servlet.ApplicationContextRequestMatcher;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

/**
 * Factory that can be used to create a {@link RequestMatcher} for actuator endpoint
 * locations.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Chris Bono
 * @since 2.0.0
 */
public final class EndpointRequest {

	private static final RequestMatcher EMPTY_MATCHER = (request) -> false;

	private EndpointRequest() {
	}

	/**
	 * Returns a matcher that includes all {@link Endpoint actuator endpoints}. It also
	 * includes the links endpoint which is present at the base path of the actuator
	 * endpoints. The {@link EndpointRequestMatcher#excluding(Class...) excluding} method
	 * can be used to further remove specific endpoints if required. For example:
	 * <pre class="code">
	 * EndpointRequest.toAnyEndpoint().excluding(ShutdownEndpoint.class)
	 * </pre>
	 * @return the configured {@link RequestMatcher}
	 */
	public static EndpointRequestMatcher toAnyEndpoint() {
		return new EndpointRequestMatcher(Collections.emptyList(), Collections.emptyList(), true, null);
	}

	/**
	 * Returns a matcher that includes the specified {@link Endpoint actuator endpoints}.
	 * For example: <pre class="code">
	 * EndpointRequest.to(ShutdownEndpoint.class, HealthEndpoint.class)
	 * </pre>
	 * @param endpoints the endpoints to include
	 * @return the configured {@link RequestMatcher}
	 */
	public static EndpointRequestMatcher to(Class<?>... endpoints) {
		return new EndpointRequestMatcher(Arrays.asList((Object[]) endpoints), Collections.emptyList(), false, null);
	}

	/**
	 * Returns a matcher that includes the specified {@link Endpoint actuator endpoints}.
	 * For example: <pre class="code">
	 * EndpointRequest.to("shutdown", "health")
	 * </pre>
	 * @param endpoints the endpoints to include
	 * @return the configured {@link RequestMatcher}
	 */
	public static EndpointRequestMatcher to(String... endpoints) {
		return new EndpointRequestMatcher(Arrays.asList((Object[]) endpoints), Collections.emptyList(), false, null);
	}

	/**
	 * Returns a matcher that matches only on the links endpoint. It can be used when
	 * security configuration for the links endpoint is different from the other
	 * {@link Endpoint actuator endpoints}. The
	 * {@link EndpointRequestMatcher#excludingLinks() excludingLinks} method can be used
	 * in combination with this to remove the links endpoint from
	 * {@link EndpointRequest#toAnyEndpoint() toAnyEndpoint}. For example:
	 * <pre class="code">
	 * EndpointRequest.toLinks()
	 * </pre>
	 * @return the configured {@link RequestMatcher}
	 */
	public static LinksRequestMatcher toLinks() {
		return new LinksRequestMatcher();
	}

	/**
	 * Base class for supported request matchers.
	 */
	private abstract static class AbstractRequestMatcher
			extends ApplicationContextRequestMatcher<WebApplicationContext> {

		private volatile RequestMatcher delegate;

		private ManagementPortType managementPortType;

		AbstractRequestMatcher() {
			super(WebApplicationContext.class);
		}

		@Override
		protected boolean ignoreApplicationContext(WebApplicationContext applicationContext) {
			if (this.managementPortType == null) {
				this.managementPortType = ManagementPortType.get(applicationContext.getEnvironment());
			}
			return this.managementPortType == ManagementPortType.DIFFERENT
					&& !WebServerApplicationContext.hasServerNamespace(applicationContext, "management");
		}

		@Override
		protected final void initialized(Supplier<WebApplicationContext> context) {
			this.delegate = createDelegate(context.get());
		}

		private RequestMatcher createDelegate(WebApplicationContext context) {
			try {
				EndpointRequestMatcherProvider requestMatcherProvider = getRequestMatcherProvider(context);
				return createDelegate(context, requestMatcherProvider);
			}
			catch (NoSuchBeanDefinitionException ex) {
				return EMPTY_MATCHER;
			}
		}

		private EndpointRequestMatcherProvider getRequestMatcherProvider(WebApplicationContext context) {
			try {
				return context.getBean(EndpointRequestMatcherProvider.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				return getFallbackRequestMatcherProvider(context);
			}
		}

		@SuppressWarnings("deprecation")
		private EndpointRequestMatcherProvider getFallbackRequestMatcherProvider(WebApplicationContext context) {
			try {
				org.springframework.boot.autoconfigure.security.servlet.RequestMatcherProvider bean = context
						.getBean(org.springframework.boot.autoconfigure.security.servlet.RequestMatcherProvider.class);
				return (path, httpMethod) -> bean.getRequestMatcher(path);
			}
			catch (NoSuchBeanDefinitionException ex) {
				return (path, httpMethod) -> new AntPathRequestMatcher(path,
						(httpMethod != null) ? httpMethod.name() : null);
			}
		}

		protected abstract RequestMatcher createDelegate(WebApplicationContext context,
				EndpointRequestMatcherProvider requestMatcherProvider);

		@Override
		protected final boolean matches(HttpServletRequest request, Supplier<WebApplicationContext> context) {
			return this.delegate.matches(request);
		}

		protected final List<RequestMatcher> getLinksMatchers(EndpointRequestMatcherProvider requestMatcherProvider,
				String basePath) {
			List<RequestMatcher> linksMatchers = new ArrayList<>();
			linksMatchers.add(getRequestMatcher(requestMatcherProvider, null, basePath));
			linksMatchers.add(getRequestMatcher(requestMatcherProvider, null, basePath, "/"));
			return linksMatchers;
		}

		protected final RequestMatcher getRequestMatcher(EndpointRequestMatcherProvider provider, HttpMethod httpMethod,
				String... parts) {
			StringBuilder pattern = new StringBuilder();
			for (String part : parts) {
				pattern.append(part);
			}
			return provider.getRequestMatcher(pattern.toString(), httpMethod);
		}

	}

	/**
	 * The request matcher used to match against {@link Endpoint actuator endpoints}.
	 */
	public static final class EndpointRequestMatcher extends AbstractRequestMatcher {

		private final List<Object> includes;

		private final List<Object> excludes;

		private final boolean includeLinks;

		private final HttpMethod httpMethod;

		private EndpointRequestMatcher(List<Object> includes, List<Object> excludes, boolean includeLinks,
				HttpMethod httpMethod) {
			this.includes = includes;
			this.excludes = excludes;
			this.includeLinks = includeLinks;
			this.httpMethod = httpMethod;
		}

		public EndpointRequestMatcher excluding(Class<?>... endpoints) {
			List<Object> excludes = new ArrayList<>(this.excludes);
			excludes.addAll(Arrays.asList((Object[]) endpoints));
			return new EndpointRequestMatcher(this.includes, excludes, this.includeLinks, this.httpMethod);
		}

		public EndpointRequestMatcher excluding(String... endpoints) {
			List<Object> excludes = new ArrayList<>(this.excludes);
			excludes.addAll(Arrays.asList((Object[]) endpoints));
			return new EndpointRequestMatcher(this.includes, excludes, this.includeLinks, this.httpMethod);
		}

		public EndpointRequestMatcher excludingLinks() {
			return new EndpointRequestMatcher(this.includes, this.excludes, false, this.httpMethod);
		}

		/**
		 * Restricts the matcher to only consider requests with a particular HTTP method.
		 * @param httpMethod the HTTP method to include
		 * @return a copy of the matcher further restricted to only match requests with
		 * the specified HTTP method
		 * @since 2.7.0
		 */
		public EndpointRequestMatcher withHttpMethod(HttpMethod httpMethod) {
			return new EndpointRequestMatcher(this.includes, this.excludes, this.includeLinks, httpMethod);
		}

		@Override
		protected RequestMatcher createDelegate(WebApplicationContext context,
				EndpointRequestMatcherProvider requestMatcherProvider) {
			PathMappedEndpoints pathMappedEndpoints = context.getBean(PathMappedEndpoints.class);
			Set<String> paths = new LinkedHashSet<>();
			if (this.includes.isEmpty()) {
				paths.addAll(pathMappedEndpoints.getAllPaths());
			}
			streamPaths(this.includes, pathMappedEndpoints).forEach(paths::add);
			streamPaths(this.excludes, pathMappedEndpoints).forEach(paths::remove);
			List<RequestMatcher> delegateMatchers = getDelegateMatchers(requestMatcherProvider, paths);
			String basePath = pathMappedEndpoints.getBasePath();
			if (this.includeLinks && StringUtils.hasText(basePath)) {
				delegateMatchers.addAll(getLinksMatchers(requestMatcherProvider, basePath));
			}
			return new OrRequestMatcher(delegateMatchers);
		}

		private Stream<String> streamPaths(List<Object> source, PathMappedEndpoints pathMappedEndpoints) {
			return source.stream().filter(Objects::nonNull).map(this::getEndpointId).map(pathMappedEndpoints::getPath);
		}

		private EndpointId getEndpointId(Object source) {
			if (source instanceof EndpointId) {
				return (EndpointId) source;
			}
			if (source instanceof String) {
				return (EndpointId.of((String) source));
			}
			if (source instanceof Class) {
				return getEndpointId((Class<?>) source);
			}
			throw new IllegalStateException("Unsupported source " + source);
		}

		private EndpointId getEndpointId(Class<?> source) {
			MergedAnnotation<Endpoint> annotation = MergedAnnotations.from(source).get(Endpoint.class);
			Assert.state(annotation.isPresent(), () -> "Class " + source + " is not annotated with @Endpoint");
			return EndpointId.of(annotation.getString("id"));
		}

		private List<RequestMatcher> getDelegateMatchers(EndpointRequestMatcherProvider requestMatcherProvider,
				Set<String> paths) {
			return paths.stream().map((path) -> getRequestMatcher(requestMatcherProvider, this.httpMethod, path, "/**"))
					.collect(Collectors.toList());
		}

	}

	/**
	 * The request matcher used to match against the links endpoint.
	 */
	public static final class LinksRequestMatcher extends AbstractRequestMatcher {

		@Override
		protected RequestMatcher createDelegate(WebApplicationContext context,
				EndpointRequestMatcherProvider requestMatcherProvider) {
			WebEndpointProperties properties = context.getBean(WebEndpointProperties.class);
			String basePath = properties.getBasePath();
			if (StringUtils.hasText(basePath)) {
				return new OrRequestMatcher(getLinksMatchers(requestMatcherProvider, basePath));
			}
			return EMPTY_MATCHER;
		}

	}

}
