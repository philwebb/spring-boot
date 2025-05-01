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

package org.springframework.boot.autoconfigure.http.client.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.http.client.service.HttpServicesAutoConfiguration.HttpExchangesRegistrar;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.registry.AbstractHttpServiceRegistrar;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for HTTP Services.
 *
 * @author Phillip Webb
 */
@AutoConfiguration
@Import(HttpExchangesRegistrar.class)
public class HttpServicesAutoConfiguration {

	/**
	 * {@link AbstractHttpServiceRegistrar} that scans {@link AutoConfigurationPackages}
	 * and registers any interface with a type level {@link HttpExchange @HttpExchange}
	 * annotation that has an absolute ULR or a {@code clientservicegroup://} pseudo URL.
	 */
	static class HttpExchangesRegistrar extends AbstractHttpServiceRegistrar {

		private static final String GROUP_URL = "clientservicegroup://";

		private final Environment environment;

		private final BeanFactory beanFactory;

		HttpExchangesRegistrar(Environment environment, BeanFactory beanFactory) {
			this.environment = environment;
			this.beanFactory = beanFactory;
		}

		@Override
		protected void registerHttpServices(GroupRegistry registry, AnnotationMetadata importingClassMetadata) {
			if (AutoConfigurationPackages.has(this.beanFactory)) {
				registerHttpServices(registry, AutoConfigurationPackages.get(this.beanFactory));
			}
		}

		private void registerHttpServices(GroupRegistry registry, List<String> basePackages) {
			registry.forGroup(this::getGroupName, (type) -> ClientType.UNSPECIFIED)
				.detectInBasePackages(basePackages.toArray(String[]::new));
		}

		private String getGroupName(Class<?> type) {
			MergedAnnotation<HttpExchange> httpExchange = MergedAnnotations.from(type).get(HttpExchange.class);
			if (httpExchange.isPresent()) {
				String value = this.environment.resolvePlaceholders(httpExchange.getString("value"));
				if (value.startsWith(GROUP_URL)) {
					return value.substring(GROUP_URL.length());
				}
				if (isAbsoluteUrl(value)) {
					return HttpServiceGroup.DEFAULT_GROUP_NAME;
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

	}

}
