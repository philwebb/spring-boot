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

import org.springframework.boot.json.JsonWriter2.ValueWriter;
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

	void write(T instance, Appendable out) throws IOException;

	default String writeToString(T instance) {
		return write(instance).toString();
	}

	default WritableJson<T> write(T instance) {
		return new WritableJson<>(this, instance);
	}

	default JsonWriter<T> withNewLineAtEnd() {
		return withSuffix("\n");
	}

	default JsonWriter<T> withSuffix(String suffix) {
		if (!StringUtils.hasLength(suffix)) {
			return this;
		}
		return (instance, out) -> {
			write(instance, out);
			out.append(suffix);
		};
	}

	static <T> JsonWriter<T> standard() {
		return of((members) -> members.addSelf());
	}

	static <T> JsonWriter<T> of(Consumer<Members<T>> members) {
		Members<T> delegate = new Members<>();
		members.accept(delegate);
		delegate.check();
		return (instance, out) -> delegate.write(instance, new JsonValueWriter(out));
	}

	static <T> JsonWriter<T> ofFormatString(String json) {
		return (instance, out) -> out.append(json.formatted(instance));
	}

	// FIXME listOf() arrayOf()

	public static class WritableJson<T> {

		private final JsonWriter<T> writer;

		private final T instance;

		WritableJson(JsonWriter<T> writer, T instance) {
			this.writer = writer;
			this.instance = instance;
		}

		@Override
		public String toString() {
			StringBuilder stringBuilder = new StringBuilder();
			to(stringBuilder);
			return stringBuilder.toString();
		}

		public void to(StringBuilder out) {
			try {
				this.writer.write(this.instance, out);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

		public void to(Appendable out) {
			try {
				this.writer.write(this.instance, out);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

	}

	public static final class Members<T> {

		private final List<Member<?>> members = new ArrayList<>();

		Members() {
		}

		public Member<T> addSelf(String name) {
			return add(name, (instance) -> instance);
		}

		public <V> Member<V> add(String name, V value) {
			return add(name, (instance) -> value);
		}

		public <V> Member<V> add(String name, Supplier<V> supplier) {
			return add(name, (instance) -> supplier.get());
		}

		public <V> Member<V> add(String name, Function<T, V> extractor) {
			Assert.notNull(name, "'name' must not be null");
			Assert.notNull(extractor, "'extractor' must not be null");
			return addMember(name, extractor);
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
			Assert.notNull(extractor, "'extractor' must not be null");
			return addMember(null, extractor);
		}

		private <V> Member<V> addMember(String name, Function<T, V> extractor) {
			Member<V> member = new Member<>(name, extractor);
			this.members.add(member);
			return member;
		}

		private void check() {
			// FIXME check not mixed types
			this.members.forEach(Member::check);
		}

		private void write(T instance, JsonValueWriter valueWriter) {
			this.members.get(0).write(instance, valueWriter);
		}

		BiConsumer<T, ValueWriter> writeAction() {
			return null;
		}

	}

	public static final class Member<T> {

		private final String name;

		private final Function<?, T> extractor;

		private Predicate<T> predicate = (instance) -> true;

		private BiConsumer<T, JsonValueWriter> writeAction;

		Member(String name, Function<?, T> extractor) {
			this.name = name;
			this.extractor = extractor;
		}

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

		public Member<T> when(Predicate<T> predicate) {
			Assert.notNull(predicate, "'predicate' must not be null");
			this.predicate = this.predicate.and(predicate);
			return this;
		}

		public <R> Member<R> as(Function<T, R> adapter) {
			Assert.notNull(adapter, "'adapter' must not be null");
			return null;
		}

		public <E, K, V> Member<T> usingElements(BiConsumer<T, Consumer<E>> elements, Class<E> elementType,
				Function<E, K> nameExtractor, Function<E, V> valueExtractor) {
			return usingElements(elements, nameExtractor, valueExtractor);
		}

		public <E, K, V> Member<T> usingElements(BiConsumer<T, Consumer<E>> elements, Function<E, K> nameExtractor,
				Function<E, V> valueExtractor) {
			return usingPairs((instance, pairs) -> elements.accept(instance, (element) -> {
				K name = nameExtractor.apply(element);
				V value = valueExtractor.apply(element);
				pairs.accept(name, value);
			}));
		}

		public <K, V> Member<T> usingPairs(BiConsumer<T, BiConsumer<K, V>> pairs) {
			return usingWriteAction((instance, valueWriter) -> valueWriter
				.<K, V>writePairs((valueWriterPairs) -> pairs.accept(instance, valueWriterPairs)));
		}

		public Member<T> usingMembers(Consumer<Members<T>> members) {
			Members<T> delegate = new Members<>();
			members.accept(delegate);
			delegate.check();
			return usingWriteAction(delegate::write);
		}

		private Member<T> usingWriteAction(BiConsumer<T, JsonValueWriter> writeAction) {
			// FIXME check we're not already done this or similar
			this.writeAction = writeAction;
			return this;
		}

		void write(Object instance, JsonValueWriter valueWriter) {
			T extracted = extract(instance);
			if (this.writeAction != null) {
				this.writeAction.accept(extracted, valueWriter);
				return;
			}
			valueWriter.write(extracted);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		private T extract(Object instance) {
			return (T) ((Function) this.extractor).apply(instance);
		}

		void check() {

		}

	}

}
