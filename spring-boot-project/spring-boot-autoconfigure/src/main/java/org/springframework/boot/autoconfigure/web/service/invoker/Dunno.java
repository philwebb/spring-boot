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

import java.util.stream.Stream;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.selector.SelectableSet.Entry;
import org.springframework.context.annotation.Bean;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactories;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * @author pwebb
 */
public class Dunno {

	@Bean
	HttpServiceProxyFactory httpServiceProxyFactory() {
		return null;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(HttpExchangeAdapters.class)
	HttpServiceProxyFactoryBuilders httpServiceProxyFactories(
			ObjectProvider<HttpExchangeAdapters> httpExchangeAdapters) {
		return new AutoConfiguredHttpServiceProxyFactoryBuilders(streamAdapterEntries(httpExchangeAdapters));
	}

	HttpServiceProxyFactories httpServiceProxyFactories(
			HttpServiceProxyFactoryBuilders httpServiceProxyFactoryBuilders) {
		return HttpServiceProxyFactories.of((name) -> httpServiceProxyFactoryBuilders.get(name).build());
	}

	private Stream<Entry<HttpExchangeAdapter>> streamAdapterEntries(ObjectProvider<HttpExchangeAdapters> adapters) {
		return adapters.orderedStream().flatMap(HttpExchangeAdapters::streamEntries);
	}

}
