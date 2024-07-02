/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.json;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @since 3.4.0
 */
@FunctionalInterface
public interface JsonWriter<T> {

	WritableJson write(T instance);

	static <T> JsonWriter<T> of(Consumer<Members<T>> consumer) {
		return null;
	}

	static <T> JsonWriter<T> using(BiConsumer<T, ValueWriter> dunno) {
		return null;
	}

	interface ValueWriter {

		<V> void write(String key, V value);

		<V> void write(V value);

	}

	// FIXME class
	interface WritableJson {

		void to(Appendable appendable);

	}

	// FIXME class
	interface Members<T> {

		<V> Member<V> add(String key, Supplier<V> supplier);

		<V> Member<V> add(String key, Function<T, V> extractor);

		<V> Member<V> add(String key, V value);

		<V> Member<V> add(Supplier<V> supplier);

		<V> Member<V> add(Function<T, V> extractor);

		<V> Member<V> add(V value);

	}

	// FIXME class
	interface Member<T> {

		Member<T> when(Predicate<T> predicate);

		Member<T> whenNot(Predicate<T> predicate);

		Member<T> whenNotNull();

		Member<T> whenHasLength();

		<R> Member<R> as(Function<T, R> adapter);

		void asJson(Consumer<Members<T>> dunno);

		void asJson(JsonWriter<T> dunno);

	}

}
