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
 * A JSON value.
 *
 * @author Moritz Halbritter
 * @since 3.4.0
 */
public interface Value {

	/**
	 * Writes the key to the given {@link StringBuilder}.
	 * @param output the string builder to write to
	 */
	void write(StringBuilder output);

	/**
	 * Creates a verbatim (unescaped) value.
	 * @param value the value
	 * @return the verbatim value
	 */
	static Value verbatim(String value) {
		return (output) -> output.append('\"').append(value).append('\"');
	}

	/**
	 * Creates an escaped value.
	 * @param value the value
	 * @return the escaped value
	 */
	static Value escaped(String value) {
		return (output) -> {
			output.append('\"');
			JsonHelper.escape(value, output);
			output.append('\"');
		};
	}

	/**
	 * Creates a value of the given {@code long}.
	 * @param value the long value
	 * @return the created value
	 */
	static Value of(long value) {
		return (output) -> output.append(value);
	}

}
