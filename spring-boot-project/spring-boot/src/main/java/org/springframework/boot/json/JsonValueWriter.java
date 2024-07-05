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

import org.springframework.boot.json.JsonWriter.WritableJson;
import org.springframework.util.ObjectUtils;

class JsonValueWriter {

	private final Appendable out;

	JsonValueWriter(Appendable out) {
		this.out = out;
	}

	<V> void write(V value) {
		if (value == null) {
			append("null");
		}
		else if (value instanceof WritableJson<?> writableJson) {
			writableJson.to(this.out);
		}
		else if (ObjectUtils.isArray(value) || value instanceof Collection) {
			writeArray(value);
		}
		else if (value instanceof Map<?, ?> map) {
			writePairs(map::forEach);
		}
		else if (value instanceof Number) {
			append(value.toString());
		}
		else if (value instanceof Boolean) {
			append(Boolean.TRUE.equals(value) ? "true" : "false");
		}
		else {
			writeString(value);
		}
	}

	// FIXME we might not need this
	<T, K, V> void writeEntries(Consumer<Consumer<T>> elements, Function<T, K> keyExtractor,
			Function<T, V> valueExtractor) {
		writePairs((pair) -> elements.accept((element) -> {
			K key = keyExtractor.apply(element);
			V value = valueExtractor.apply(element);
			pair.accept(key, value);
		}));
	}

	<K, V> void writePairs(Consumer<BiConsumer<K, V>> pairs) {
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