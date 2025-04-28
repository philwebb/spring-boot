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

package org.springframework.boot.autoconfigure.http.interfaceclient.reactive;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.http.client.HttpClientProperties;
import org.springframework.boot.autoconfigure.http.client.reactive.ClientHttpConnectors;
import org.springframework.boot.autoconfigure.http.client.reactive.HttpReactiveClientProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorSettings;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.HttpServiceGroup;

/**
 * A {@link RestClientHttpServiceGroupConfigurer} that configures the group and its
 * underlying {@link RestClient} using {@link HttpClientProperties}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Phillip Webb
 */
class WebClientPropertiesHttpServiceGroupConfigurer implements WebClientHttpServiceGroupConfigurer {

	private final ClassLoader classLoader;

	private final ObjectProvider<SslBundles> sslBundles;

	private final HttpReactiveClientProperties properties;

	private final ObjectProvider<ClientHttpConnectorBuilder<?>> clientConnectorBuilder;

	private final ObjectProvider<ClientHttpConnectorSettings> clientConnectorSettings;

	WebClientPropertiesHttpServiceGroupConfigurer(ClassLoader classLoader, ObjectProvider<SslBundles> sslBundles,
			HttpReactiveClientProperties properties,
			ObjectProvider<ClientHttpConnectorBuilder<?>> clientConnectorBuilder,
			ObjectProvider<ClientHttpConnectorSettings> clientConnectorSettings) {
		this.classLoader = classLoader;
		this.sslBundles = sslBundles;
		this.properties = properties;
		this.clientConnectorBuilder = clientConnectorBuilder;
		this.clientConnectorSettings = clientConnectorSettings;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public void configureGroups(Groups<WebClient.Builder> groups) {
		groups.configureClient(this::configureClient);
	}

	private void configureClient(HttpServiceGroup group, WebClient.Builder builder) {
		HttpReactiveClientProperties.Group groupProperties = this.properties.getGroup().get(group.name());
		builder.clientConnector(getClientConnector(groupProperties));
		if (groupProperties != null) {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(groupProperties::getBaseUrl).whenHasText().to(builder::baseUrl);
			map.from(groupProperties::getDefaultHeaders).as(this::putAllHeaders).to(builder::defaultHeaders);
		}
	}

	private Consumer<HttpHeaders> putAllHeaders(Map<String, List<String>> defaultHeaders) {
		return (httpHeaders) -> httpHeaders.putAll(defaultHeaders);
	}

	private ClientHttpConnector getClientConnector(HttpReactiveClientProperties.Group groupProperties) {
		ClientHttpConnectors connectors = new ClientHttpConnectors(this.sslBundles, groupProperties,
				this.properties.getSettings());
		ClientHttpConnectorBuilder<?> builder = this.clientConnectorBuilder
			.getIfAvailable(() -> connectors.builder(this.classLoader));
		ClientHttpConnectorSettings settings = this.clientConnectorSettings.getIfAvailable(connectors::settings);
		return builder.build(settings);
	}

}
