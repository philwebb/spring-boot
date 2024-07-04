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

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link XJsonWriter}.
 *
 * @author Moritz Halbritter
 */
class XXJsonWriterTests {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Test
	void object() {
		XJsonWriter writer = new XJsonWriter();
		writer.object(() -> writer.numberMember("a", 1)
			.numberMember("b", 2.0)
			.boolMember("c", true)
			.stringMember("d", "d")
			.member("e", null));
		assertThatJson(writer).isEqualTo("""
				{"a":1,"b":2.0,"c":true,"d":"d","e":null}
				""".trim());
	}

	@Test
	void nestedObject() {
		XJsonWriter writer = new XJsonWriter();
		writer.object(
				() -> writer.numberMember("a", 1).member("b", () -> writer.object(() -> writer.numberMember("c", 2))));
		assertThatJson(writer).isEqualTo("""
				{"a":1,"b":{"c":2}}
				""".trim());
	}

	@Test
	void array() {
		XJsonWriter writer = new XJsonWriter();
		writer.array(() -> writer.string("a").string("b").string("c"));
		assertThatJson(writer).isEqualTo("""
				["a","b","c"]
				""".trim());
	}

	@Test
	void stringArray() {
		XJsonWriter writer = new XJsonWriter();
		writer.stringArray("a", "b", "c");
		assertThatJson(writer).isEqualTo("""
				["a","b","c"]
				""".trim());
	}

	@Test
	void stringArrayFromIterable() {
		XJsonWriter writer = new XJsonWriter();
		writer.stringArray(List.of("a", "b", "c"));
		assertThatJson(writer).isEqualTo("""
				["a","b","c"]
				""".trim());
	}

	@Test
	void doubleArray() {
		XJsonWriter writer = new XJsonWriter();
		writer.numberArray(1.0, 2.0, 3.0);
		assertThatJson(writer).isEqualTo("""
				[1.0,2.0,3.0]
				""".trim());
	}

	@Test
	void longArray() {
		XJsonWriter writer = new XJsonWriter();
		writer.numberArray(1, 2, 3);
		assertThatJson(writer).isEqualTo("""
				[1,2,3]
				""".trim());
	}

	@Test
	void booleanArray() {
		XJsonWriter writer = new XJsonWriter();
		writer.boolArray(true, false, true);
		assertThatJson(writer).isEqualTo("""
				[true,false,true]
				""".trim());
	}

	@Test
	void arrayWithObjects() {
		XJsonWriter writer = new XJsonWriter();
		writer.array(
				() -> writer.object(() -> writer.stringMember("a", "1")).object(() -> writer.stringMember("b", "2")));
		assertThatJson(writer).isEqualTo("""
				[{"a":"1"},{"b":"2"}]
				""".trim());
	}

	@Test
	void nullArray() {
		XJsonWriter writer = new XJsonWriter();
		writer.array(() -> writer.string(null).string(null));
		assertThatJson(writer).isEqualTo("""
				[null,null]
				""".trim());
	}

	@Test
	void escapeString() { // DONE
		XJsonWriter writer = new XJsonWriter();
		writer.string("\"\\/\b\f\n\r\t\u0000\u001F");
		assertThatJson(writer).isEqualTo("""
				"\\"\\\\\\/\\b\\f\\n\\r\\t\\u0000\\u001F"
				""".trim());
	}

	@Test
	void newLine() {
		XJsonWriter writer = new XJsonWriter();
		writer.newLine();
		assertThatJson(writer).isEqualTo("\n");
	}

	@Test
	void newLineWithContent() {
		XJsonWriter writer = new XJsonWriter();
		writer.object();
		writer.newLine();
		assertThatJson(writer).isEqualTo("{}\n");
	}

	private static AbstractStringAssert<?> assertThatJson(XJsonWriter writer) {
		String json = writer.toJson();
		try {
			OBJECT_MAPPER.readTree(json);
		}
		catch (JsonProcessingException ex) {
			Assertions.fail("Invalid JSON produced: '%s'".formatted(json), ex);
		}
		return assertThat(json);
	}

}
