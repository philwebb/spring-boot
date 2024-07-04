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
import java.util.Collection;
import java.util.LinkedHashMap;
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

	WritableJson write(T instance);

	static <T> JsonWriter<T> of(Consumer<Members<T>> consumer) {
		return null;
	}

	static <T> JsonWriter<T> using(BiConsumer<T, ValueWriter> consumer) {
		return (instance) -> {
			ValueWriter valueWriter = new ValueWriter(null);
			consumer.accept(instance, valueWriter);
			return null;
		};
	}

	public static class ValueWriter {

		private final Appendable out;

		ValueWriter(Appendable out) {
			this.out = out;
		}

		public <V> void write(V value) throws IOException {
			if (value == null) {
				writeNull();
			}
			else if (ObjectUtils.isArray(value) || value instanceof Collection) {
				writeArray(value);
			}
			else if (value instanceof Map<?, ?> map) {
				writeObject(map);
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

		public void temp() {
			Map<String, Object> map = new LinkedHashMap<>();
			BiConsumer<? super String, ? super Object> foo = null;
			writeObject(map::forEach);
		}

		public void y(Consumer<Consumer<String>> arg) {

		}

		public <K, V> void writeObject(Consumer<BiConsumer<K, V>> dunno) {

		}

		private void writeObject(Map<?, ?> map) throws IOException {
			append("{");
			int i = 0;
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				appendIf(i > 0, ',');
				writeString(entry.getKey());
				append(":");
				write(entry.getValue());
				i++;
			}
			append("}");
		}

		private void writeArray(Object value) throws IOException {
			append('[');
			if (ObjectUtils.isArray(value)) {
				writeElements(ObjectUtils.toObjectArray(value));
			}
			else {
				writeElements((Iterable<?>) value);
			}
			append(']');
		}

		private void writeElements(Object[] array) throws IOException {
			for (int i = 0; i < array.length; i++) {
				appendIf(i > 0, ',');
				write(array[i]);
			}
		}

		private void writeElements(Iterable<?> iterable) throws IOException {
			int i = 0;
			for (Object element : iterable) {
				appendIf(i > 0, ',');
				write(element);
				i++;
			}
		}

		private void writeString(Object value) throws IOException {
			this.out.append('"');
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
					default -> append(ch, true);
				}
			}
			this.out.append('"');
		}

		private void writeNumber(Object value) throws IOException {
			append(value.toString());
		}

		private void writeNull() throws IOException {
			append("null");
		}

		private void writeBoolean(Object value) throws IOException {
			append(Boolean.TRUE.equals(value) ? "true" : "false");
		}

		private void appendIf(boolean match, char ch) throws IOException {
			if (match) {
				append(ch);
			}
		}

		private void append(char ch, boolean escapeUnicode) throws IOException {
			if (escapeUnicode && Character.isISOControl(ch)) {
				append("\\u");
				append(String.format("%04X", (int) ch));
			}
			else {
				append(ch);
			}
		}

		private void append(char ch) throws IOException {
			this.out.append(ch);
		}

		private void append(CharSequence value) throws IOException {
			this.out.append(value);
		}

	}

	// FIXME class
	interface WritableJson {

		void to(Appendable appendable);

		/**
		 * @return
		 */
		String toStringWithNewLine();

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
