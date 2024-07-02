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
 * @author pwebb
 */
public interface JsonWriter<T> {

	WritableJson write(T instance);

	static <T> JsonWriter<T> of(Consumer<JsonNotation<T>> dunno) {
		return null;
	}

	static <T> JsonWriter<T> of(BiConsumer<JsonNotation<T>, T> dunno) {
		return null;
	}

	interface JsonNotation<T> {

		<V> JsonMember<V> add(String key, Supplier<V> valueSupplier);

		<V> JsonMember<V> add(String key, Function<T, V> valueExtractor);

		<V> JsonMember<V> add(String key, V value);

		<V> JsonMember<V> add(Supplier<V> supplier);

		<V> JsonMember<V> add(Function<T, V> extractor);

		<V> JsonMember<V> add(V value);

	}

	interface JsonMember<T> {

		JsonMember<T> when(Predicate<T> predicate);

		JsonMember<T> whenNot(Predicate<T> predicate);

		JsonMember<T> whenNotNull();

		JsonMember<T> whenHasLength();

		<R> JsonMember<R> as(Function<T, R> adapter);

		JsonMember<T> asJson(Consumer<JsonNotation<T>> dunno); // FIXME what return type?

		JsonMember<T> asJson(BiConsumer<JsonNotation<T>, T> dunno); // FIXME what return

	}

	interface WritableJson {

		void to(Appendable appendable);

	}

}
