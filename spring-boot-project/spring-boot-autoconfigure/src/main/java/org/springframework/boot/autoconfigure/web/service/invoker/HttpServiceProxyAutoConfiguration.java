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

import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.selector.Selectable;
import org.springframework.boot.selector.SelectableSet.Entry;
import org.springframework.boot.selector.Selector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.invoker.HttpServiceProxyFactoryProvider;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for HTTP service proxies.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(HttpServiceProxyFactory.class)
public class HttpServiceProxyAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnSingleCandidate(HttpExchangeAdapter.class)
	HttpServiceProxyFactory httpServiceProxyFactory(HttpExchangeAdapter httpExchangeAdapter,
			List<HttpServiceProxyFactoryCustomizer> httpServiceProxyFactoryCustomizers) {
		HttpServiceProxyFactory.Builder builder = HttpServiceProxyFactory.builderFor(httpExchangeAdapter);
		Selector.streamSelected(httpServiceProxyFactoryCustomizers, Selectable.blank())
			.forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(HttpExchangeAdapters.class)
	HttpServiceProxyFactoryBuilders httpServiceProxyFactoryBuilders(
			ObjectProvider<HttpExchangeAdapters> httpExchangeAdapters,
			List<HttpServiceProxyFactoryCustomizer> httpServiceProxyFactoryCustomizers) {
		return new AutoConfiguredHttpServiceProxyFactoryBuilders(streamHttpExchangeAdapters(httpExchangeAdapters),
				httpServiceProxyFactoryCustomizers);
	}

	private Stream<Entry<HttpExchangeAdapter>> streamHttpExchangeAdapters(
			ObjectProvider<HttpExchangeAdapters> adapters) {
		return adapters.orderedStream().flatMap(HttpExchangeAdapters::streamHttpExchangeAdapters);
	}

	@Bean
	@ConditionalOnSingleCandidate(HttpServiceProxyFactoryBuilders.class)
	HttpServiceProxyFactoryProvider httpServiceProxyFactoryBuildersServiceProxyFactoryProvider(
			HttpServiceProxyFactoryBuilders httpServiceProxyFactoryBuilders) {
		return (name) -> httpServiceProxyFactoryBuilders.get(name).build();
	}

	@Bean
	@Primary
	AutoConfiguredHttpServiceProxyFactoryProvider httpServiceProxyFactoryProvider(
			List<HttpServiceProxyFactoryProvider> providers) {
		// FIXME we probably need a ConditionalOnMissingSingleCandidate
		return new AutoConfiguredHttpServiceProxyFactoryProvider(providers);
	}

}
