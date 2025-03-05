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

import org.springframework.util.Assert;

/**
 * An item that can be selected by its name or labels.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see Selector
 * @see SelectableSet
 */
public interface Selectable {

	/**
	 * The item name or an empty {@link String} if the there is no name. This method must
	 * never return {@code null}.
	 * @return the name of the item
	 */
	String name();

	/**
	 * The item labels or {@link Labels#NONE} if there are no labels. This method must
	 * never return {@code null}.
	 * @return the item labels
	 */
	Labels labels();

	/**
	 * Factory method to create simple {@link Selectable} consisting of only a name.
	 * @param name the name of the selectable
	 * @return a simple {@link Selectable} instance
	 */
	static Selectable of(String name) {
		return SimpleSelectable.of(name, Labels.NONE);
	}

	/**
	 * Factory method to create simple {@link Selectable} consisting of only a name and
	 * some labels.
	 * @param name the name of the selectable
	 * @param labels the labels of the selectable or {@code null} to use
	 * {@link Labels#NONE}
	 * @return a simple {@link Selectable} instance
	 */
	static Selectable of(String name, Map<String, String> labels) {
		return SimpleSelectable.of(name, Labels.fromMap(labels));
	}

	/**
	 * Factory method to create simple {@link Selectable} consisting of only a name and
	 * some labels.
	 * @param name the name of the selectable
	 * @param labels the labels of the selectable or {@code null} to use
	 * {@link Labels#NONE}
	 * @return a simple {@link Selectable} instance
	 */
	static Selectable of(String name, Labels labels) {
		return SimpleSelectable.of(name, labels);
	}

	/**
	 * Factory method to create a {@link Selectable} from a {@link Map} entry.
	 * @param <V> the map entry value type
	 * @param mapEntry the map entry
	 * @param labelsProvider a function to provide the label given the map value (may be
	 * {@code null} if there are no labels)
	 * @return a new {@link Selectable} instance
	 */
	static <V> Selectable fromMapEntry(Map.Entry<String, V> mapEntry, Function<? super V, Labels> labelsProvider) {
		Assert.notNull(mapEntry, "'mapEntry' must not be null");
		Assert.notNull(mapEntry.getKey(), "'mapEntry' must not have an emtpty key");
		V value = mapEntry.getValue();
		return of(mapEntry.getKey(), (labelsProvider != null) ? labelsProvider.apply(value) : null);
	}

	/**
	 * Factory method to create a {@link Selectable} from any {@link Object}.
	 * @param <T> the object type
	 * @param object the source object
	 * @param nameProvider a function to provide the name of the object or {@code null} to
	 * use the object's {@code toString}.
	 * @param labelsProvider a function to provide the label of the object (may be
	 * {@code null} if there are no labels)
	 * @return a new {@link Selectable} instance
	 */
	static <T> Selectable from(T object, Function<? super T, String> nameProvider,
			Function<? super T, Labels> labelsProvider) {
		Assert.notNull(object, "'Object' must not be null");
		String name = (nameProvider != null) ? nameProvider.apply(object) : object.toString();
		Labels labels = (labelsProvider != null) ? labelsProvider.apply(object) : null;
		return of(name, labels);
	}

	/**
	 * A special {@link Selectable} that has no name or label. Can be used to filter
	 * {@link Selector selectors} to those not having any {@code onlyWhen...}
	 * restrictions.
	 * @return a blank {@link Selectable}
	 */
	static Selectable blank() {
		return SimpleSelectable.BLANK;
	}

}
