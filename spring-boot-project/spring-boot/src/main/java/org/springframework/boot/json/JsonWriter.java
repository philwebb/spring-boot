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
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.springframework.util.ObjectUtils;

/**
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @param <T> The type being written
 * @since 3.4.0
 */
@FunctionalInterface
public interface JsonWriter<T> {

	default String writeToString(T instance) {
		return writeToString(instance, null);
	}

	default String writeToString(T instance, String suffix) {
		StringBuilder out = new StringBuilder();
		write(instance, out);
		out.append(suffix != null ? suffix : "");
		return out.toString();
	}

	void write(T instance, Appendable out);

	static <T> JsonWriter<T> of(Consumer<Members<T>> consumer) {
		return null;
	}

	static <T> JsonWriter<T> using(BiConsumer<T, ValueWriter> consumer) {
		return (instance, out) -> consumer.accept(instance, new ValueWriter(out));
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
				writeObject(map::forEach);
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

		public <T, K, V> void writeObject(Consumer<Consumer<T>> elementsProvider, Function<T, K> keyExtractor,
				Function<T, V> valueExtractor) {
			writeObject((pair) -> elementsProvider.accept((element) -> {
				K key = keyExtractor.apply(element);
				V value = valueExtractor.apply(element);
				pair.accept(key, value);
			}));
		}

		public <K, V> void writeObject(Consumer<BiConsumer<K, V>> pairsProvider) {
			append("{");
			boolean[] addComma = { false };
			pairsProvider.accept((key, value) -> {
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
			try {
				this.out.append(ch);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

		private void append(CharSequence value) {
			try {
				this.out.append(value);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

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

		Member<T> whenNotNull();

		Member<T> whenHasLength();

		Member<T> when(Predicate<T> predicate);

		Member<T> whenNot(Predicate<T> predicate);

		<R> Member<R> as(Function<T, R> adapter);

		void asJson(Consumer<Members<T>> dunno);

		void asWrittenJson(JsonWriter<T> dunno);

	}

}
