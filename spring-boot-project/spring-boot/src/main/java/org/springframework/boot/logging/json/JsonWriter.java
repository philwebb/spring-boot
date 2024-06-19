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

package org.springframework.boot.logging.json;

/**
 * Writer for JSON formats.
 *
 * @author Moritz Halbritter
 * @since 3.4.0
 */
public class JsonWriter implements StructuredLoggingWriter {

	private final StringBuilder stringBuilder;

	public JsonWriter() {
		this(new StringBuilder());
	}

	public JsonWriter(StringBuilder stringBuilder) {
		this.stringBuilder = stringBuilder;
	}

	/**
	 * Writes object start.
	 * @return self for method chaining
	 */
	public JsonWriter objectStart() {
		this.stringBuilder.append("{");
		return this;
	}

	/**
	 * Writes object end.
	 * @return self for method chaining
	 */
	public JsonWriter objectEnd() {
		removeTrailingComma();
		this.stringBuilder.append("}");
		return this;
	}

	/**
	 * Writes an attribute.
	 * @param key the key of the attribute
	 * @param value the value of the attribute
	 * @return self for method chaining
	 */
	public JsonWriter attribute(String key, String value) {
		writeKey(key);
		writeString(value);
		this.stringBuilder.append(',');
		return this;
	}

	/**
	 * Writes an attribute.
	 * @param key the key of the attribute
	 * @param value the value of the attribute
	 * @return self for method chaining
	 */
	public JsonWriter attribute(String key, long value) {
		writeKey(key);
		writeLong(value);
		this.stringBuilder.append(',');
		return this;
	}

	/**
	 * Writes an attribute with multiple string values.
	 * @param key the key of the attribute
	 * @param values the values of the attribute
	 * @return self for method chaining
	 */
	public JsonWriter attribute(String key, Iterable<String> values) {
		writeKey(key);
		this.stringBuilder.append('[');
		for (String value : values) {
			writeString(value);
			this.stringBuilder.append(',');
		}
		removeTrailingComma();
		this.stringBuilder.append("],");
		return this;
	}

	@Override
	public void newLine() {
		this.stringBuilder.append('\n');
	}

	private void removeTrailingComma() {
		if (this.stringBuilder.isEmpty()) {
			return;
		}
		if (this.stringBuilder.charAt(this.stringBuilder.length() - 1) == ',') {
			this.stringBuilder.setLength(this.stringBuilder.length() - 1);
		}
	}

	private void writeLong(long value) {
		this.stringBuilder.append(value);
	}

	private void writeString(String value) {
		if (value == null) {
			this.stringBuilder.append("null");
		}
		else {
			this.stringBuilder.append('"');
			escape(value, this.stringBuilder);
			this.stringBuilder.append('"');
		}
	}

	private void writeKey(String key) {
		this.stringBuilder.append('"');
		escape(key, this.stringBuilder);
		this.stringBuilder.append("\":");
	}

	@Override
	public String finish() {
		removeTrailingComma();
		return this.stringBuilder.toString();
	}

	private static void escape(String text, StringBuilder output) {
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			escape(c, output);
		}
	}

	private static void escape(char c, StringBuilder output) {
		// TODO MH: More JSON \\u escaping, see co.elastic.logging.JsonUtils#quoteAsString
		switch (c) {
			case '"' -> output.append("\\\"");
			case '\\' -> output.append("\\\\");
			case '/' -> output.append("\\/");
			case '\b' -> output.append("\\b");
			case '\f' -> output.append("\\f");
			case '\n' -> output.append("\\n");
			case '\r' -> output.append("\\r");
			case '\t' -> output.append("\\t");
			default -> output.append(c);
		}
	}

}
