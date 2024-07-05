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
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.assertj.core.util.Arrays;

import org.springframework.boot.json.JsonWriter.WritableJson;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

class JsonValueWriter {

	private final Appendable out;

	private final Queue<ActiveSeries> activeSeries = new ArrayDeque<>();

	JsonValueWriter(Appendable out) {
		this.out = out;
	}

	void start(Series series) {
		this.activeSeries.add(new ActiveSeries(series));
		append(series.getOpenChar());
	}

	void end(Series series) {
		this.activeSeries.remove(getActiveSeries(series));
		append(series.closeChar);
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
			writeObject(map::forEach);
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

	private void writeArray(Object value) {
		if (value instanceof Iterable<?> iterable) {
			writeArray(iterable::forEach);
			return;
		}
		if (ObjectUtils.isArray(value)) {
			writeArray(Arrays.asList(ObjectUtils.toObjectArray(value))::forEach);
			return;
		}
		throw new IllegalStateException("Unknow array type");
	}

	<K, V> void writeObject(Consumer<BiConsumer<K, V>> pairs) {
		start(Series.OBJECT);
		pairs.accept(this::writePair);
		end(Series.OBJECT);
	}

	<K, V> void writePairs(Consumer<BiConsumer<K, V>> pairs) {
		pairs.accept(this::writePair);
	}

	private <V, K> void writePair(K key, V value) {
		getActiveSeries(Series.OBJECT).write(() -> {
			writeString(key);
			append(":");
			write(value);
		});
	}

	<E> void writeArray(Consumer<Consumer<E>> elements) {
		start(Series.ARRAY);
		elements.accept(this::writeElement);
		end(Series.ARRAY);
	}

	<E> void writeElements(Consumer<Consumer<E>> elements) {
		elements.accept(this::writeElement);
	}

	private <E> void writeElement(E element) {
		getActiveSeries(Series.ARRAY).write(() -> write(element));
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

	private ActiveSeries getActiveSeries(Series series) {
		ActiveSeries activeSeries = this.activeSeries.peek();
		Assert.state(activeSeries != null, "No series has been started");
		Assert.state(activeSeries.is(series), () -> "Existing series is not " + series);
		return activeSeries;
	}

	enum Series {

		OBJECT('{', '}'), ARRAY('[', ']');

		private final char openChar;

		private final char closeChar;

		Series(char openChar, char closeChar) {
			this.openChar = openChar;
			this.closeChar = closeChar;
		}

		char getOpenChar() {
			return this.openChar;
		}

		char getCloseChar() {
			return this.closeChar;
		}

	}

	private class ActiveSeries {

		private final Series series;

		private boolean commaRequired;

		ActiveSeries(Series series) {
			this.series = series;
		}

		void write(Runnable action) {
			appendIf(this.commaRequired, ',');
			this.commaRequired = true;
			action.run();
		}

		boolean is(Series series) {
			return this.series == series;
		}

	}

}