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
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.util.Assert;

/**
 * Interface that can be used {@link #selects(Selectable) select} a {@link Selectable}
 * based on various criteria.
 *
 * @param <S> a self reference for fluent methods
 * @author Phillip Webb
 * @since 4.0.0
 * @see #select()
 * @see Selectable
 * @see SelectableSet
 */
public interface Selector<S extends Selector<S>> {

	/**
	 * Test if this selector selects the given {@link Selectable}.
	 * @param selectable an item that may be selected
	 * @return {@code true} if the item is selected
	 */
	default boolean selects(Selectable selectable) {
		return true;
	}

	/**
	 * Return a new {@link Selector} further limits selection to {@link Selectable} items
	 * with the given name. The resulting set will include any previously made
	 * {@code having...} selections.
	 * @param name the {@link Selectable#name() name} to select
	 * @return a new {@link Selector} instance
	 */
	default S onlyWhenNamed(String name) {
		return onlyWhen((selectable) -> selectable.name().equals(name));
	}

	/**
	 * Return a new {@link Selector} further limits selection to {@link Selectable} items
	 * with a name that matches the given predicate. The resulting set will include any
	 * previously made {@code having...} selections.
	 * @param predicate a predicate used to test if the name matches
	 * @return a new {@link Selector} instance
	 */
	default S onlyWhenNamed(Predicate<String> predicate) {
		Assert.notNull(predicate, "'predicate' must not be null");
		return onlyWhen((selectable) -> predicate.test(selectable.name()));
	}

	/**
	 * Return a new {@link Selector} further limits selection to {@link Selectable} items
	 * with any of the given names. The resulting selector will also apply any previously
	 * made {@code onlyWhen...} restrictions.
	 * @param names the {@link Selectable#name() names} to select
	 * @return a new {@link Selector} instance
	 */
	default S onlyWhenNamed(String... names) {
		return onlyWhenNamed(Set.of(names));
	}

	/**
	 * Return a new {@link Selector} further limits selection to {@link Selectable} items
	 * any of the given names. The resulting selector will also apply any previously made
	 * {@code onlyWhen...} restrictions.
	 * @param names the {@link Selectable#name() names} to select
	 * @return a new {@link Selector} instance
	 */
	default S onlyWhenNamed(Collection<String> names) {
		Assert.notNull(names, "'names' must not be null");
		Assert.noNullElements(names, "'names' must not contain null elements");
		return onlyWhen((selectable) -> names.contains(selectable.name()));
	}

	/**
	 * Return a new {@link Selector} further limits selection to {@link Selectable} items
	 * with the given label key. The resulting selector will also apply any previously
	 * made {@code onlyWhen...} restrictions.
	 * @param labelKey the label key to select
	 * @return a new {@link Selector} instance
	 */
	default S onlyWhenLabeled(String labelKey) {
		Assert.notNull(labelKey, "'labelKey' must not be null");
		return onlyWhen((selectable) -> selectable.labels().contains(labelKey));
	}

	/**
	 * Return a new {@link Selector} further limits selection to {@link Selectable} items
	 * with the given label. The resulting selector will also apply any previously made
	 * {@code onlyWhen...} restrictions.
	 * @param label the label to select
	 * @return a new {@link Selector} instance
	 */
	default S onlyWhenLabeled(Label label) {
		Assert.notNull(label, "'label' must not be null");
		return onlyWhen((selectable) -> selectable.labels().contains(label));
	}

	/**
	 * Return a new {@link Selector} further limits selection to {@link Selectable} items
	 * with the given label key and value. The resulting selector will also apply any
	 * previously made {@code onlyWhen...} restrictions.
	 * @param labelKey the label key to select
	 * @param labelValue the label value to select
	 * @return a new {@link Selector} instance
	 */
	default S onlyWhenLabeled(String labelKey, String labelValue) {
		Assert.notNull(labelKey, "'labelKey' must not be null");
		return onlyWhen((selectable) -> selectable.labels().contains(labelKey, labelValue));
	}

	/**
	 * Return a new {@link Selector} further limits selection to {@link Selectable} items
	 * with the given label key and matching value. The resulting selector will also apply
	 * any previously made {@code onlyWhen...} restrictions.
	 * @param labelKey the label key to select
	 * @param predicate a predicate used to test if the label value matches
	 * @return a new {@link Selector} instance
	 */
	default S onlyWhenLabeled(String labelKey, Predicate<String> predicate) {
		Assert.notNull(labelKey, "'labelKey' must not be null");
		return onlyWhen((selectable) -> selectable.labels().contains(labelKey, predicate));
	}

	/**
	 * Return a new {@link Selector} further limits selection to {@link Selectable} items
	 * with the matching label.The resulting selector will also apply any previously made
	 * {@code onlyWhen...} restrictions.
	 * @param predicate a predicate used to test if the label matches
	 * @return a new {@link Selector} instance
	 */
	default S onlyWhenLabeled(Predicate<Label> predicate) {
		return onlyWhen((selectable) -> selectable.labels().contains(predicate));
	}

	/**
	 * Return a new {@link Selector} further limits selection to {@link Selectable} items
	 * that are {@link Selector#selects(Selectable) selected} by the given
	 * {@link Selector}. The resulting selector will also apply any previously made
	 * {@code onlyWhen...} restrictions.
	 * @param selector the selector used to select
	 * @return a new {@link Selector} instance
	 */
	default S onlyWhen(Selector<?> selector) {
		Assert.notNull(selector, "'selector' must not be null");
		return onlyWhen(selector::selects);
	}

	/**
	 * Return a new {@link Selector} further limits selection to {@link Selectable} items
	 * that are {@link Selector#selects(Selectable) selected} by the given
	 * {@link Predicate}. The resulting selector will also apply any previously made
	 * {@code onlyWhen...} restrictions.
	 * @param predicate the predicate used to select
	 * @return a new {@link Selector} instance
	 */
	S onlyWhen(Predicate<Selectable> predicate);

	/**
	 * Factory method used to create a simple {@link Selector} instance.
	 * @param <S> a self reference for fluent methods (should be a vanilla
	 * {@link Selector})
	 * @return a selector instance that can be used to apply further selections.
	 */
	static <S extends Selector<S>> S select() {
		return SimpleSelector.instance();
	}

}
