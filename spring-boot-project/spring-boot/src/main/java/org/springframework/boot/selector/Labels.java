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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * A set of {@link Label} instances that all have different keys.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface Labels extends Iterable<Label> {

	/**
	 * An empty label collection.
	 */
	Labels NONE = SimpleLabels.NONE;

	/**
	 * Get the label with the specified key.
	 * @param key the label key
	 * @return the Label
	 * @throws NoSuchLabelKeyException if there is no label with the given key
	 */
	Label get(String key) throws NoSuchLabelKeyException;

	/**
	 * Return {@code true} if there are no labels.
	 * @return if the labels are empty;
	 */
	default boolean isEmpty() {
		return !iterator().hasNext();
	}

	/**
	 * Determine if a matching label is contained in this set.
	 * @param label the label to check
	 * @return {@code true} if the set contains a matching label
	 */
	default boolean contains(Label label) {
		return contains((candidate) -> candidate.matches(label));
	}

	/**
	 * Determine if a label with a matching key is contained in this set.
	 * @param key the label key to check
	 * @return {@code true} if the set contains a matching label
	 */
	default boolean contains(String key) {
		return contains((candidate) -> candidate.matches(key));
	}

	/**
	 * Determine if a matching label is contained in this set.
	 * @param key the label key to check
	 * @param value the required value
	 * @return {@code true} if the set contains a matching label
	 */
	default boolean contains(String key, String value) {
		return contains((candidate) -> candidate.matches(key, value));
	}

	/**
	 * Determine if a matching label is contained in this set.
	 * @param key the label key to check
	 * @param predicate a predicate used to check the label value
	 * @return {@code true} if the set contains a matching label
	 */
	default boolean contains(String key, Predicate<String> predicate) {
		return contains((candidate) -> candidate.matches(key, predicate));
	}

	/**
	 * Determine if a matching label is contained in this set.
	 * @param predicate a predicate to check labels
	 * @return {@code true} if the set contains a matching label
	 */
	default boolean contains(Predicate<Label> predicate) {
		return stream().anyMatch(predicate);
	}

	/**
	 * Stream the labels from this set.
	 * @return a stream of {@link Label} instances
	 */
	Stream<Label> stream();

	/**
	 * Factory method to create {@link Labels} from the given {@link Label} instances.
	 * @param labels the contained labels
	 * @return a new {@link Labels} instance
	 * @throws DuplicateLabelKeyException if duplicate {@link Label#key() label keys}
	 * would be added to the set
	 */
	static Labels of(Label... labels) throws DuplicateLabelKeyException {
		return (!ObjectUtils.isEmpty(labels)) ? of(Arrays.stream(labels)) : NONE;
	}

	/**
	 * Factory method to create {@link Labels} from the given {@link Label} instances.
	 * @param labels the contained labels
	 * @return a new {@link Labels} instance
	 * @throws DuplicateLabelKeyException if duplicate {@link Label#key() label keys}
	 * would be added to the set
	 */
	static Labels of(Collection<Label> labels) throws DuplicateLabelKeyException {
		return (!CollectionUtils.isEmpty(labels)) ? of(labels.stream()) : NONE;
	}

	/**
	 * Factory method to create {@link Labels} from the given {@link Label} instances.
	 * @param labels the contained labels
	 * @return a new {@link Labels} instance
	 * @throws DuplicateLabelKeyException if duplicate {@link Label#key() label keys}
	 * would be added to the set
	 */
	static Labels of(Stream<Label> labels) throws DuplicateLabelKeyException {
		return SimpleLabels.of(labels);
	}

	/**
	 * Factory method to create {@link Labels} from the given Map.
	 * @param map the map of label key to label value
	 * @return a new {@link Labels} instance
	 */
	static Labels fromMap(Map<String, String> map) {
		return (!CollectionUtils.isEmpty(map)) ? of(map.entrySet().stream().map(Label::fromMapEntry)) : NONE;
	}

}
