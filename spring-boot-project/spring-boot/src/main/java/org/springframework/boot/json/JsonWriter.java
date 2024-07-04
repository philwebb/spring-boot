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

		public <V> void write(String key, V value) {
		}

		public <V> void write(V value) throws IOException {
			ValueType valueType = ValueType.deduce(value);
			switch (valueType) {
				case OBJECT -> writeObject(value);
				case ARRAY -> writeArray(value);
				case STRING -> writeString(value);
				case NUMBER -> writeNumber(value);
				case BOOLEAN -> writeBoolean(value);
				case NULL -> writeNull();
			}
		}

		private void writeObject(Object value) {
		}

		private void writeArray(Object value) throws IOException {
			append('[');
			writeArrayElements(value);
			append(']');
		}

		private void writeArrayElements(Object value) throws IOException {
			Object[] array = ObjectUtils.toObjectArray(value);
			for (int i = 0; i < array.length; i++) {
				if (i > 0) {
					append(',');
				}
				write(array[i]);
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

		enum ValueType {

			OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL;

			static ValueType deduce(Object value) {
				if (value == null) {
					return NULL;
				}
				if (ObjectUtils.isArray(value) || value instanceof Collection) {
					return ARRAY;
				}
				if (value instanceof Number) {
					return NUMBER;
				}
				if (value instanceof Boolean) {
					return BOOLEAN;
				}
				return STRING;
			}

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
