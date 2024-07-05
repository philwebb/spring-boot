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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @param <T> The type being written
 * @since 3.4.0
 */
@FunctionalInterface
public interface JsonWriter<T> {

	default String writeToString(T instance) {
		StringBuilder out = new StringBuilder();
		write(instance, out);
		return out.toString();
	}

	default void write(T instance, StringBuilder out) {
	}

	void write(T instance, Appendable out) throws IOException;

	default JsonWriter<T> withNewLineAtEnd() {
		return withSuffix("\n");
	}

	default JsonWriter<T> withSuffix(String suffix) {
		return (instance, out) -> {
			write(instance, out);
			append(out, suffix);
		};
	}

	private static void append(Appendable out, char ch) {
		try {
			out.append(ch);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static void append(Appendable out, CharSequence value) {
		try {
			out.append(value);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	static <T> JsonWriter<T> of(Consumer<Members<T>> members) {
		return accept(members, new Members<>(), Members::writer);
	}

	// FIXME listOf() arrayOf()

	private static <T, R> R accept(Consumer<T> consumer, T value, Function<T, R> finalizer) {
		consumer.accept(value);
		return finalizer.apply(value);
	}

	public static final class Members<T> {

		private final List<Member<?>> members = new ArrayList<>();

		Members() {
		}

		public Member<T> addSelf(String key) {
			return add(key, (instance) -> instance);
		}

		public <V> Member<V> add(String key, V value) {
			return add(key, (instance) -> value);
		}

		public <V> Member<V> add(String key, Supplier<V> supplier) {
			return add(key, (instance) -> supplier.get());
		}

		@SuppressWarnings("unchecked")
		public <V> Member<V> add(String key, Function<T, V> extractor) {
			Assert.notNull(key, "'key' must not be null");
			Assert.notNull(extractor, "'extractor' must not be null");
			return addMember((instance, pairs) -> pairs.accept(key, extractor.apply((T) instance)));
		}

		public Member<T> addSelf() {
			return add((instance) -> instance);
		}

		public <M extends Map<K, V>, K, V> Member<M> addMapEntries(Function<T, M> extractor) {
			return add(extractor).usingPairs(Map::forEach);
		}

		public <V> Member<V> add(V value) {
			return add((instance) -> value);
		}

		public <V> Member<V> add(Supplier<V> supplier) {
			return add((instance) -> supplier.get());
		}

		public <V> Member<V> add(Function<T, V> extractor) {
			return new Member<>(null);
		}

		private <V> Member<V> addMember(Member.Writer<V> memberWriter) {
			Member<V> member = new Member<>(memberWriter);
			this.members.add(member);
			return member;
		}

		JsonWriter<T> writer() {
			// List<Member.Writer> memberWriters =
			// this.members.stream().map(Member::writer).toList();
			// return JsonWriter.using((instance, valueWriter) ->
			// valueWriter.writePairs((pairs) -> {
			// for (Member.Writer memberWriter : memberWriters) {
			// memberWriter.write(instance, pairs);
			// }
			// }));
			return null;
		}

	}

	public static final class Member<T> {

		private Predicate<T> predicate = (instance) -> true;

		private Function<T, Member<?>> delegate;

		public Member<T> whenNotNull() {
			return when(Objects::nonNull);
		}

		public Member<T> whenNotNull(Function<T, ?> extractor) {
			return when((instance) -> Objects.nonNull(extractor.apply(instance)));
		}

		public Member<T> whenHasLength() {
			return when((instance) -> instance != null && StringUtils.hasLength(instance.toString()));
		}

		public Member<T> whenNotEmpty() {
			return whenNot(ObjectUtils::isEmpty);
		}

		public Member<T> whenNot(Predicate<T> predicate) {
			Assert.notNull(predicate, "'predicate' must not be null");
			return when(predicate.negate());
		}

		@SuppressWarnings("unchecked")
		public Member<T> when(Predicate<T> predicate) {
			Assert.notNull(predicate, "'predicate' must not be null");
			this.predicate = this.predicate.and(predicate);
			return this;
		}

		@SuppressWarnings("unchecked")
		public <R> Member<R> as(Function<T, R> adapter) {
			Assert.notNull(adapter, "'adapter' must not be null");
			return this;
		}

		public <E, K, V> Member<T> usingElements(BiConsumer<T, Consumer<E>> elements, Class<E> elementType,
				Function<E, K> keyExtractor, Function<E, V> valueExtractor) {
			return usingElements(elements, keyExtractor, valueExtractor);
		}

		public <E, K, V> Member<T> usingElements(BiConsumer<T, Consumer<E>> elements, Function<E, K> keyExtractor,
				Function<E, V> valueExtractor) {
			return this;
		}

		public <K, V> Member<T> usingPairs(BiConsumer<T, BiConsumer<K, V>> pairs) {
			return this;
		}

		public Member<T> usingMembers(Consumer<Members<T>> members) {
			return this;
		}

	}

}
