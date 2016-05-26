/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplateHandler;

/**
 * Builder that can be used to configure and create a {@link RestTemplate}. Provides
 * convenience methods to register {@link #messageConverters(HttpMessageConverter...)
 * converters}, {@link #errorHandler(ResponseErrorHandler) error handlers} and
 * {@link #uriTemplateHandler(UriTemplateHandler) UriTemplateHandlers}.
 * <p>
 * By default the built {@link RestTemplate} will attempt to use the most suitable
 * {@link ClientHttpRequestFactory}, call {@link #detectRequestFactory(boolean)
 * detectRequestFactory(false)} if you prefer to keep the default. In a typical
 * auto-configured Spring Boot application this builder is available as a bean and can be
 * injected whenever a {@link RestTemplate} is needed.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.4.0
 */
public class RestTemplateBuilder {

	private static final Map<String, String> REQUEST_FACTORY_CANDIDATES;

	static {
		Map<String, String> candidates = new LinkedHashMap<String, String>();
		candidates.put("org.apache.http.client.HttpClient",
				"org.springframework.http.client.HttpComponentsClientHttpRequestFactory");
		candidates.put("okhttp3.OkHttpClient",
				"org.springframework.http.client.OkHttp3ClientHttpRequestFactory");
		candidates.put("com.squareup.okhttp.OkHttpClient",
				"org.springframework.http.client.OkHttpClientHttpRequestFactory");
		candidates.put("io.netty.channel.EventLoopGroup",
				"org.springframework.http.client.Netty4ClientHttpRequestFactory");
		REQUEST_FACTORY_CANDIDATES = Collections.unmodifiableMap(candidates);
	}

	private boolean detectRequestFactory = true;

	private String rootUri;

	private Set<HttpMessageConverter<?>> messageConverters = new LinkedHashSet<HttpMessageConverter<?>>();

	private ClientHttpRequestFactory requestFactory;

	private UriTemplateHandler uriTemplateHandler;

	private ResponseErrorHandler errorHandler;

	private BasicAuthorizationInterceptor basicAuthorization;

	private Set<RestTemplateCustomizer> customizers;

	/**
	 * Create a new {@link RestTemplateBuilder} instance.
	 * @param customizers any {@link RestTemplateCustomizer RestTemplateCustomizers} that
	 * should be applied when the {@link RestTemplate} is built
	 */
	public RestTemplateBuilder(RestTemplateCustomizer... customizers) {
		customizers(customizers);
	}

	/**
	 * Set if the {@link ClientHttpRequestFactory} should be detected based on the
	 * classpath. Default if {@code true}.
	 * @param detectRequestFactory if the {@link ClientHttpRequestFactory} should be
	 * detected
	 * @return this builder
	 */
	public RestTemplateBuilder detectRequestFactory(boolean detectRequestFactory) {
		this.detectRequestFactory = detectRequestFactory;
		return this;
	}

	/**
	 * Set a root URL that should be applied to each request that starts with {@code '/'}.
	 * See {@link RootUriTemplateHandler} for details.
	 * @param rootUri the root URI or {@code null}
	 * @return this builder
	 */
	public RestTemplateBuilder rootUri(String rootUri) {
		this.rootUri = rootUri;
		return this;
	}

	/**
	 * Set the {@link HttpMessageConverter HttpMessageConverters} that should be used with
	 * the {@link RestTemplate}. Setting this value will replace any previously calls.
	 * @param messageConverters the converters to set
	 * @return this builder
	 * @see #additionalMessageConverters(HttpMessageConverter...)
	 */
	public RestTemplateBuilder messageConverters(
			HttpMessageConverter<?>... messageConverters) {
		Assert.notNull(messageConverters, "MessageConverters must not be null");
		return messageConverters(Arrays.asList(messageConverters));
	}

	/**
	 * Set the {@link HttpMessageConverter HttpMessageConverters} that should be used with
	 * the {@link RestTemplate}. Setting this value will replace any previously calls.
	 * @param messageConverters the converters to set
	 * @return this builder
	 * @see #additionalMessageConverters(HttpMessageConverter...)
	 */
	public RestTemplateBuilder messageConverters(
			Collection<? extends HttpMessageConverter<?>> messageConverters) {
		Assert.notNull(messageConverters, "MessageConverters must not be null");
		this.messageConverters = new LinkedHashSet<HttpMessageConverter<?>>(
				messageConverters);
		return this;
	}

	/**
	 * Add additional {@link HttpMessageConverter HttpMessageConverters} that should be
	 * used with the {@link RestTemplate}.
	 * @param messageConverters the converters to add
	 * @return this builder
	 * @see #messageConverters(HttpMessageConverter...)
	 */
	public RestTemplateBuilder additionalMessageConverters(
			HttpMessageConverter<?>... messageConverters) {
		Assert.notNull(messageConverters, "MessageConverters must not be null");
		return additionalMessageConverters(Arrays.asList(messageConverters));
	}

	/**
	 * Add additional {@link HttpMessageConverter HttpMessageConverters} that should be
	 * used with the {@link RestTemplate}.
	 * @param messageConverters the converters to add
	 * @return this builder
	 * @see #messageConverters(HttpMessageConverter...)
	 */
	public RestTemplateBuilder additionalMessageConverters(
			Collection<? extends HttpMessageConverter<?>> messageConverters) {
		Assert.notNull(messageConverters, "MessageConverters must not be null");
		this.messageConverters.addAll(messageConverters);
		return this;
	}

	/**
	 * Set the {@link HttpMessageConverter HttpMessageConverters} that should be used with
	 * the {@link RestTemplate} to the default set. Calling this method will replace any
	 * previously defined converters.
	 * @return this builder
	 * @see #messageConverters(HttpMessageConverter...)
	 */
	public RestTemplateBuilder defaultMessageConverters() {
		this.messageConverters = new LinkedHashSet<HttpMessageConverter<?>>(
				new RestTemplate().getMessageConverters());
		return this;
	}

	/**
	 * Set the {@link ClientHttpRequestFactory} class that should be used with the
	 * {@link RestTemplate}.
	 * @param requestFactory the request factory to use
	 * @return this builder
	 */
	public RestTemplateBuilder requestFactory(
			Class<? extends ClientHttpRequestFactory> requestFactory) {
		Assert.notNull(requestFactory, "RequestFactory must not be null");
		return requestFactory(BeanUtils.instantiate(requestFactory));
	}

	/**
	 * Set the {@link ClientHttpRequestFactory} that should be used with the
	 * {@link RestTemplate}.
	 * @param requestFactory the request factory to use
	 * @return this builder
	 */
	public RestTemplateBuilder requestFactory(ClientHttpRequestFactory requestFactory) {
		Assert.notNull(requestFactory, "RequestFactory must not be null");
		this.requestFactory = requestFactory;
		return this;
	}

	/**
	 * Set the {@link UriTemplateHandler} that should be used with the
	 * {@link RestTemplate}.
	 * @param uriTemplateHandler the URI template handler to use
	 * @return this builder
	 */
	public RestTemplateBuilder uriTemplateHandler(UriTemplateHandler uriTemplateHandler) {
		Assert.notNull(uriTemplateHandler, "UriTemplateHandler must not be null");
		this.uriTemplateHandler = uriTemplateHandler;
		return this;
	}

	/**
	 * Set the {@link ResponseErrorHandler} that should be used with the
	 * {@link RestTemplate}.
	 * @param errorHandler the error hander to use
	 * @return this builder
	 */
	public RestTemplateBuilder errorHandler(ResponseErrorHandler errorHandler) {
		Assert.notNull(errorHandler, "ErrorHandler must not be null");
		this.errorHandler = errorHandler;
		return this;
	}

	/**
	 * Add HTTP basic authentication to requests. See
	 * {@link BasicAuthorizationInterceptor} for details.
	 * @param username the user name
	 * @param password the password
	 * @return this builder
	 */
	public RestTemplateBuilder basicAuthorization(String username, String password) {
		this.basicAuthorization = new BasicAuthorizationInterceptor(username, password);
		return this;
	}

	/**
	 * Set the {@link HttpMessageConverter HttpMessageConverters} that should be applied
	 * to the {@link RestTemplate}. Customizers are applied in the order that they were
	 * added after builder configuration has been applied. Setting this value will replace
	 * any previously configured customizers.
	 * @param restTemplateCustomizers the customizers to set
	 * @return this builder
	 * @see #additionalCustomizers(RestTemplateCustomizer...)
	 */
	public RestTemplateBuilder customizers(
			RestTemplateCustomizer... restTemplateCustomizers) {
		Assert.notNull(restTemplateCustomizers,
				"RestTemplateCustomizers must not be null");
		return customizers(Arrays.asList(restTemplateCustomizers));
	}

	/**
	 * Set the {@link HttpMessageConverter HttpMessageConverters} that should be applied
	 * to the {@link RestTemplate}. Customizers are applied in the order that they were
	 * added after builder configuration has been applied. Setting this value will replace
	 * any previously configured customizers.
	 * @param restTemplateCustomizers the customizers to set
	 * @return this builder
	 * @see #additionalCustomizers(RestTemplateCustomizer...)
	 */
	public RestTemplateBuilder customizers(
			Collection<? extends RestTemplateCustomizer> restTemplateCustomizers) {
		Assert.notNull(restTemplateCustomizers,
				"RestTemplateCustomizers must not be null");
		this.customizers = new LinkedHashSet<RestTemplateCustomizer>(
				restTemplateCustomizers);
		return this;
	}

	/**
	 * Add {@link HttpMessageConverter HttpMessageConverters} that should be applied to
	 * the {@link RestTemplate}. Customizers are applied in the order that they were added
	 * after builder configuration has been applied.
	 * @param restTemplateCustomizers the customizers to add
	 * @return this builder
	 * @see #customizers(RestTemplateCustomizer...)
	 */
	public RestTemplateBuilder additionalCustomizers(
			RestTemplateCustomizer... restTemplateCustomizers) {
		Assert.notNull(restTemplateCustomizers,
				"RestTemplateCustomizers must not be null");
		return additionalCustomizers(Arrays.asList(restTemplateCustomizers));
	}

	/**
	 * Add {@link HttpMessageConverter HttpMessageConverters} that should be applied to
	 * the {@link RestTemplate}. Customizers are applied in the order that they were added
	 * after builder configuration has been applied.
	 * @param restTemplateCustomizers the customizers to add
	 * @return this builder
	 * @see #customizers(RestTemplateCustomizer...)
	 */
	public RestTemplateBuilder additionalCustomizers(
			Collection<? extends RestTemplateCustomizer> restTemplateCustomizers) {
		Assert.notNull(restTemplateCustomizers,
				"RestTemplateCustomizers must not be null");
		this.customizers.addAll(restTemplateCustomizers);
		return this;
	}

	/**
	 * Build a new {@link RestTemplate} instance and configure it using this builder.
	 * @return a configured {@link RestTemplate} instance.
	 * @see #build(Class)
	 * @see #configure(RestTemplate)
	 */
	public RestTemplate build() {
		return build(RestTemplate.class);
	}

	/**
	 * Build a new {@link RestTemplate} instance of the specified type and configure it
	 * using this builder.
	 * @param restTemplateClass the template type to create
	 * @return a configured {@link RestTemplate} instance.
	 * @see RestTemplateBuilder#build()
	 * @see #configure(RestTemplate)
	 */
	public <T extends RestTemplate> T build(Class<T> restTemplateClass) {
		return configure(BeanUtils.instantiate(restTemplateClass));
	}

	/**
	 * Configure the provided {@link RestTemplate} instance using this builder.
	 * @param restTemplate the {@link RestTemplate} to configure
	 * @return the rest template instance
	 * @see RestTemplateBuilder#build()
	 * @see RestTemplateBuilder#build(Class)
	 */
	public <T extends RestTemplate> T configure(T restTemplate) {
		if (this.requestFactory != null) {
			restTemplate.setRequestFactory(this.requestFactory);
		}
		else if (this.detectRequestFactory) {
			restTemplate.setRequestFactory(detectRequestFactory());
		}
		if (!CollectionUtils.isEmpty(this.messageConverters)) {
			restTemplate.setMessageConverters(
					new ArrayList<HttpMessageConverter<?>>(this.messageConverters));
		}
		if (this.uriTemplateHandler != null) {
			restTemplate.setUriTemplateHandler(this.uriTemplateHandler);
		}
		if (this.errorHandler != null) {
			restTemplate.setErrorHandler(this.errorHandler);
		}
		if (this.rootUri != null) {
			RootUriTemplateHandler.addTo(restTemplate, this.rootUri);
		}
		if (this.basicAuthorization != null) {
			restTemplate.getInterceptors().add(this.basicAuthorization);
		}
		for (RestTemplateCustomizer customizer : this.customizers) {
			customizer.customize(restTemplate);
		}
		return restTemplate;
	}

	private ClientHttpRequestFactory detectRequestFactory() {
		for (Map.Entry<String, String> candidate : REQUEST_FACTORY_CANDIDATES
				.entrySet()) {
			ClassLoader classLoader = getClass().getClassLoader();
			if (ClassUtils.isPresent(candidate.getKey(), classLoader)) {
				Class<?> factoryClass = ClassUtils.resolveClassName(candidate.getValue(),
						classLoader);
				return (ClientHttpRequestFactory) BeanUtils.instantiate(factoryClass);
			}
		}
		return new SimpleClientHttpRequestFactory();
	}

}
