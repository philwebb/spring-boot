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
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.springframework.boot.selector.SelectableSet.ElementProvider.Scope;

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
		Stream<Map.Entry<String, E>> stream = (map != null) ? map.entrySet().stream() : Stream.empty();
		return from(stream, (entry) -> Selectable.fromMapEntry(entry, labelsProvider), Map.Entry::getValue);
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
		return fromIterable(collection, nameProvider, labelsProvider);
	}

	/**
	 * Factory method that can be used to create a {@link SelectableSet} from a
	 * {@link Collection}.
	 * @param <S> a self reference for fluent methods
	 * @param <E> the element type of the {@link SelectableSet}
	 * @param iterable an {@link Iterable} of the elements to use
	 * @param nameProvider a function that provides the name of an element
	 * @return a new {@link SelectableSet}
	 * @throws DuplicateSelectableNameException if duplicate {@link Selectable#name()
	 * names} would be added to the set
	 */
	static <S extends SelectableSet<S, E>, E> SelectableSet<S, E> fromIterable(Iterable<E> iterable,
			Function<? super E, String> nameProvider) throws DuplicateSelectableNameException {
		return fromIterable(iterable, nameProvider, null);
	}

	/**
	 * Factory method that can be used to create a {@link SelectableSet} from a
	 * {@link Collection}.
	 * @param <S> a self reference for fluent methods
	 * @param <E> the element type of the {@link SelectableSet}
	 * @param iterable an {@link Iterable} of the elements to use
	 * @param nameProvider a function that provides the name of an element or {@code null}
	 * to use the elements {@code toString}
	 * @param labelsProvider a function that provides the {@link Labels} for an element
	 * @return a new {@link SelectableSet}
	 * @throws DuplicateSelectableNameException if duplicate {@link Selectable#name()
	 * names} would be added to the set
	 */
	static <S extends SelectableSet<S, E>, E> SelectableSet<S, E> fromIterable(Iterable<E> iterable,
			Function<? super E, String> nameProvider, Function<? super E, Labels> labelsProvider)
			throws DuplicateSelectableNameException {
		return from(iterable, (element) -> Selectable.from(element, nameProvider, labelsProvider),
				ElementProvider.identity(Scope.SINGLETON));
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
	static <S extends SelectableSet<S, E>, E, T> SelectableSet<S, E> from(Stream<T> stream,
			Function<? super T, Selectable> selectableProvider, ElementProvider<? super T, E> elementProvider)
			throws DuplicateSelectableNameException {
		return (stream != null) ? from(stream::iterator, selectableProvider, elementProvider) : empty();
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
	static <S extends SelectableSet<S, E>, E, T> SelectableSet<S, E> from(Iterable<T> iterable,
			Function<? super T, Selectable> selectableProvider, ElementProvider<? super T, E> elementProvider)
			throws DuplicateSelectableNameException {
		return SimpleSelectableSet.from(iterable, selectableProvider, elementProvider);
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
		 * Return element managed by this entry.
		 * @return the element entry
		 */
		E element();

		/**
		 * Return {@link Selectable} used with this entry.
		 * @return the element {@link Selectable}
		 */
		Selectable selectable();

		/**
		 * Factory method to create a new {@link Entry}.
		 * @param <T> the source type
		 * @param <E> the element type
		 * @param selectable the selectable used to select the entry
		 * @param source the source
		 * @param elementProvider an {@link ElementProvider} to provider the element from
		 * a source
		 * @return a new {@link Entry}
		 */
		static <T, E> Entry<E> of(Selectable selectable, T source, ElementProvider<T, E> elementProvider) {
			return new SimpleSelectableSetEntry<>(selectable, elementProvider.asScopedSupplier(source));
		}

	}

	/**
	 * Functional interface used to provide an element within a specific scope.
	 *
	 * @param <T> the source type
	 * @param <E> the element type
	 */
	@FunctionalInterface
	interface ElementProvider<T, E> {

		/**
		 * Return the the scope of the element.
		 * @return the element scope
		 */
		default Scope getScope() {
			return Scope.SINGLETON;
		}

		/**
		 * Get the provided element.
		 * @param source the source of the element
		 * @return the element
		 */
		E getElement(T source);

		/**
		 * Return a {@link Supplier} that will supply the element respecting
		 * {@link #getScope()}.
		 * @param source the source of the element
		 * @return a supplier that supplies the element by calling
		 * {@link #getElement(Object)} only when necessary.
		 */
		default Supplier<E> asScopedSupplier(T source) {
			return SimpleSelectableSetElementProvider.asScopedSupplier(this, source);
		}

		/**
		 * Factory method to create an {@link ElementProvider} from the given
		 * {@link Function} in {@link Scope#SINGLETON singleton scope}.
		 * @param <T> the source type
		 * @param <E> the element type
		 * @param function a function the provides the element
		 * @return a new {@link ElementProvider} instance
		 */
		static <T, E> ElementProvider<T, E> ofSingleton(Function<? super T, E> function) {
			return of(Scope.SINGLETON, function);
		}

		/**
		 * Factory method to create an {@link ElementProvider} from the given
		 * {@link Function} in {@link Scope#PROTOTYPE prototype scope}.
		 * @param <T> the source type
		 * @param <E> the element type
		 * @param function a function the provides the element
		 * @return a new {@link ElementProvider} instance
		 */
		static <T, E> ElementProvider<T, E> ofPrototype(Function<? super T, E> function) {
			return of(Scope.PROTOTYPE, function);
		}

		/**
		 * Factory method to create an {@link ElementProvider} from the given
		 * {@link Function} in the specified {@link Scope}.
		 * @param <T> the source type
		 * @param <E> the element type
		 * @param scope the scope
		 * @param function a function the provides the element
		 * @return a new {@link ElementProvider} instance.
		 */
		static <T, E> ElementProvider<T, E> of(Scope scope, Function<? super T, E> function) {
			return new SimpleSelectableSetElementProvider<>(scope, function);
		}

		/**
		 * Returns an {@link ElementProvider} that always returns its input argument.
		 * @param <T> the source type
		 * @param scope the scope of the element provider
		 * @return a new {@link ElementProvider} instance.
		 */
		static <T> ElementProvider<T, T> identity(Scope scope) {
			return of(scope, Function.identity());
		}

		/**
		 * Return new {@link ElementProvider} that applies the given post processor.
		 * @param postProcessor the post processor to apply
		 * @return a new element provider
		 */
		default ElementProvider<T, E> withPostProcessor(UnaryOperator<E> postProcessor) {
			if (postProcessor == null) {
				return this;
			}
			Function<T, E> function = this::getElement;
			return new SimpleSelectableSetElementProvider<>(getScope(), function.andThen(postProcessor));
		}

		/**
		 * The scope of an element.
		 */
		enum Scope {

			/**
			 * Singleton scope where the {@link ElementProvider#getElement(Object)} method
			 * will be called the first time that the element is needed.
			 */
			SINGLETON,

			/**
			 * Prototype scope where the {@link ElementProvider#getElement(Object)} method
			 * will be called the each time that the element is needed.
			 */
			PROTOTYPE

		}

	}

}
