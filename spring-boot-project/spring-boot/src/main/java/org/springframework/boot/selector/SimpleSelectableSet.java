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

package org.springframework.boot.selector;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Simple {@link SimpleSelectableSet} implementation.
 *
 * @param <S> a self reference for fluent methods
 * @param <E> the type of elements maintained
 * @author Phillip Webb
 */
final class SimpleSelectableSet<S extends SelectableSet<S, E>, E> extends AbstractSelectableSet<S, E> {

	private static final SelectableSet<?, ?> EMPTY = new SimpleSelectableSet<>();

	private SimpleSelectableSet() {
	}

	<T> SimpleSelectableSet(Iterable<T> iterable, Function<? super T, Entry<E>> entryProvider)
			throws DuplicateSelectableNameException {
		super(iterable, entryProvider);
	}

	<T> SimpleSelectableSet(Stream<T> stream, Function<? super T, Entry<E>> entryProvider)
			throws DuplicateSelectableNameException {
		super(stream, entryProvider);
	}

	private SimpleSelectableSet(Map<String, Entry<E>> entries, Predicate<Entry<E>> predicate) {
		super(entries, predicate);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected S withPredicate(Map<String, Entry<E>> entries, Predicate<Entry<E>> predicate) {
		return (S) new SimpleSelectableSet<>(entries, predicate);
	}

	@SuppressWarnings("unchecked")
	static <S extends SelectableSet<S, E>, E> SelectableSet<S, E> empty() {
		return (SelectableSet<S, E>) EMPTY;
	}

}
