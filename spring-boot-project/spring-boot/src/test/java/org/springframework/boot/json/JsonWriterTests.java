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

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.json.JsonWriter.WritableJson;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonWriter}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
public class JsonWriterTests {

	@Test
	void writeToStringWritesToString() {
		assertThat(JsonWriter.ofFormatString("%s").writeToString(123)).isEqualTo("123");
	}

	@Test
	void writeReturnsWritableJson() {
		assertThat(JsonWriter.ofFormatString("%s").write(123)).isInstanceOf(WritableJson.class);
	}

	@Test
	void withSuffixAddsSuffixToWrittenString() {
		assertThat(JsonWriter.ofFormatString("%s").withSuffix("000").writeToString(123)).isEqualTo("123000");
	}

	@Test
	void withSuffixWhenSuffixIsNullReturnsExistingWriter() {
		JsonWriter<?> formatter = JsonWriter.ofFormatString("%s");
		assertThat(formatter.withSuffix(null)).isSameAs(formatter);
	}

	@Test
	void withSuffixWhenSuffixIsEmptyReturnsExistingWriter() {
		JsonWriter<?> formatter = JsonWriter.ofFormatString("%s");
		assertThat(formatter.withSuffix("")).isSameAs(formatter);
	}

	@Test
	void withNewLineAtEndAddsNewLineToWrittenString() {
		assertThat(JsonWriter.ofFormatString("%s").withNewLineAtEnd().writeToString(123)).isEqualTo("123\n");
	}

	@Test
	void testName() {
		JsonWriter<Person> writer = JsonWriter.of((members) -> {
			members.add("firstName", Person::firstName);
			members.add("lastName", Person::lastName);
			members.add("age", Person::age);
		});
		System.out.println(writer.write(new Person("Spring", "Boot", 10)));
	}

	private static String quoted(String value) {
		return "\"" + value + "\"";
	}

	@Nested
	class StandardWriterTests {

		@Test
		void whenPrimitive() {
			assertThat(write(null)).isEqualTo("null");
			assertThat(write(123)).isEqualTo("123");
			assertThat(write(true)).isEqualTo("true");
			assertThat(write("test")).isEqualTo(quoted("test"));
		}

		@Test
		void whenMap() {
			assertThat(write(Map.of("spring", "boot"))).isEqualTo("""
					{"spring":"boot"}""");
		}

		@Test
		void whenArray() {
			assertThat(write(new int[] { 1, 2, 3 })).isEqualTo("[1,2,3]");
		}

		private <T> String write(T instance) {
			return JsonWriter.standard().writeToString(instance);
		}

	}

	record Person(String firstName, String lastName, int age) {

	}

}
