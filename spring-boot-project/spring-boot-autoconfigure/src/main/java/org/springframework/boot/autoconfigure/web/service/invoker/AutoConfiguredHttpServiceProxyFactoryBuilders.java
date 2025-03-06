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
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.boot.selector.AbstractSelectableSet;
import org.springframework.boot.selector.Selectable;
import org.springframework.boot.selector.Selector;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.invoker.HttpServiceProxyFactory.Builder;

/**
 * @author Phillip Webb
 */
class AutoConfiguredHttpServiceProxyFactoryBuilders
		extends AbstractSelectableSet<HttpServiceProxyFactoryBuilders, HttpServiceProxyFactory.Builder>
		implements HttpServiceProxyFactoryBuilders {

	public AutoConfiguredHttpServiceProxyFactoryBuilders(Stream<Entry<HttpExchangeAdapter>> adapterEntries,
			List<HttpServiceProxyFactoryCustomizer> customizers) {
		super(adapterEntries, (adapterEntry) -> asBuilderEntry(adapterEntry, customizers));
	}

	private static Entry<HttpServiceProxyFactory.Builder> asBuilderEntry(Entry<HttpExchangeAdapter> adapterEntry,
			List<HttpServiceProxyFactoryCustomizer> customizers) {
		Selectable selectable = adapterEntry.selectable();
		HttpExchangeAdapter adapter = adapterEntry.element();
		return Entry.of(selectable, () -> {
			HttpServiceProxyFactory.Builder builder = HttpServiceProxyFactory.builderFor(adapter);
			Selector.streamSelected(customizers, selectable).forEach((customizer) -> customizer.customize(builder));
			return builder;
		});
	}

	@Override
	protected HttpServiceProxyFactoryBuilders withPredicate(Map<String, Entry<Builder>> entries,
			Predicate<Entry<Builder>> predicate) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
