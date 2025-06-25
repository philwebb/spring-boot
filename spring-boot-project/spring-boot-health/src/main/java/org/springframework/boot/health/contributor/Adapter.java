/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.health.contributor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Internal base class for adapters.
 *
 * @author Phillip Webb
 * @param <S> the source providing the contributors
 * @param <T> the type of elements provided by the source
 * @param <C> the contributor type
 * @param <E> the entry type
 */
class Adapter<S, T, C, E> {

	private final Collection<S> sources;

	private final BiFunction<S, String, C> contributorProvider;

	private final Function<S, Stream<T>> elementsProvider;

	private final Function<T, String> nameProvider;

	private final Function<T, E> entryProvider;

	Adapter(Collection<S> sources, BiFunction<S, String, C> contributorProvider,
			Function<S, Stream<T>> elementStreamProvider, Function<T, String> nameProvider,
			Function<T, E> entryProvider) {
		this.sources = sources;
		this.contributorProvider = contributorProvider;
		this.elementsProvider = elementStreamProvider;
		this.nameProvider = nameProvider;
		this.entryProvider = entryProvider;
	}

	C getContributor(String name) {
		return this.sources.stream()
			.map((source) -> this.contributorProvider.apply(source, name))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}

	Stream<E> stream() {
		Set<String> seen = new HashSet<>();
		return this.sources.stream()
			.flatMap(this.elementsProvider)
			.filter((element) -> seen.add(this.nameProvider.apply(element)))
			.map(this.entryProvider);
	}

}
