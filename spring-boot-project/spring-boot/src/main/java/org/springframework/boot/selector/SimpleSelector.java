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

import java.util.function.Predicate;

/**
 * Simple {@link Selector} implementation.
 *
 * @param <S> a self reference for fluent methods
 * @author Phillip Webb
 */
class SimpleSelector<S extends Selector<S>> implements Selector<S> {

	private static final SimpleSelector<?> INSTANCE = new SimpleSelector<>();

	@SuppressWarnings("unchecked")
	static <S extends Selector<S>> S instance() {
		return (S) INSTANCE;
	}

	@Override
	@SuppressWarnings("unchecked")
	public S onlyWhen(Predicate<Selectable> predicate) {
		return (S) new SimpleSelector<S>() {

			@Override
			public boolean selects(Selectable selectable) {
				return (selectable != null) && SimpleSelector.this.selects(selectable) && predicate.test(selectable);
			}

		};
	}

}
