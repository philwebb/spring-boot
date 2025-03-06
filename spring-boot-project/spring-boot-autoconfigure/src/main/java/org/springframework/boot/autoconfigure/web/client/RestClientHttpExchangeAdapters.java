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

import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.web.service.invoker.HttpExchangeAdapters;
import org.springframework.boot.selector.SelectableSet.Entry;
import org.springframework.boot.web.client.RestClients;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpExchangeAdapter;

/**
 * {@link HttpExchangeAdapters} backed by {@link RestClients}.
 *
 * @author Phillip Webb
 */
class RestClientHttpExchangeAdapters implements HttpExchangeAdapters {

	private final RestClients restClients;

	RestClientHttpExchangeAdapters(RestClients restClients) {
		this.restClients = restClients;
	}

	@Override
	public Stream<Entry<HttpExchangeAdapter>> streamHttpExchangeAdapters() {
		return this.restClients.streamEntries().map(this::asHttpExchangeAdapterEntry);
	}

	private Entry<HttpExchangeAdapter> asHttpExchangeAdapterEntry(Entry<RestClient> restClientEntry) {
		return Entry.ofSingleton(restClientEntry.selectable(),
				() -> RestClientAdapter.create(restClientEntry.element()));
	}

}
