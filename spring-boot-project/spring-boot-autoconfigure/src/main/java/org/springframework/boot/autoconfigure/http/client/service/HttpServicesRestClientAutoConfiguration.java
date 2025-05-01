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

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.http.client.HttpClientSettingsProperties;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RestClientAdapter} backed
 * HTTP Service clients.
 * <p>
 * This will result in the creation of blocking HTTP Service client beans defined by
 * {@link ImportHttpServices @ImportHttpServices} annotations.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(after = { RestClientAutoConfiguration.class, HttpServicesAutoConfiguration.class })
@ConditionalOnClass(RestClientAdapter.class)
@ConditionalOnHttpServiceProxyBean
@EnableConfigurationProperties(HttpClientServiceProperties.class)
public class HttpServicesRestClientAutoConfiguration implements BeanClassLoaderAware {

	private final Environment environment;

	private ClassLoader beanClassLoader;

	HttpServicesRestClientAutoConfiguration(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Bean
	RestClientPropertiesHttpServiceGroupConfigurer restClientPropertiesHttpServiceGroupConfigurer(
			ObjectProvider<SslBundles> sslBundles, HttpClientSettingsProperties settingsProperties,
			HttpClientServiceProperties serviceProperties,
			ObjectProvider<ClientHttpRequestFactoryBuilder<?>> clientFactoryBuilder,
			ObjectProvider<ClientHttpRequestFactorySettings> clientHttpRequestFactorySettings) {
		return new RestClientPropertiesHttpServiceGroupConfigurer(this.beanClassLoader, this.environment, sslBundles,
				settingsProperties, serviceProperties, clientFactoryBuilder, clientHttpRequestFactorySettings);
	}

	@Bean
	RestClientCustomizerHttpServiceGroupConfigurer restClientCustomizerHttpServiceGroupConfigurer(
			ObjectProvider<RestClientCustomizer> customizers) {
		return new RestClientCustomizerHttpServiceGroupConfigurer(customizers);
	}

}
