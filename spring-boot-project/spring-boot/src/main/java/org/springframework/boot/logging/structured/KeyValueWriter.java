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

package org.springframework.boot.logging.structured;

/**
 * Writer for key-value formats.
 *
 * @author Moritz Halbritter
 * @since 3.4.0
 */
public class KeyValueWriter implements StructuredLoggingWriter {

	private final StringBuilder stringBuilder;

	public KeyValueWriter() {
		this(new StringBuilder());
	}

	public KeyValueWriter(StringBuilder stringBuilder) {
		this.stringBuilder = stringBuilder;
	}

	/**
	 * Writes an attribute.
	 * @param key the key of the attribute
	 * @param value the value of the attribute
	 * @return self for method chaining
	 */
	public KeyValueWriter attribute(String key, String value) {
		boolean needQuotes = value.contains(" ");
		this.stringBuilder.append(key).append('=');
		if (needQuotes) {
			this.stringBuilder.append('"');
		}
		escape(value, this.stringBuilder);
		if (needQuotes) {
			this.stringBuilder.append('"');
		}
		this.stringBuilder.append(' ');
		return this;
	}

	private static void escape(String text, StringBuilder output) {
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			switch (c) {
				case '"' -> output.append("\\\"");
				case '\\' -> output.append("\\\\");
				case '\b' -> output.append("\\b");
				case '\f' -> output.append("\\f");
				case '\n' -> output.append("\\n");
				case '\r' -> output.append("\\r");
				case '\t' -> output.append("\\t");
				default -> output.append(c);
			}
		}
	}

	@Override
	public void newLine() {
		this.stringBuilder.append('\n');
	}

	@Override
	public String finish() {
		removeTrailingSpace();
		return this.stringBuilder.toString();
	}

	private void removeTrailingSpace() {
		if (this.stringBuilder.isEmpty()) {
			return;
		}
		if (this.stringBuilder.charAt(this.stringBuilder.length() - 1) == ' ') {
			this.stringBuilder.setLength(this.stringBuilder.length() - 1);
		}
	}

}
