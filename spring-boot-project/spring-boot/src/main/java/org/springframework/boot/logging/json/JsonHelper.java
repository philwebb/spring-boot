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

final class JsonHelper {

	private JsonHelper() {
	}

	static void escape(String text, StringBuilder output) {
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			escape(c, output);
		}
	}

	static void escape(char c, StringBuilder output) {
		// TODO: More JSON \\u escaping, see
		// co.elastic.logging.JsonUtils#quoteAsString(java.lang.CharSequence, int,
		// int,
		// java.lang.StringBuilder)
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
