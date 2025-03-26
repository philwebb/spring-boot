/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.service.invoker;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.registry.AbstractHttpServiceRegistrar;
import org.springframework.web.service.registry.HttpServiceGroup;

/**
 * {@link AbstractHttpServiceRegistrar} that scans {@link AutoConfigurationPackages} and
 * registers any interface with a type level {@link HttpExchange @HttpExchange} annotation
 * that has an absolute ULR or a reference to a group.
 *
 * @author Phillip Webb
 */
class HttpServiceComponentRegistrar extends AbstractHttpServiceRegistrar {

	private static final Pattern GROUP_REFERENCE = Pattern.compile("^@(\\w+)$");

	private final Environment environment;

	private final BeanFactory beanFactory;

	HttpServiceComponentRegistrar(Environment environment, BeanFactory beanFactory) {
		this.environment = environment;
		this.beanFactory = beanFactory;
	}

	@Override
	protected void registerHttpServices(HttpServiceRegistry registry, AnnotationMetadata importingClassMetadata) {
		if (AutoConfigurationPackages.has(this.beanFactory)) {
			registerHttpServices(registry, AutoConfigurationPackages.get(this.beanFactory));
		}
	}

	private void registerHttpServices(HttpServiceRegistry registry, List<String> basePackages) {
		registry.forGroup(this::getGroupName)
			.detectInBasePackages(this::isHttpServiceComponent, basePackages.toArray(String[]::new));
	}

	private boolean isHttpServiceComponent(AnnotationMetadata metadata) {
		return getComponent(metadata.getAnnotations()) != null;
	}

	private String getGroupName(Class<?> type) {
		return getComponent(MergedAnnotations.from(type)).group();
	}

	private HttpServiceComponent getComponent(MergedAnnotations annotations) {
		MergedAnnotation<HttpExchange> httpExchange = annotations.get(HttpExchange.class);
		if (httpExchange.isPresent()) {
			String value = this.environment.resolvePlaceholders(httpExchange.getString("value"));
			Matcher groupMatcher = GROUP_REFERENCE.matcher(value);
			if (groupMatcher.matches()) {
				return new HttpServiceComponent(groupMatcher.group(1), null);
			}
			if (isAbsoluteUrl(value)) {
				return new HttpServiceComponent(HttpServiceGroup.DEFAULT_GROUP_NAME, value);
			}
		}
		return null;
	}

	private boolean isAbsoluteUrl(String value) {
		try {
			return new URI(value).isAbsolute();
		}
		catch (URISyntaxException e) {
			return false;
		}
	}

	private record HttpServiceComponent(String group, String url) {

	}

}
