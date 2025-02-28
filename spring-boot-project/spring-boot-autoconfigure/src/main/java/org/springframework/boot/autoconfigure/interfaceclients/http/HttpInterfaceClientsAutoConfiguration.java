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

package org.springframework.boot.autoconfigure.interfaceclients.http;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.client.support.RestClientHttpServiceProxyRegistry;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceProxyRegistry;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for HTTP Interface Clients.
 * <p>
 * This will result in the creation of Interface Client beans defined by
 * {@link EnableInterfaceClients} annotations.
 *
 * @author Olga Maciaszek-Sharma
 * @since 4.0.0
 */
@AutoConfiguration(after = { RestTemplateAutoConfiguration.class, RestClientAutoConfiguration.class,
		WebClientAutoConfiguration.class })
@ConditionalOnProperty(value = "spring.interface-clients.enabled", havingValue = "true", matchIfMissing = true)
// TODO*: should this be in the autoconfig or only user-provided?
@EnableInterfaceClients
public class HttpInterfaceClientsAutoConfiguration {

	// TODO*: consider making registry more lazy and converting this into a
	// `@ConfigurationProperties` bean
	@Bean
	HttpInterfaceClientsProperties httpInterfaceClientsProperties(ListableBeanFactory beanFactory) {
		return Binder.get(beanFactory.getBean(Environment.class))
				.bindOrCreate("spring.interface-clients.http", HttpInterfaceClientsProperties.class);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ RestClient.class, RestClientAdapter.class, HttpServiceProxyFactory.class })
	@ConditionalOnMissingClass("org.springframework.web.reactive.function.client.WebClient")
	protected static class RestClientInterfaceClientsConfiguration {

		@Bean
		@ConditionalOnBean(RestClient.Builder.class)
		@ConditionalOnMissingBean
		RestClientHttpServiceProxyRegistry httpServiceProxyRegistry(RestClient.Builder baseRestClientBuilder) {
			return RestClientHttpServiceProxyRegistry.create(baseRestClientBuilder);
		}

		@Bean
		@ConditionalOnBean(RestClient.Builder.class)
		RestClientPropertyBasedHttpServiceGroupConfigurer restClientPropertyBasedHttpServiceGroupConfigurer(
				ObjectProvider<HttpInterfaceClientsProperties> propertiesProvider) {
			return new RestClientPropertyBasedHttpServiceGroupConfigurer(propertiesProvider);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ WebClient.class, WebClientAdapter.class, HttpServiceProxyFactory.class })
	protected static class WebClientInterfaceClientsConfiguration {

		@Bean
		@ConditionalOnBean(WebClient.Builder.class)
		@ConditionalOnMissingBean
		WebClientHttpServiceProxyRegistry httpServiceProxyRegistry(WebClient.Builder baseWebClientBuilder) {
			return WebClientHttpServiceProxyRegistry.create(baseWebClientBuilder);
		}

	}

}
