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

import org.springframework.boot.selector.SelectableSet.ElementProvider;
import org.springframework.util.function.SingletonSupplier;

/**
 * Simple {@link SelectableSet.ElementProvider} implementation.
 *
 * @author Phillip Webb
 * @param <T> the source type
 * @param <E> the element type
 * @param scope the scope of the element
 * @param function the function used to create the element
 */
record SimpleSelectableSetElementProvider<T, E>(Scope scope,
		Function<? super T, E> function) implements SelectableSet.ElementProvider<T, E> {

	@Override
	public Scope getScope() {
		return scope();
	}

	@Override
	public E getElement(T source) {
		return this.function.apply(source);
	}

	static <T, E> Supplier<E> asScopedSupplier(ElementProvider<T, E> elementProvider, T source) {
		Scope scope = elementProvider.getScope();
		Supplier<E> instanceSupplier = () -> elementProvider.getElement(source);
		return (!Scope.PROTOTYPE.equals(scope)) ? SingletonSupplier.of(instanceSupplier) : instanceSupplier;
	}

}
