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
import org.springframework.boot.json.JsonWriter.Member.Extractor;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.function.ThrowingConsumer;

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
		return WritableJson.toString(write(instance));
	}

	default WritableJson write(T instance) {
		return WritableJson.of((out) -> write(instance, out));
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
		Members<T> initiaizedMembers = new Members<>(members, false);
		return (instance, out) -> initiaizedMembers.write(instance, new JsonValueWriter(out));
	}

	static <T> JsonWriter<T> ofFormatString(String json) {
		return (instance, out) -> out.append(json.formatted(instance));
	}

	interface WritableJson {

		void to(Appendable out);

		static WritableJson of(ThrowingConsumer<Appendable> writeAction) {
			return new WritableJson() {

				@Override
				public void to(Appendable out) {
					writeAction.accept(out);
				}

				@Override
				public String toString() {
					return WritableJson.toString(this);
				}

			};
		}

		static String toString(WritableJson writableJson) {
			StringBuilder stringBuilder = new StringBuilder();
			writableJson.to(stringBuilder);
			return stringBuilder.toString();
		}

	}

	public static final class Members<T> {

		private final List<Member<?>> members = new ArrayList<>();

		private final boolean contributesPair;

		private final Series series;

		Members(Consumer<Members<T>> initializer, boolean contributesToExistingSeries) {
			initializer.accept(this);
			Assert.state(!this.members.isEmpty(), "No members have been added");
			this.contributesPair = this.members.stream().anyMatch(Member::contributesPair);
			this.series = (this.contributesPair && !contributesToExistingSeries) ? Series.OBJECT : null;
			if (this.contributesPair || this.members.size() > 1) {
				this.members.forEach((member) -> Assert.state(member.contributesPair(),
						() -> String.format("%s does not contribute a named pair, ensure that all members have "
								+ "a name or call an appropriate 'using' method", member)));
			}
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
			Member<V> member = new Member<>(this.members.size(), name, Extractor.of(extractor));
			this.members.add(member);
			return member;
		}

		void write(T instance, JsonValueWriter valueWriter) {
			valueWriter.start(this.series);
			this.members.forEach((member) -> member.write(instance, valueWriter));
			valueWriter.end(this.series);
		}

		boolean contributesPair() {
			return this.contributesPair;
		}

	}

	public static final class Member<T> {

		private final int index;

		private final String name;

		private Extractor<T> extractor;

		private BiConsumer<T, BiConsumer<?, ?>> pairs;

		private Members<T> members;

		Member(int index, String name, Extractor<T> extractor) {
			this.index = index;
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
			this.extractor = this.extractor.when(predicate);
			return this;
		}

		@SuppressWarnings("unchecked")
		public <R> Member<R> as(Function<T, R> adapter) {
			Assert.notNull(adapter, "'adapter' must not be null");
			Member<R> result = (Member<R>) this;
			result.extractor = this.extractor.as(adapter);
			return result;
		}

		public <E, N, V> Member<T> usingExtractedPairs(BiConsumer<T, Consumer<E>> elements,
				PairExtractor<E> extractor) {
			Assert.notNull(elements, "'elements' must not be null");
			Assert.notNull(extractor, "'extractor' must not be null");
			return usingExtractedPairs(elements, extractor::getName, extractor::getValue);
		}

		public <E, N, V> Member<T> usingExtractedPairs(BiConsumer<T, Consumer<E>> elements,
				Function<E, N> nameExtractor, Function<E, V> valueExtractor) {
			Assert.notNull(elements, "'elements' must not be null");
			Assert.notNull(nameExtractor, "'nameExtractor' must not be null");
			Assert.notNull(valueExtractor, "'valueExtractor' must not be null");
			return usingPairs((instance, pairs) -> elements.accept(instance, (element) -> {
				N name = nameExtractor.apply(element);
				V value = valueExtractor.apply(element);
				pairs.accept(name, value);
			}));
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public <N, V> Member<T> usingPairs(BiConsumer<T, BiConsumer<N, V>> pairs) {
			Assert.notNull(pairs, "'pairs' must not be null");
			Assert.state(this.pairs == null, "Pairs cannot be declared multiple times");
			Assert.state(this.members == null, "Pairs cannot be declared when using members");
			this.pairs = (BiConsumer) pairs;
			return this;
		}

		public Member<T> usingMembers(Consumer<Members<T>> members) {
			Assert.notNull(members, "'members' must not be null");
			Assert.state(this.members == null, "Members cannot be declared multiple times");
			Assert.state(this.pairs == null, "Members cannot be declared when using pairs");
			this.members = new Members<>(members, this.name == null);
			return this;
		}

		boolean contributesPair() {
			return this.name != null || this.pairs != null || (this.members != null && this.members.contributesPair());
		}

		void write(Object instance, JsonValueWriter valueWriter) {
			T extracted = this.extractor.extract(instance);
			if (Extractor.skip(extracted)) {
				return;
			}
			Object value = getValueToWrite(extracted, valueWriter);
			valueWriter.write(this.name, value);
		}

		private Object getValueToWrite(T extracted, JsonValueWriter valueWriter) {
			if (this.pairs != null) {
				return WritableJson.of((out) -> valueWriter.writePairs((pairs) -> this.pairs.accept(extracted, pairs)));
			}
			if (this.members != null) {
				return WritableJson.of((out) -> this.members.write(extracted, valueWriter));
			}
			return extracted;
		}

		@Override
		public String toString() {
			return "Member #" + this.index + (this.name != null ? "{%s}".formatted(this.name) : "");
		}

		@FunctionalInterface
		interface Extractor<T> {

			Object SKIP = new Object();

			T extract(Object instance);

			default Extractor<T> when(Predicate<T> predicate) {
				return (instance) -> test(extract(instance), predicate);
			}

			@SuppressWarnings("unchecked")
			private T test(T extracted, Predicate<T> predicate) {
				return !skip(extracted) && predicate.test(extracted) ? extracted : (T) SKIP;
			}

			default <R> Extractor<R> as(Function<T, R> adapter) {
				return (instance) -> apply(extract(instance), adapter);
			}

			@SuppressWarnings("unchecked")
			private <R> R apply(T extracted, Function<T, R> function) {
				return !skip(extracted) ? function.apply(extracted) : (R) SKIP;
			}

			@SuppressWarnings("unchecked")
			static <S, T> Extractor<T> of(Function<S, T> extractor) {
				return (instance) -> !skip(instance) ? extractor.apply((S) instance) : (T) SKIP;
			}

			static <T> boolean skip(T extracted) {
				return extracted == SKIP;
			}

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
