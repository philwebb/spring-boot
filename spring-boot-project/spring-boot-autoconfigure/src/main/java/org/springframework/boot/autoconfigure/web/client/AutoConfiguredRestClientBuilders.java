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

package org.springframework.boot.autoconfigure.web.client;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.http.client.AutoConfiguredClientHttpRequestFactories;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.selector.AbstractSelectableSet;
import org.springframework.boot.selector.Selectable;
import org.springframework.boot.selector.Selector;
import org.springframework.boot.web.client.RestClientBuilders;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

/**
 * @author Phillip Webb
 * @author Olga Maciaszek-Sharma
 */
class AutoConfiguredRestClientBuilders extends AbstractSelectableSet<RestClientBuilders, RestClient.Builder>
		implements RestClientBuilders {

	AutoConfiguredRestClientBuilders(RestClientsProperties properties,
			AutoConfiguredClientHttpRequestFactories httpRequestFactories,
			ObjectProvider<RestClientCustomizer> customizers) {
		super(properties, (name, clientProperties) -> asRestClientBuilder2(httpRequestFactories, customizers, name,
				clientProperties));
	}

	private AutoConfiguredRestClientBuilders(Map<String, Entry<Builder>> entries, Predicate<Entry<Builder>> predicate) {
		super(entries, predicate);
	}

	private static Entry<RestClient.Builder> asRestClientBuilder2(
			AutoConfiguredClientHttpRequestFactories httpRequestFactories,
			ObjectProvider<RestClientCustomizer> customizers, String name, RestClientProperties properties) {
		return null;
	}

	private static RestClient.Builder asRestClientBuilder(AutoConfiguredClientHttpRequestFactories httpRequestFactories,
			ObjectProvider<RestClientCustomizer> customizers, String name, RestClientProperties properties) {
		ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder = httpRequestFactories.builder(properties);
		ClientHttpRequestFactorySettings requestFactorySettings = httpRequestFactories.settings(properties);
		ClientHttpRequestFactory requestFactory = requestFactoryBuilder.build(requestFactorySettings);
		RestClient.Builder restClientBuilder = RestClient.builder().requestFactory(requestFactory);
		customizeRestClientBuilder(restClientBuilder, customizers, name, properties);
		return restClientBuilder;
	}

	private static void customizeRestClientBuilder(RestClient.Builder restClientBuilder,
			ObjectProvider<RestClientCustomizer> customizers, String name, RestClientProperties properties) {
		Selectable selectable = asSelectable(name, properties);
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getBaseUrl).to(restClientBuilder::baseUrl);
		map.from(properties::getDefaultHeaders)
			.as(AutoConfiguredRestClientBuilders::defaultHeaders)
			.to(restClientBuilder::defaultHeaders);
		customizers.orderedStream()
			.filter(Selector.selecting(selectable))
			.forEach((customizer) -> customizer.customize(restClientBuilder));
	}

	static Consumer<HttpHeaders> defaultHeaders(Map<String, List<String>> defaultHeaders) {
		return (headers) -> defaultHeaders.forEach(headers::addAll);
	}

	private static Selectable asSelectable(String name, RestClientProperties properties) {
		return Selectable.of(name, properties.getLabel());
	}

	@Override
	protected RestClientBuilders withPredicate(Map<String, Entry<Builder>> entries,
			Predicate<Entry<Builder>> predicate) {
		return new AutoConfiguredRestClientBuilders(entries, predicate);
	}

}
