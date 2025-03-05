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
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
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

	/**
	 * Create a new {@link AbstractSelectableSet} instance populated from the given
	 * {@link Iterable}.
	 * @param <T> the type returned from the iterable
	 * @param iterable the source iterable
	 * @param selectableProvider a function that provides the {@link Selectable}
	 * @param elementProvider a function that provides the element
	 * @throws DuplicateSelectableNameException if duplicate {@link Selectable#name()
	 * names} would be added to the set
	 */
	protected <T> AbstractSelectableSet(Iterable<T> iterable, Function<? super T, Selectable> selectableProvider,
			ElementProvider<? super T, E> elementProvider) throws DuplicateSelectableNameException {
		this(iterable, selectableProvider, elementProvider, UnaryOperator.identity());
	}

	/**
	 * Create a new {@link AbstractSelectableSet} instance populated from the given
	 * {@link Iterable}.
	 * @param <T> the type returned from the iterable
	 * @param iterable the source iterable
	 * @param selectableProvider a function that provides the {@link Selectable}
	 * @param elementProvider a function that provides the element
	 * @param elementPostProcessor a post processor to apply to the element
	 * @throws DuplicateSelectableNameException if duplicate {@link Selectable#name()
	 * names} would be added to the set
	 */
	protected <T> AbstractSelectableSet(Iterable<T> iterable, Function<? super T, Selectable> selectableProvider,
			ElementProvider<? super T, E> elementProvider, UnaryOperator<E> elementPostProcessor)
			throws DuplicateSelectableNameException {
		this(buildEntries(iterable, selectableProvider, elementProvider.withPostProcessor(elementPostProcessor)));
	}

	/**
	 * Create a new {@link AbstractSelectableSet} instance populated from the given
	 * {@link Map}.
	 * @param <K> the map key type
	 * @param <V> the map value type
	 * @param map the source map
	 * @param selectableProvider a bi-function that provides the {@link Selectable}
	 * @param elementScope the scope of the elements in the set
	 * @param elementProvider a bi-function that provides the element
	 * @throws DuplicateSelectableNameException if duplicate {@link Selectable#name()
	 * names} would be added to the set
	 */
	protected <K, V> AbstractSelectableSet(Map<K, V> map,
			BiFunction<? super K, ? super V, Selectable> selectableProvider, ElementProvider.Scope elementScope,
			BiFunction<? super K, ? super V, E> elementProvider) throws DuplicateSelectableNameException {
		this(map, selectableProvider, elementProvider, elementScope, UnaryOperator.identity());
	}

	/**
	 * Create a new {@link AbstractSelectableSet} instance populated from the given
	 * {@link Map}.
	 * @param <K> the map key type
	 * @param <V> the map value type
	 * @param map the source map
	 * @param selectableProvider a bi-function that provides the {@link Selectable}
	 * @param elementScope the scope of the elements in the set
	 * @param elementProvider a bi-function that provides the element
	 * @param elementPostProcessor a post processor to apply to the element
	 * @throws DuplicateSelectableNameException if duplicate {@link Selectable#name()
	 * names} would be added to the set
	 */
	protected <K, V> AbstractSelectableSet(Map<K, V> map,
			BiFunction<? super K, ? super V, Selectable> selectableProvider,
			BiFunction<? super K, ? super V, E> elementProvider, ElementProvider.Scope elementScope,
			UnaryOperator<E> elementPostProcessor) throws DuplicateSelectableNameException {
		this(buildEntries(entrySet(map), adaptForMapEntry(selectableProvider),
				adaptForMapEntry(elementScope, elementPostProcessor, elementProvider)));
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

	private static <T, E> Map<String, Entry<E>> buildEntries(Iterable<T> iterable,
			Function<? super T, Selectable> selectableFactory, ElementProvider<? super T, E> elementProvider)
			throws DuplicateSelectableNameException {
		Assert.notNull(iterable, "'iterable' must not be null");
		Assert.notNull(selectableFactory, "'selectableFactory' must not be null");
		Assert.notNull(elementProvider, "'elementProvider' must not be null");
		Map<String, Entry<E>> entries = new LinkedHashMap<>();
		for (T source : iterable) {
			Selectable selectable = selectableFactory.apply(source);
			Assert.state(StringUtils.hasText(selectable.name()), "Selectable instances must have a name");
			Entry<E> entry = Entry.of(selectable, source, elementProvider);
			Entry<E> duplicate = entries.put(selectable.name(), entry);
			if (duplicate != null) {
				List<Selectable> duplicates = List.of(entry.selectable(), entry.selectable());
				throw new DuplicateSelectableNameException(null, duplicates, null);
			}
		}
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

	private static <K, V, R> ElementProvider<Map.Entry<V, K>, R> adaptForMapEntry(ElementProvider.Scope elementScope,
			UnaryOperator<R> elementPostProcessor, BiFunction<? super V, ? super K, R> biFunction) {
		ElementProvider<Map.Entry<V, K>, R> elementProvider = ElementProvider.of(elementScope,
				adaptForMapEntry(biFunction));
		return elementProvider.withPostProcessor(elementPostProcessor);
	}

	private static <K, V, R> Function<? super Map.Entry<K, V>, R> adaptForMapEntry(
			BiFunction<? super K, ? super V, R> biFunction) {
		return (biFunction != null) ? (entry) -> biFunction.apply(entry.getKey(), entry.getValue()) : null;
	}

	private static <K, V> Set<Map.Entry<K, V>> entrySet(Map<K, V> map) {
		Assert.notNull(map, "'map' must not be null");
		return map.entrySet();
	}

	// FIXME put scope back?

}
