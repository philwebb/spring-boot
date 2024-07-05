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

import org.springframework.boot.json.JsonValueWriter.Series;
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

		public <M extends Map<K, V>, K, V> Member<M> addMapEntries(Function<T, M> extractor) {
			return add(extractor).usingPairs(Map::forEach);
		}

		private <V> Member<V> addMember(String name, Function<T, V> extractor) {
			Member<V> member = new Member<>(name, extractor);
			this.members.add(member);
			return member;
		}

		void check() {
			Assert.state(!this.members.isEmpty(), "No members have been added");
			if (this.members.size() > 1) {
				this.members.forEach(Member::checkContributingToJsonObject);
			}
		}

		void write(T instance, JsonValueWriter valueWriter) {
			Member<?> firstMember = this.members.get(0);
			if (this.members.size() == 1 && !firstMember.isContributingToJsonObject()) {
				firstMember.write(instance, valueWriter);
				return;
			}
			valueWriter.start(Series.OBJECT);
			this.members.forEach((member) -> member.write(instance, valueWriter));
			valueWriter.end(Series.OBJECT);
		}

	}

	public static final class Member<T> {

		private final String name;

		private Function<?, T> extractor;

		private Predicate<Object> predicate = (instance) -> true;

		private BiConsumer<T, JsonValueWriter> writeAction;

		Member(String name, Function<?, T> extractor) {
			this.name = name;
			this.extractor = (extractor != null) ? extractor : Function.identity();
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

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Member<T> when(Predicate<T> predicate) {
			Assert.notNull(predicate, "'predicate' must not be null");
			this.predicate = ((Predicate) this.predicate).and(predicate);
			return this;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public <R> Member<R> as(Function<T, R> adapter) {
			Assert.notNull(adapter, "'adapter' must not be null");
			Function existing = this.extractor;
			this.extractor = existing.andThen(adapter);
			return (Member<R>) this;
		}

		public <E, N, V> Member<T> usingExtractedPairs(BiConsumer<T, Consumer<E>> elements,
				PairExtractor<E> extractor) {
			return usingExtractedPairs(elements, extractor::getName, extractor::getValue);
		}

		public <E, N, V> Member<T> usingExtractedPairs(BiConsumer<T, Consumer<E>> elements,
				Function<E, N> nameExtractor, Function<E, V> valueExtractor) {
			return usingPairs((instance, pairs) -> elements.accept(instance, (element) -> {
				N name = nameExtractor.apply(element);
				V value = valueExtractor.apply(element);
				pairs.accept(name, value);
			}));
		}

		public <N, V> Member<T> usingPairs(BiConsumer<T, BiConsumer<N, V>> pairs) {
			return useWriteAction((instance, valueWriter) -> valueWriter
				.<N, V>writePairs((valueWriterPairs) -> pairs.accept(instance, valueWriterPairs)));
		}

		public Member<T> usingMembers(Consumer<Members<T>> members) {
			Members<T> delegate = new Members<>();
			members.accept(delegate);
			delegate.check();
			return useWriteAction(delegate::write);
		}

		private Member<T> useWriteAction(BiConsumer<T, JsonValueWriter> writeAction) {
			Assert.state(this.writeAction == null, "Write action already defined");
			this.writeAction = writeAction;
			return this;
		}

		void write(Object instance, JsonValueWriter valueWriter) {
			if (!this.predicate.test(instance)) {
				return;
			}
			T extracted = extract(instance);
			if (this.writeAction != null) {
				this.writeAction.accept(extracted, valueWriter);
				return;
			}
			if (this.name != null) {
				valueWriter.writePair(this.name, extracted);
				return;
			}
			valueWriter.write(extracted);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		private T extract(Object instance) {
			return (T) ((Function) this.extractor).apply(instance);
		}

		void checkContributingToJsonObject() {
			Assert.state(isContributingToJsonObject(),
					"Incomplete member definition, ensure that an appropriate 'use' method has been called");
		}

		boolean isContributingToJsonObject() {
			return this.name != null || this.writeAction != null;
		}

	}

	interface PairExtractor<T> {

		<N> N getName(T instance);

		<V> V getValue(T instance);

		static <T> PairExtractor<T> of(Function<T, ?> nameExtractor, Function<T, ?> valueExtractor) {
			Assert.notNull(nameExtractor, "'nameExtractor' must not be null");
			Assert.notNull(valueExtractor, "'valueExtractor' must not be null");
			return new PairExtractor<>() {

				@Override
				@SuppressWarnings("unchecked")
				public <N> N getName(T instance) {
					return (N) nameExtractor.apply(instance);
				}

				@Override
				@SuppressWarnings("unchecked")
				public <V> V getValue(T instance) {
					return (V) valueExtractor.apply(instance);
				}

			};
		}

	}

}
