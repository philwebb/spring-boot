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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonWriter}.
 *
 * @author Moritz Halbritter
 */
class JsonWriterTests {

	@Test
	void object() {
		JsonWriter writer = new JsonWriter();
		writer.object(() -> writer.numberMember("a", 1)
			.numberMember("b", 2.0)
			.boolMember("c", true)
			.stringMember("d", "d")
			.member("e", null));
		String json = writer.toJson();
		assertThat(json).isEqualTo("""
				{"a":1,"b":2.0,"c":true,"d":"d","e":null}
				""".trim());
	}

	@Test
	void nestedObject() {
		JsonWriter writer = new JsonWriter();
		writer.object(() -> writer.numberMember("a", 1).object(() -> writer.numberMember("b", 2)));
		String json = writer.toJson();
		assertThat(json).isEqualTo("""
				{"a":1,{"b":2}}
				""".trim());
	}

	@Test
	void array() {
		JsonWriter writer = new JsonWriter();
		writer.array(() -> writer.string("a").string("b").string("c"));
		String json = writer.toJson();
		assertThat(json).isEqualTo("""
				["a","b","c"]
				""".trim());
	}

	@Test
	void arrayWithObjects() {
		JsonWriter writer = new JsonWriter();
		writer.array(
				() -> writer.object(() -> writer.stringMember("a", "1")).object(() -> writer.stringMember("b", "2")));
		String json = writer.toJson();
		assertThat(json).isEqualTo("""
				[{"a":"1"},{"b":"2"}]
				""".trim());
	}

	@Test
	void nullArray() {
		JsonWriter writer = new JsonWriter();
		writer.array(() -> writer.string(null).string(null));
		String json = writer.toJson();
		assertThat(json).isEqualTo("""
				[null,null]
				""".trim());
	}

	@Test
	void escapeString() {
		JsonWriter writer = new JsonWriter();
		writer.string("\"\\/\b\f\n\r\t\u0000\u001F");
		String json = writer.toJson();
		assertThat(json).isEqualTo("""
				"\\"\\\\\\/\\b\\f\\n\\r\\t\\u0000\\u001F"
				""".trim());
	}

}
