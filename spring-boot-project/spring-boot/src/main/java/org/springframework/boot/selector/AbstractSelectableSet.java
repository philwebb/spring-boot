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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Skeletal implementation of {@link SelectableSet} designed for subclassing.
 *
 * @param <S> a self reference for fluent methods
 * @param <E> the type of elements maintained
 * @author Phillip Webb
 * @since 4.0.0
 */
public abstract class AbstractSelectableSet<S extends SelectableSet<S, E>, E> implements SelectableSet<S, E> {

	private final Map<String, Entry<E>> entries;

	private final Predicate<Entry<E>> predicate;

	private volatile Boolean empty;

	private volatile String toString;

	/**
	 * Package-private constructor used to create {@link SelectableSet#empty()}.
	 */
	AbstractSelectableSet() {
		this(Collections.emptyMap());
	}

	protected <T> AbstractSelectableSet(Stream<T> stream, Function<? super T, Entry<E>> entryProvider)
			throws DuplicateSelectableNameException {
		this(buildEntries(stream, entryProvider));
	}

	protected <T> AbstractSelectableSet(Iterable<T> iterable, Function<? super T, Entry<E>> entryProvider)
			throws DuplicateSelectableNameException {
		this(buildEntries(iterable, entryProvider));
	}

	protected <K, V> AbstractSelectableSet(Map<K, V> map, BiFunction<? super K, ? super V, Entry<E>> entryProvider)
			throws DuplicateSelectableNameException {
		this(buildEntries(map, entryProvider));
	}

	private AbstractSelectableSet(Map<String, Entry<E>> entries) {
		this(entries, (entry) -> true);
	}

	/**
	 * Create a new {@link AbstractSelectableSet} instance with the given entries and
	 * predicate.
	 * @param entries the set entries
	 * @param predicate an updated predicate
	 * @see #withPredicate(Map, Predicate)
	 */
	protected AbstractSelectableSet(Map<String, Entry<E>> entries, Predicate<Entry<E>> predicate) {
		this.entries = entries;
		this.predicate = predicate;
	}

	/**
	 * Factory method that must be implemented by subclasses to create a copy of this set
	 * with an updated predicate.
	 * @param entries the set entries
	 * @param predicate an updated predicate
	 * @return a new {@link SelectableSet} instance
	 * @see #AbstractSelectableSet(Map, Predicate)
	 */
	protected abstract S withPredicate(Map<String, Entry<E>> entries, Predicate<Entry<E>> predicate);

	private static <K, V, E> Map<String, Entry<E>> buildEntries(Map<K, V> map,
			BiFunction<? super K, ? super V, Entry<E>> entryProvider) throws DuplicateSelectableNameException {
		return null; // FIXME
	}

	private static <T, E> Map<String, Entry<E>> buildEntries(Iterable<T> stream,
			Function<? super T, Entry<E>> entryProvider) throws DuplicateSelectableNameException {
		return null; // FIXME
	}

	private static <T, E> Map<String, Entry<E>> buildEntries(Stream<T> stream,
			Function<? super T, Entry<E>> entryProvider) throws DuplicateSelectableNameException {
		Assert.notNull(stream, "'stream' must not be null");
		Assert.notNull(entryProvider, "'entryProvider' must not be null");
		Map<String, Entry<E>> entries = new LinkedHashMap<>();
		stream.forEach((source) -> {
			Entry<E> entry = entryProvider.apply(source);
			String name = entry.selectable().name();
			Assert.state(StringUtils.hasText(name), "Selectable instances must have a name");
			Entry<E> duplicate = entries.put(name, entry);
			if (duplicate != null) {
				List<Selectable> duplicates = List.of(entry.selectable(), entry.selectable());
				throw new DuplicateSelectableNameException(null, duplicates, null);
			}
		});
		return Collections.unmodifiableMap(entries);
	}

	@Override
	public Entry<E> getEntry(String name) {
		Assert.notNull(name, "'name' must not be null");
		Entry<E> entry = this.entries.get(name);
		if (entry == null || !this.predicate.test(entry)) {
			throw new NoSuchSelectableNameException(null, name, null);
		}
		return entry;
	}

	@Override
	public Iterator<E> iterator() {
		return stream().iterator();
	}

	@Override
	public Stream<Entry<E>> streamEntries() {
		return (!isKnownEmpty()) ? this.entries.values().stream().filter(this::filter) : Stream.empty();
	}

	private boolean filter(Entry<E> entry) {
		return this.predicate.test(entry);
	}

	@Override
	public boolean isEmpty() {
		Boolean empty = this.empty;
		if (empty == null) {
			empty = !iterator().hasNext();
			this.empty = empty;
		}
		return empty;
	}

	@Override
	@SuppressWarnings("unchecked")
	public S having(BiPredicate<Selectable, E> predicate) {
		if (isKnownEmpty()) {
			return (S) this;
		}
		return withPredicate(this.entries, this.predicate.and(entryPredicate(predicate)));
	}

	private Predicate<Entry<E>> entryPredicate(BiPredicate<? super Selectable, ? super E> predicate) {
		return (entry) -> predicate.test(entry.selectable(), entry.element());
	}

	private boolean isKnownEmpty() {
		return Boolean.TRUE.equals(this.empty);
	}

	@Override
	public String toString() {
		String toString = this.toString;
		if (toString == null) {
			toString = streamEntries().map(SimpleSelectableSetEntry::toString)
				.collect(Collectors.joining(", ", "[", "]"));
			this.toString = toString;
		}
		return toString;
	}

}
