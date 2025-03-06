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
import java.util.function.Predicate;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A key/value pair attached to a {@link Selectable} specifying identifying attributes
 * which may be used to limit selections.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface Label {

	/**
	 * The label key (never @code null}.
	 * @return the key of the label
	 */
	String key();

	/**
	 * The label value or {@code null} if no value is present.
	 * @return the value of the label
	 */
	String value();

	/**
	 * Check if this label matches another.
	 * @param labelKey the label key to check
	 * @return if the label matches
	 */
	default boolean matches(String labelKey) {
		return (labelKey != null) && ObjectUtils.nullSafeEquals(key(), labelKey);
	}

	/**
	 * Check if this label matches another.
	 * @param label the label to check
	 * @return if the label matches
	 */
	default boolean matches(Label label) {
		return (label != null) && matches(label.key(), label.value());
	}

	/**
	 * Check if this label matches another.
	 * @param labelKey the required label key
	 * @param labelValue the required label value
	 * @return if the label matches
	 */
	default boolean matches(String labelKey, String labelValue) {
		return ObjectUtils.nullSafeEquals(key(), labelKey) && ObjectUtils.nullSafeEquals(value(), labelValue);
	}

	/**
	 * Check if this label matches another.
	 * @param labelKey the required label key
	 * @param predicate a predicate used to check if the label value matches
	 * @return if the label matches
	 */
	default boolean matches(String labelKey, Predicate<String> predicate) {
		Assert.notNull(predicate, "'predicate' must not be null");
		return ObjectUtils.nullSafeEquals(key(), labelKey) && (value() != null) && predicate.test(value());
	}

	/**
	 * Check if this label matches another.
	 * @param predicate a predicate used to check if the label matches
	 * @return if the label matches
	 */
	default boolean matches(Predicate<Label> predicate) {
		Assert.notNull(predicate, "'predicate' must not be null");
		return predicate.test(this);
	}

	/**
	 * Factory method to create a label with the specified key and no value.
	 * @param key the label key
	 * @return a new {@link Label} instance
	 */
	static Label of(String key) {
		return of(key, null);
	}

	/**
	 * Factory method to create a label with the specified key and value.
	 * @param key the label key
	 * @param value the label value
	 * @return a new {@link Label} instance
	 */
	static Label of(String key, String value) {
		Assert.hasText(key, "'key' must not be empty");
		return new SimpleLabel(key, value);
	}

	/**
	 * Factory method to create a label from the given map entry.
	 * @param mapEntry the label map entry
	 * @return a new {@link Label} instance
	 */
	static Label fromMapEntry(Map.Entry<String, String> mapEntry) {
		Assert.notNull(mapEntry, "'mapEntry' must not be empty");
		Assert.hasText(mapEntry.getKey(), "'mapEntry' must not have empty key");
		return new SimpleLabel(mapEntry.getKey(), mapEntry.getValue());
	}

}
