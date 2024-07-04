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
import java.util.Collection;
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

	void write(T instance, Appendable out);

	default JsonWriter<T> endingWithNewLine() {
		return endingWith("\n");
	}

	default JsonWriter<T> endingWith(String suffix) {
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

	static <T> JsonWriter<T> using(BiConsumer<T, ValueWriter> writer) {
		return (instance, out) -> writer.accept(instance, new ValueWriter(out));
	}

	private static <T, R> R accept(Consumer<T> consumer, T value, Function<T, R> finalizer) {
		consumer.accept(value);
		return finalizer.apply(value);
	}

	public static class ValueWriter {

		private final Appendable out;

		ValueWriter(Appendable out) {
			this.out = out;
		}

		public <V> void write(V value) {
			if (value == null) {
				writeNull();
			}
			else if (ObjectUtils.isArray(value) || value instanceof Collection) {
				writeArray(value);
			}
			else if (value instanceof Map<?, ?> map) {
				writePairs(map::forEach);
			}
			else if (value instanceof Number) {
				writeNumber(value);
			}
			else if (value instanceof Boolean) {
				writeBoolean(value);
			}
			else {
				writeString(value);
			}
		}

		public <T, K, V> void writeEntries(Consumer<Consumer<T>> elements, Function<T, K> keyExtractor,
				Function<T, V> valueExtractor) {
			writePairs((pair) -> elements.accept((element) -> {
				K key = keyExtractor.apply(element);
				V value = valueExtractor.apply(element);
				pair.accept(key, value);
			}));
		}

		public <K, V> void writePairs(Consumer<BiConsumer<K, V>> pairs) {
			append("{");
			boolean[] addComma = { false };
			pairs.accept((key, value) -> {
				appendIf(addComma[0], ',');
				writeString(key);
				append(":");
				write(value);
				addComma[0] = true;
			});
			append("}");
		}

		private void writeArray(Object value) {
			append('[');
			if (ObjectUtils.isArray(value)) {
				writeElements(ObjectUtils.toObjectArray(value));
			}
			else {
				writeElements((Iterable<?>) value);
			}
			append(']');
		}

		private void writeElements(Object[] array) {
			for (int i = 0; i < array.length; i++) {
				appendIf(i > 0, ',');
				write(array[i]);
			}
		}

		private void writeElements(Iterable<?> iterable) {
			boolean addComma = false;
			for (Object element : iterable) {
				appendIf(addComma, ',');
				write(element);
				addComma = true;
			}
		}

		private void writeString(Object value) {
			append('"');
			String string = value.toString();
			for (int i = 0; i < string.length(); i++) {
				char ch = string.charAt(i);
				switch (ch) {
					case '"' -> append("\\\"");
					case '\\' -> append("\\\\");
					case '/' -> append("\\/");
					case '\b' -> append("\\b");
					case '\f' -> append("\\f");
					case '\n' -> append("\\n");
					case '\r' -> append("\\r");
					case '\t' -> append("\\t");
					default -> appendWithIsoControlEscape(ch);
				}
			}
			append('"');
		}

		private void writeNumber(Object value) {
			append(value.toString());
		}

		private void writeNull() {
			append("null");
		}

		private void writeBoolean(Object value) {
			append(Boolean.TRUE.equals(value) ? "true" : "false");
		}

		private void appendIf(boolean condition, char ch) {
			if (condition) {
				append(ch);
			}
		}

		private void appendWithIsoControlEscape(char ch) {
			if (Character.isISOControl(ch)) {
				append("\\u");
				append(String.format("%04X", (int) ch));
			}
			else {
				append(ch);
			}
		}

		private void append(char ch) {
			JsonWriter.append(this.out, ch);
		}

		private void append(CharSequence value) {
			JsonWriter.append(this.out, value);
		}

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
			return addMember((instance, pairs) -> pairs.accept(key, extractor.apply((T) instance)));
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
			return new Member<>(null);
		}

		public <M extends Map<K, V>, K, V> Member<M> addMapEntries(Function<T, M> extractor) {
			return add(extractor).usingPairs(Map::forEach);
		}

		private <V> Member<V> addMember(JsonMemberWriter writer) {
			Member<V> member = null;
			this.members.add(member);
			return member;
		}

		JsonWriter<T> writer() {
			JsonMemberWriters memberWriters = new JsonMemberWriters(this.members.stream().map(Member::writer));
			return JsonWriter.using(memberWriters::write);
		}

	}

	public static final class Member<T> {

		private JsonMemberWriter writer;

		Member(JsonMemberWriter writer) {
			this.writer = writer;
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
			return this;
		}

		public <R> Member<R> as(Function<T, R> adapter) {
			return null;
		}

		public <E, K, V> Member<T> usingElements(BiConsumer<T, Consumer<E>> elements, Class<E> elementType,
				Function<E, K> keyExtractor, Function<E, V> valueExtractor) {
			return usingElements(elements, keyExtractor, valueExtractor);
		}

		public <E, K, V> Member<T> usingElements(BiConsumer<T, Consumer<E>> elements, Function<E, K> keyExtractor,
				Function<E, V> valueExtractor) {
			return null;
		}

		public <K, V> Member<T> usingPairs(BiConsumer<T, BiConsumer<K, V>> pairs) {
			return null;
		}

		public Member<T> usingMembers(Consumer<Members<T>> members) {
			return null;
		}

		JsonMemberWriter writer() {
			Assert.state(this.writer != null,
					"Unable to write member JSON. Please add the member with a 'key' or complete "
							+ "the definition with the 'using(...) method");
			return this.writer;
		}

	}

}
