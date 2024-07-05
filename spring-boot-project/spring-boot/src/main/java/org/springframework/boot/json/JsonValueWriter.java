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
import java.util.Map;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.assertj.core.util.Arrays;

import org.springframework.boot.json.JsonWriter.WritableJson;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Internal class used by {@link JsonWriter} to handle the lower-level concerns of writing
 * JSON.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class JsonValueWriter {

	private final Appendable out;

	private final Queue<ActiveSeries> activeSeries = new ArrayDeque<>();

	/**
	 * Create a new {@link JsonValueWriter} instance.
	 * @param out the {@link Appendable} used to receive the JSON output
	 */
	JsonValueWriter(Appendable out) {
		this.out = out;
	}

	/**
	 * Write a value to the JSON output. The following value types are supported:
	 * <ul>
	 * <li>Any {@code null} value</li>
	 * <li>A {@link WritableJson} instance</li>
	 * <li>Any {@link Iterable} or Array (written as a JSON array)</li>
	 * <li>A {@link Map} (written as a JSON object)</li>
	 * <li>Any {@link Number}</li>
	 * <li>A {@link Boolean}</li>
	 * </ul>
	 * All other values are written as JSON strings.
	 * @param <V> the value type
	 * @param value the value to write
	 */
	<V> void write(V value) {
		if (value == null) {
			append("null");
		}
		else if (value instanceof WritableJson<?> writableJson) {
			writableJson.to(this.out);
		}
		else if (value instanceof Iterable<?> iterable) {
			writeArray(iterable::forEach);
		}
		else if (ObjectUtils.isArray(value)) {
			writeArray(Arrays.asList(ObjectUtils.toObjectArray(value))::forEach);
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

	/**
	 * Start a new {@link Series} (JSON object or array).
	 * @param series the series to start
	 * @see #end(Series)
	 * @see #writePairs(Consumer)
	 * @see #writeElements(Consumer)
	 */
	void start(Series series) {
		this.activeSeries.add(new ActiveSeries(series));
		append(series.openChar);
	}

	/**
	 * End an active {@link Series} (JSON object or array).
	 * @param series the series type being ended (must match {@link #start(Series)})
	 * @see #start(Series)
	 */
	void end(Series series) {
		this.activeSeries.remove(getActiveSeries(series));
		append(series.closeChar);
	}

	/**
	 * Write the specified elements to a newly started {@link Series#ARRAY array series}.
	 * @param <E> the element type
	 * @param elements a callback that will be used to provide each element. Typically a
	 * {@code forEach} method reference.
	 * @see #writeElements(Consumer)
	 */
	<E> void writeArray(Consumer<Consumer<E>> elements) {
		start(Series.ARRAY);
		elements.accept(this::writeElement);
		end(Series.ARRAY);
	}

	/**
	 * Write the specified elements to an already started {@link Series#ARRAY array
	 * series}
	 * @param <E> the element type
	 * @param elements a callback that will be used to provide each element. Typically a
	 * {@code forEach} method reference.
	 * @see #writeElements(Consumer)
	 */
	<E> void writeElements(Consumer<Consumer<E>> elements) {
		elements.accept(this::writeElement);
	}

	<E> void writeElement(E element) {
		getActiveSeries(Series.ARRAY).write(() -> write(element));
	}

	/**
	 * Write the specified pairs to a newly started {@link Series#OBJECT object series}.
	 * @param <N> the name type in the pair
	 * @param <V> the value type in the pair
	 * @param pairs a callback that will be used to provide each pair. Typically a
	 * {@code forEach} method reference.
	 * @see #writePairs(Consumer)
	 */
	<N, V> void writeObject(Consumer<BiConsumer<N, V>> pairs) {
		start(Series.OBJECT);
		pairs.accept(this::writePair);
		end(Series.OBJECT);
	}

	/**
	 * Write the specified pairs to an already started {@link Series#OBJECT object
	 * series}.
	 * @param <N> the name type in the pair
	 * @param <V> the value type in the pair
	 * @param pairs a callback that will be used to provide each pair. Typically a
	 * {@code forEach} method reference.
	 * @see #writePairs(Consumer)
	 */
	<N, V> void writePairs(Consumer<BiConsumer<N, V>> pairs) {
		pairs.accept(this::writePair);
	}

	<N, V> void writePair(N name, V value) {
		getActiveSeries(Series.OBJECT).write(() -> {
			writeString(name);
			append(":");
			write(value);
		});
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
		Assert.state(activeSeries.is(series), () -> "Existing series is not " + series.name());
		return activeSeries;
	}

	/**
	 * A series of items that can be written to the JSON output.
	 */
	enum Series {

		/**
		 * A JSON object series consisting of name/value pairs.
		 */
		OBJECT('{', '}'),

		/**
		 * A JSON array series consisting of elements.
		 */
		ARRAY('[', ']');

		final char openChar;

		final char closeChar;

		Series(char openChar, char closeChar) {
			this.openChar = openChar;
			this.closeChar = closeChar;
		}

	}

	/**
	 * Details of the currently active {@link Series}.
	 */
	private class ActiveSeries {

		private final Series series;

		private boolean commaRequired;

		ActiveSeries(Series series) {
			this.series = series;
		}

		void write(Runnable action) {
			if (this.commaRequired) {
				append(',');
			}
			this.commaRequired = true;
			action.run();
		}

		boolean is(Series series) {
			return this.series == series;
		}

	}

}