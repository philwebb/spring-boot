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
 * A JSON field.
 *
 * @author Moritz Halbritter
 * @since 3.4.0
 */
public interface Field {

	/**
	 * Writes the field to the given {@link StringBuilder}.
	 * @param output the string builder to write to
	 */
	void write(StringBuilder output);

	/**
	 * Creates a new field, consisting of a {@link Key key} and a {@link Value value}.
	 * @param key the key
	 * @param value the value
	 * @return the field
	 */
	static Field of(Key key, Value value) {
		return (output) -> {
			output.append('\"');
			key.write(output);
			output.append("\":");
			value.write(output);
		};
	}

}
