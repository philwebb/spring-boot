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

/**
 * Writes JSON.
 *
 * @author Moritz Halbritter
 * @since 3.4.0
 */
public class JsonWriter {

	private final StringBuilder stringBuilder;

	/**
	 * Creates a new instance.
	 */
	public JsonWriter() {
		this(new StringBuilder());
	}

	/**
	 * Creates a new instance, using the given {@link StringBuilder}.
	 * @param stringBuilder the string builder to use
	 */
	public JsonWriter(StringBuilder stringBuilder) {
		this.stringBuilder = stringBuilder;
	}

	/**
	 * Writes a new object.
	 * @param nested the {@link Runnable} to populate the object
	 * @return this for method chaining
	 */
	public JsonWriter object(Runnable nested) {
		this.stringBuilder.append('{');
		nested.run();
		removeTrailingComma();
		this.stringBuilder.append("},");
		return this;
	}

	/**
	 * Writes an empty object.
	 * @return this for method chaining
	 */
	public JsonWriter object() {
		return object(() -> {
		});
	}

	/**
	 * Writes an empty array.
	 * @return this for method chaining
	 */
	public JsonWriter array() {
		return array(() -> {
		});
	}

	/**
	 * Writes a new array.
	 * @param nested the {@link Runnable} to populate the array
	 * @return this for method chaining
	 */
	public JsonWriter array(Runnable nested) {
		this.stringBuilder.append('[');
		nested.run();
		removeTrailingComma();
		this.stringBuilder.append("],");
		return this;
	}

	/**
	 * Writes a new string array.
	 * @param values the strings to write
	 * @return this for method chaining
	 */
	public JsonWriter stringArray(String... values) {
		array(() -> {
			for (String value : values) {
				string(value);
			}
		});
		return this;
	}

	/**
	 * Writes a new string array.
	 * @param values the strings to write
	 * @return this for method chaining
	 */
	public JsonWriter stringArray(Iterable<String> values) {
		array(() -> {
			for (String value : values) {
				string(value);
			}
		});
		return this;
	}

	/**
	 * Writes a new number array.
	 * @param values the numbers to write
	 * @return this for method chaining
	 */
	public JsonWriter numberArray(double... values) {
		array(() -> {
			for (double value : values) {
				number(value);
			}
		});
		return this;
	}

	/**
	 * Writes a new number array.
	 * @param values the numbers to write
	 * @return this for method chaining
	 */
	public JsonWriter numberArray(long... values) {
		array(() -> {
			for (long value : values) {
				number(value);
			}
		});
		return this;
	}

	/**
	 * Writes a new boolean array.
	 * @param values the booleans to write
	 * @return this for method chaining
	 */
	public JsonWriter boolArray(boolean... values) {
		array(() -> {
			for (boolean value : values) {
				bool(value);
			}
		});
		return this;
	}

	/**
	 * Writes a string.
	 * @param value the string to write
	 * @return this for method chaining
	 */
	public JsonWriter string(String value) {
		if (value == null) {
			this.stringBuilder.append("null,");
		}
		else {
			this.stringBuilder.append('"');
			appendWithEscaping(value);
			this.stringBuilder.append('"').append(',');
		}
		return this;
	}

	private void appendWithEscaping(String value) {
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '"' -> this.stringBuilder.append("\\\"");
				case '\\' -> this.stringBuilder.append("\\\\");
				case '/' -> this.stringBuilder.append("\\/");
				case '\b' -> this.stringBuilder.append("\\b");
				case '\f' -> this.stringBuilder.append("\\f");
				case '\n' -> this.stringBuilder.append("\\n");
				case '\r' -> this.stringBuilder.append("\\r");
				case '\t' -> this.stringBuilder.append("\\t");
				default -> {
					if (Character.isISOControl(c)) {
						this.stringBuilder.append("\\u");
						this.stringBuilder.append(String.format("%04X", (int) c));
					}
					else {
						this.stringBuilder.append(c);
					}
				}
			}
		}
	}

	/**
	 * Writes a number.
	 * @param value the number to write
	 * @return this for method chaining
	 */
	public JsonWriter number(double value) {
		this.stringBuilder.append(value).append(',');
		return this;
	}

	/**
	 * Writes a number.
	 * @param value the number to write
	 * @return this for method chaining
	 */
	public JsonWriter number(long value) {
		this.stringBuilder.append(value).append(',');
		return this;
	}

	/**
	 * Writes a boolean.
	 * @param value the boolean to write
	 * @return this for method chaining
	 */
	public JsonWriter bool(boolean value) {
		this.stringBuilder.append(value).append(',');
		return this;
	}

	/**
	 * Writes a string object member.
	 * @param key the name of the key
	 * @param value the string value
	 * @return this for method chaining
	 */
	public JsonWriter stringMember(String key, String value) {
		return member(key, () -> string(value));
	}

	/**
	 * Writes a number object member.
	 * @param key the name of the key
	 * @param value the number value
	 * @return this for method chaining
	 */
	public JsonWriter numberMember(String key, double value) {
		return member(key, () -> number(value));
	}

	/**
	 * Writes a number object member.
	 * @param key the name of the key
	 * @param value the number value
	 * @return this for method chaining
	 */
	public JsonWriter numberMember(String key, long value) {
		return member(key, () -> number(value));
	}

	/**
	 * Writes a boolean object member.
	 * @param key the name of the key
	 * @param value the boolean value
	 * @return this for method chaining
	 */
	public JsonWriter boolMember(String key, boolean value) {
		return member(key, () -> bool(value));
	}

	/**
	 * Writes an object member.
	 * @param key the name of the key
	 * @param nested the {@link Runnable} to populate the object
	 * @return this for method chaining
	 */
	public JsonWriter member(String key, Runnable nested) {
		this.stringBuilder.append('"').append(key).append('"').append(':');
		if (nested == null) {
			this.stringBuilder.append("null,");
		}
		else {
			nested.run();
		}
		return this;
	}

	/**
	 * Writes a new line.
	 * @return this for method chaining
	 */
	public JsonWriter newLine() {
		removeTrailingComma();
		this.stringBuilder.append('\n');
		return this;
	}

	/**
	 * Generates a JSON string.
	 * @return the JSON string
	 */
	public String toJson() {
		removeTrailingComma();
		return this.stringBuilder.toString();
	}

	private void removeTrailingComma() {
		if (this.stringBuilder.isEmpty()) {
			return;
		}
		char lastChar = this.stringBuilder.charAt(this.stringBuilder.length() - 1);
		if (lastChar == ',') {
			this.stringBuilder.setLength(this.stringBuilder.length() - 1);
		}
	}

}
