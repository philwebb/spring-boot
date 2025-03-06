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

package org.springframework.boot.web.client;

import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.boot.selector.AbstractSelectableSet;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

/**
 * Simple {@link RestClients} implementation backed by a {@link RestClientBuilders}.
 *
 * @author Phillip Webb
 */
final class SimpleRestClients extends AbstractSelectableSet<RestClients, RestClient> implements RestClients {

	private final RestClientBuilders builders;

	SimpleRestClients(RestClientBuilders builders) {
		super(streamEntries(builders), (builderEntry) -> asRestClientEntry(builderEntry));
		this.builders = builders;
	}

	private SimpleRestClients(RestClientBuilders restClientBuilders, Map<String, Entry<RestClient>> entries,
			Predicate<Entry<RestClient>> predicate) {
		super(entries, predicate);
		this.builders = restClientBuilders;
	}

	private static Stream<Entry<Builder>> streamEntries(RestClientBuilders builders) {
		Assert.notNull(builders, "'builders' must not be null");
		return builders.streamEntries();
	}

	private static Entry<RestClient> asRestClientEntry(Entry<Builder> builderEntry) {
		Builder builder = builderEntry.element();
		RestClient restClient = builder.build();
		return Entry.of(builderEntry.selectable(), () -> restClient); // FIXME
																		// ofSingleton?
	}

	@Override
	protected RestClients withPredicate(Map<String, Entry<RestClient>> entries,
			Predicate<Entry<RestClient>> predicate) {
		return new SimpleRestClients(this.builders, entries, predicate);
	}

	@Override
	public RestClientBuilders builders() {
		return this.builders;
	}

}
