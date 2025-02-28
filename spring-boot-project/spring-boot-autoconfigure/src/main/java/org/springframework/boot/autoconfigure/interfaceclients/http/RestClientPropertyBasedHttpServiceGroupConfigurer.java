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

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient.Builder;
import org.springframework.web.client.support.RestClientHttpServiceGroup;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

// TODO: add corresponding WebClient-based implementation

/**
 * @author Olga Maciaszek-Sharma
 */
public class RestClientPropertyBasedHttpServiceGroupConfigurer implements RestClientHttpServiceGroupConfigurer {

	private final ObjectProvider<HttpInterfaceClientsProperties> propertiesProvider;

	public RestClientPropertyBasedHttpServiceGroupConfigurer(
			ObjectProvider<HttpInterfaceClientsProperties> propertiesProvider) {
		this.propertiesProvider = propertiesProvider;
	}

	@Override
	public void configure(RestClientHttpServiceGroup group) {
		// TODO: handle null name and only url scenario
		group.configureClient(buildClientBuilderConsumer(group.name()));
	}

	private Consumer<Builder> buildClientBuilderConsumer(String clientGroupName) {
		return (builder) -> {
			HttpInterfaceClientGroupProperties clientGroupProperties = this.propertiesProvider.getObject()
					.getProperties(clientGroupName);
			builder.requestFactory(buildClientHttpRequestFactory(clientGroupProperties));
			Map<String, List<String>> defaultHeaders = clientGroupProperties.getDefaultHeaders();
			for (String headerName : defaultHeaders.keySet()) {
				builder.defaultHeader(headerName, defaultHeaders.get(headerName).toArray(String[]::new));
			}
		};
	}

	private ClientHttpRequestFactory buildClientHttpRequestFactory(
			HttpInterfaceClientGroupProperties clientProperties) {
		ClientHttpRequestFactorySettings factorySettings = ClientHttpRequestFactorySettings.defaults()
				.withConnectTimeout(clientProperties.getConnectTimeout())
				.withReadTimeout(clientProperties.getReadTimeout());
		return ClientHttpRequestFactoryBuilder.detect().build(factorySettings);
	}

}
