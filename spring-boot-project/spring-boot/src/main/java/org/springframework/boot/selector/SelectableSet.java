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

import java.util.Collection;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;

/**
 * A set of elements that can be selected based on various criteria.
 *
 * @param <S> a self reference for fluent methods
 * @param <E> the type of elements in this set
 * @author Phillip Webb
 * @since 4.0.0
 * @see Selectable
 * @see Selector
 */
public interface SelectableSet<S extends SelectableSet<S, E>, E> extends Iterable<E> {

	/**
	 * Get an element from the set by its {@link Selectable#name() name}.
	 * @param name the {@link Selectable#name() selectable name}
	 * @return the related element
	 * @throws NoSuchSelectableNameException if no selectable with the name is available
	 * in the set
	 */
	default E get(String name) throws NoSuchSelectableNameException {
		return getEntry(name).element();
	}

	/**
	 * Get an {@link Entry} from the set by its {@link Selectable#name() name}.
	 * @param name the {@link Selectable#name() selectable name}
	 * @return the related element
	 * @throws NoSuchSelectableNameException if no selectable with the name is available
	 * in the set
	 */
	Entry<E> getEntry(String name) throws NoSuchSelectableNameException;

	/**
	 * Return a sequential {@code Stream} with this set as its source.
	 * @return a stream of the contined elements.
	 */
	default Stream<E> stream() {
		return streamEntries().map(Entry::element);
	}

	/**
	 * Return an {@link Iterable} all {@link Entry entries} in this set. Entries provide
	 * access to the element and its selector.
	 * @return the entries in this set
	 */
	default Iterable<Entry<E>> entries() {
		return streamEntries()::iterator;
	}

	/**
	 * Return an {@link Iterable} all {@link Entry entries} in this set. Entries provide
	 * access to the element and its selector.
	 * @return the entries in this set
	 */
	Stream<Entry<E>> streamEntries();

	/**
	 * Returns {@code true} if the set is empty.
	 * @return if the set is empty
	 */
	default boolean isEmpty() {
		return !iterator().hasNext();
	}

	/**
	 * Return a new {@link SelectableSet} that includes {@link Selectable} entries with
	 * the given name. The resulting set will include any previously made
	 * {@code having...} selections.
	 * @param name the {@link Selectable#name() name} to select
	 * @return a new {@link SelectableSet} instance
	 */
	default S havingName(String name) {
		return having(Selector.select().onlyWhenNamed(name));
	}

	/**
	 * Return a new {@link SelectableSet} that includes {@link Selectable} entries with a
	 * name that matches the given predicate. The resulting set will include any
	 * previously made {@code having...} selections.
	 * @param predicate a predicate used to test if the name matches
	 * @return a new {@link SelectableSet} instance
	 */
	default S havingName(Predicate<String> predicate) {
		return having(Selector.select().onlyWhenNamed(predicate));
	}

	/**
	 * Return a new {@link SelectableSet} that includes {@link Selectable} entries with
	 * any of the given names. The resulting set will include any previously made
	 * {@code having...} selections.
	 * @param names the {@link Selectable#name() names} to select
	 * @return a new {@link SelectableSet} instance
	 */
	default S havingNames(String... names) {
		return having(Selector.select().onlyWhenNamed(names));
	}

	/**
	 * Return a new {@link SelectableSet} that includes {@link Selectable} entries with
	 * any of the given names. The resulting set will include any previously made
	 * {@code having...} selections.
	 * @param names the {@link Selectable#name() names} to select
	 * @return a new {@link SelectableSet} instance
	 */
	default S havingNames(Collection<String> names) {
		return having(Selector.select().onlyWhenNamed(names));
	}

	/**
	 * Return a new {@link SelectableSet} that includes {@link Selectable} entries with
	 * the given label key. The resulting set will include any previously made
	 * {@code having...} selections.
	 * @param labelKey the label key to select
	 * @return a new {@link SelectableSet} instance
	 */
	default S havingLabel(String labelKey) {
		return having(Selector.select().onlyWhenLabeled(labelKey));
	}

	/**
	 * Return a new {@link SelectableSet} that includes {@link Selectable} entries with
	 * the given label. The resulting set will include any previously made
	 * {@code having...} selections.
	 * @param label the label to select
	 * @return a new {@link SelectableSet} instance
	 */
	default S havingLabel(Label label) {
		return having(Selector.select().onlyWhenLabeled(label));
	}

	/**
	 * Return a new {@link SelectableSet} that includes {@link Selectable} entries with
	 * the given label key and value. The resulting set will include any previously made
	 * {@code having...} selections.
	 * @param labelKey the label key to select
	 * @param labelValue the label value to select
	 * @return a new {@link SelectableSet} instance
	 */
	default S havingLabel(String labelKey, String labelValue) {
		return having(Selector.select().onlyWhenLabeled(labelKey, labelValue));
	}

	/**
	 * Return a new {@link SelectableSet} that includes {@link Selectable} entries with
	 * the given label key and matching value. The resulting set will include any
	 * previously made {@code having...} selections.
	 * @param labelKey the label key to select
	 * @param predicate a predicate used to test if the label value matches
	 * @return a new {@link SelectableSet} instance
	 */
	default S havingLabel(String labelKey, Predicate<String> predicate) {
		return having(Selector.select().onlyWhenLabeled(labelKey, predicate));
	}

	/**
	 * Return a new {@link SelectableSet} that includes {@link Selectable} entries with
	 * the matching label. The resulting set will include any previously made
	 * {@code having...} selections.
	 * @param predicate a predicate used to test if the label matches
	 * @return a new {@link SelectableSet} instance
	 */
	default S havingLabel(Predicate<Label> predicate) {
		return having(Selector.select().onlyWhenLabeled(predicate));
	}

	/**
	 * Return a new {@link SelectableSet} that includes {@link Selectable} entries that
	 * are {@link Selector#selects(Selectable) selected} by the given {@link Selector}.
	 * The resulting set will include any previously made {@code having...} selections.
	 * @param selector the selector used to test if the {@link Selectable} is included
	 * @return a new {@link SelectableSet} instance
	 */
	default S having(Selector<?> selector) {
		return having(selector::selects);
	}

	/**
	 * Return a new {@link SelectableSet} that includes {@link Selectable} entries that
	 * match the given {@link Predicate}. The resulting set will include any previously
	 * made {@code having...} selections.
	 * @param predicate the predicate used to test if the {@link Selectable} is included
	 * @return a new {@link SelectableSet} instance
	 */
	default S having(Predicate<Selectable> predicate) {
		return having((selectable, element) -> predicate.test(selectable));
	}

	/**
	 * Return a new {@link SelectableSet} that includes {@link Entry entries} that match
	 * the given predicate. The resulting set will include any previously made
	 * {@code having...} selections.
	 * @param predicate the predicate used to test if the {@link Entry} is included
	 * @return a new {@link SelectableSet} instance
	 */
	S having(BiPredicate<Selectable, E> predicate);

	/**
	 * Factory method that can be used to create a {@link SelectableSet} from a
	 * {@link Map}.
	 * @param <S> a self reference for fluent methods
	 * @param <E> the element type of the {@link SelectableSet}
	 * @param map a mapping between the name and the element
	 * @return a new {@link SelectableSet}
	 */
	static <S extends SelectableSet<S, E>, E> SelectableSet<S, E> fromMap(Map<String, E> map) {
		return fromMap(map, null);
	}

	/**
	 * Factory method that can be used to create a {@link SelectableSet} from a
	 * {@link Map}.
	 * @param <S> a self reference for fluent methods
	 * @param <E> the element type of the {@link SelectableSet}
	 * @param map a mapping between the name and the element
	 * @param labelsProvider function that provides {@link Labels} for an element
	 * @return a new {@link SelectableSet}
	 */
	static <S extends SelectableSet<S, E>, E> SelectableSet<S, E> fromMap(Map<String, E> map,
			Function<? super E, Labels> labelsProvider) {
		Assert.notNull(map, "'map' must not be null");
		return from(map.entrySet(), (mapEntry) -> {
			Selectable selectable = Selectable.fromMapEntry(mapEntry, labelsProvider);
			return Entry.ofInstance(selectable, mapEntry.getValue());
		});
	}

	/**
	 * Factory method that can be used to create a {@link SelectableSet} from a
	 * {@link Collection}.
	 * @param <S> a self reference for fluent methods
	 * @param <E> the element type of the {@link SelectableSet}
	 * @param collection a collection of the elements to use
	 * @param nameProvider a function that provides the name of an element
	 * @return a new {@link SelectableSet}
	 * @throws DuplicateSelectableNameException if duplicate {@link Selectable#name()
	 * names} would be added to the set
	 */
	static <S extends SelectableSet<S, E>, E> SelectableSet<S, E> fromCollection(Collection<E> collection,
			Function<? super E, String> nameProvider) throws DuplicateSelectableNameException {
		return fromCollection(collection, nameProvider, null);
	}

	/**
	 * Factory method that can be used to create a {@link SelectableSet} from a
	 * {@link Collection}.
	 * @param <S> a self reference for fluent methods
	 * @param <E> the element type of the {@link SelectableSet}
	 * @param collection a collection of the elements to use
	 * @param nameProvider a function that provides the name of an element
	 * @param labelsProvider a function that provides the {@link Labels} for an element
	 * @return a new {@link SelectableSet}
	 * @throws DuplicateSelectableNameException if duplicate {@link Selectable#name()
	 * names} would be added to the set
	 */
	static <S extends SelectableSet<S, E>, E> SelectableSet<S, E> fromCollection(Collection<E> collection,
			Function<? super E, String> nameProvider, Function<? super E, Labels> labelsProvider)
			throws DuplicateSelectableNameException {
		Assert.notNull(collection, "'collection' must not be null");
		return from(collection.stream(), (element) -> {
			return null;
		});
	}

	/**
	 * Factory method that can be used to create a {@link SelectableSet} from a
	 * {@link Stream}. For simpler use-cases the {@code fromMap(...)},
	 * {@code fromCollection(...)} or {@code #fromIterable(...)} factory methods may be
	 * more suitable.
	 * @param stream a {@link Stream} of objects that will ultimately provide the
	 * {@link Selectable} and element.
	 * @param selectableProvider a function to provide the {@link Selectable} from a
	 * streamed object
	 * @param elementProvider an {@link ElementProvider} to provide the element from a
	 * streamed object
	 * @param <S> a self reference for fluent methods
	 * @param <E> the element type of the {@link SelectableSet}
	 * @param <T> the type of element in the stream
	 * @return a new {@link SelectableSet}
	 * @throws DuplicateSelectableNameException if duplicate {@link Selectable#name()
	 * names} would be added to the set
	 */
	@Deprecated
	static <S extends SelectableSet<S, E>, T, E> SelectableSet<S, E> from(Stream<T> stream,
			Function<? super T, Entry<E>> entryProvider) throws DuplicateSelectableNameException {
		return new SimpleSelectableSet<>(stream, entryProvider);
	}

	/**
	 * Factory method that can be used to create a {@link SelectableSet} from an
	 * {@link Iterable}. For simpler use-cases the {@code fromMap(...)},
	 * {@code fromCollection(...)} or {@code #fromIterable(...)} factory methods may be
	 * more suitable.
	 * @param iterable an {@link Iterable} of objects that will ultimately provide the
	 * {@link Selectable} and element.
	 * @param selectableProvider a function to provide the {@link Selectable} from an
	 * iterated object
	 * @param elementProvider an {@link ElementProvider} to provide the element from a
	 * iterated object
	 * @param <S> a self reference for fluent methods
	 * @param <E> the element type of the {@link SelectableSet}
	 * @param <T> the type of element in the stream
	 * @return a new {@link SelectableSet}
	 * @throws DuplicateSelectableNameException if duplicate {@link Selectable#name()
	 * names} would be added to the set
	 */
	@Deprecated
	static <S extends SelectableSet<S, E>, E, T> SelectableSet<S, E> from(Iterable<T> iterable,
			Function<? super T, Entry<E>> entryProvider) throws DuplicateSelectableNameException {
		return new SimpleSelectableSet<>(iterable, entryProvider);
	}

	/**
	 * Returns an empty selectable set.
	 * @param <S> a self reference for fluent methods
	 * @param <E> the element type of the {@link SelectableSet}
	 * @return an empty {@link SelectableSet}
	 */
	static <S extends SelectableSet<S, E>, E> SelectableSet<S, E> empty() {
		return SimpleSelectableSet.empty();
	}

	/**
	 * A single entry contained in the set.
	 *
	 * @param <E> the element type
	 */
	interface Entry<E> {

		/**
		 * Return {@link Selectable} used with this entry.
		 * @return the element {@link Selectable}
		 */
		Selectable selectable();

		/**
		 * Return element managed by this entry.
		 * @return the element entry
		 */
		E element();

		/**
		 * Factory method to create a new {@link Entry}.
		 * @param <E> the element type
		 * @param selectable the selectable used to select the entry a source
		 * @param element the supplier used to provide the element
		 * @return a new {@link Entry}
		 */
		static <E> Entry<E> ofInstance(Selectable selectable, E element) {
			return of(selectable, () -> element);
		}

		/**
		 * Factory method to create a new {@link Entry}.
		 * @param <E> the element type
		 * @param selectable the selectable used to select the entry a source
		 * @param elementSupplier the supplier used to provide the element
		 * @return a new {@link Entry}
		 */
		static <E> Entry<E> ofSingleton(Selectable selectable, Supplier<E> elementSupplier) {
			return of(selectable, SingletonSupplier.of(elementSupplier));
		}

		/**
		 * Factory method to create a new {@link Entry}.
		 * @param <E> the element type
		 * @param selectable the selectable used to select the entry a source
		 * @param elementSupplier the supplier used to provide the element
		 * @return a new {@link Entry}
		 */
		static <E> Entry<E> of(Selectable selectable, Supplier<E> elementSupplier) {
			return new SimpleSelectableSetEntry<>(selectable, elementSupplier);
		}

		static <E> Entry<E> fromInstance(E element, Function<? super E, String> nameProvider,
				Function<? super E, Labels> labelsProvider) {
			throw new RuntimeException();
		}

	}

}
