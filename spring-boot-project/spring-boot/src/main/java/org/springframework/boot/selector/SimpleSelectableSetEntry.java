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

import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.boot.selector.SelectableSet.Entry;
import org.springframework.util.Assert;

/**
 * Simple {@link SelectableSet.Entry} implementation.
 *
 * @param <E> the element type
 * @param selectable the selectable
 * @param elementSupplier the supplier of the element
 * @author Phillip Webb
 */
record SimpleSelectableSetEntry<E>(Selectable selectable,
		Supplier<E> elementSupplier) implements SelectableSet.Entry<E> {

	@Override
	public E element() {
		return elementSupplier().get();
	}

	@Override
	public final String toString() {
		return toString(this);
	}

	static <E> String toString(SelectableSet.Entry<E> entry) {
		return "%s -> '%s'".formatted(entry.selectable(), entry.element());
	}

	static <E> Entry<E> of(Selectable selectable, Supplier<E> elementSupplier) {
		Assert.notNull(selectable, "'selectable' must not be null");
		Assert.notNull(elementSupplier, "'elementSupplier' must not be null");
		return new SimpleSelectableSetEntry<>(selectable, elementSupplier);
	}

	static <E> Entry<E> fromInstance(E element, Function<? super E, String> nameProvider,
			Function<? super E, Labels> labelsProvider) {
		Assert.notNull(element, "'element' must not be null");
		Assert.notNull(nameProvider, "'nameProvider' must not be null");
		Selectable selectable = Selectable.from(element, nameProvider, labelsProvider);
		return new SimpleSelectableSetEntry<>(selectable, () -> element);

	}

}
