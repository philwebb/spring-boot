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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.json.JsonWriter.ValueWriter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonWriter}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
public class JsonWriterTests {

	@Test
	void using() {
		JsonWriter<Person> writer = JsonWriter.using((person, valueWriter) -> valueWriter.writePairs((pairs) -> {
			pairs.accept("firstName", person.firstName);
			pairs.accept("lastName", person.lastName);
			pairs.accept("age", person.age);
		}));
		assertThat(writer.writeToString(new Person("Spring", "Boot", 15))).isEqualTo("""
				{"firstName":"Spring","lastName":"Boot","age":15}""");
	}

	@Test
	void of() {
		JsonWriter<Person> writer = JsonWriter.of((members) -> {
			members.add("firstName", Person::firstName);
			members.add("lastName", Person::lastName);
			members.add("age", Person::age);
		});
		assertThat(writer.writeToString(new Person("Spring", "Boot", 15))).isEqualTo("""
				{"firstName":"Spring","lastName":"Boot","age":15}""");
	}

	@Nested
	class ValueWriterTests {

		@Test
		void writeWhenNullValue() {
			assertThat(write(null)).isEqualTo("null");
		}

		@Test
		void writeWhenStringValue() {
			assertThat(write("test")).isEqualTo(quoted("test"));
		}

		@Test
		void writeWhenStringValueWithEscape() {
			assertThat(write("\"")).isEqualTo(quoted("\\\""));
			assertThat(write("\\")).isEqualTo(quoted("\\\\"));
			assertThat(write("/")).isEqualTo(quoted("\\/"));
			assertThat(write("\b")).isEqualTo(quoted("\\b"));
			assertThat(write("\f")).isEqualTo(quoted("\\f"));
			assertThat(write("\n")).isEqualTo(quoted("\\n"));
			assertThat(write("\r")).isEqualTo(quoted("\\r"));
			assertThat(write("\t")).isEqualTo(quoted("\\t"));
			assertThat(write("\\u0000\\u001F")).isEqualTo(quoted("\\\\u0000\\\\u001F"));
		}

		@Test
		void writeWhenNumberValue() {
			assertThat(write((byte) 123)).isEqualTo("123");
			assertThat(write(123)).isEqualTo("123");
			assertThat(write(123L)).isEqualTo("123");
			assertThat(write(2.0)).isEqualTo("2.0");
			assertThat(write(2.0f)).isEqualTo("2.0");
			assertThat(write(Byte.valueOf((byte) 123))).isEqualTo("123");
			assertThat(write(Integer.valueOf(123))).isEqualTo("123");
			assertThat(write(Long.valueOf(123L))).isEqualTo("123");
			assertThat(write(Double.valueOf(2.0))).isEqualTo("2.0");
			assertThat(write(Float.valueOf(2.0f))).isEqualTo("2.0");
		}

		@Test
		void writeWhenBooleanValue() {
			assertThat(write(true)).isEqualTo("true");
			assertThat(write(Boolean.TRUE)).isEqualTo("true");
			assertThat(write(false)).isEqualTo("false");
			assertThat(write(Boolean.FALSE)).isEqualTo("false");
		}

		@Test
		void writeWhenStringArrayValue() {
			assertThat(write(new String[] { "a", "b", "c" })).isEqualTo("""
					["a","b","c"]""");
		}

		@Test
		void writeWhenNumberArrayValue() {
			assertThat(write(new int[] { 1, 2, 3 })).isEqualTo("[1,2,3]");
			assertThat(write(new double[] { 1.0, 2.0, 3.0 })).isEqualTo("[1.0,2.0,3.0]");
		}

		@Test
		void writeWhenBooleanArrayValue() {
			assertThat(write(new boolean[] { true, false, true })).isEqualTo("[true,false,true]");
		}

		@Test
		void writeWhenNullArrayValue() {
			assertThat(write(new Object[] { null, null })).isEqualTo("[null,null]");
		}

		@Test
		void writeWhenMixedArrayValue() {
			assertThat(write(new Object[] { "a", "b", "c", 1, 2, true, null })).isEqualTo("""
					["a","b","c",1,2,true,null]""");
		}

		@Test
		void writeWhenCollectionValue() {
			assertThat(write(List.of("a", "b", "c"))).isEqualTo("""
					["a","b","c"]""");
			assertThat(write(new LinkedHashSet<>(List.of("a", "b", "c")))).isEqualTo("""
					["a","b","c"]""");
		}

		@Test
		void writeWhenMapValue() {
			Map<String, String> map = new LinkedHashMap<>();
			map.put("a", "A");
			map.put("b", "B");
			assertThat(write(map)).isEqualTo("""
					{"a":"A","b":"B"}""");
		}

		@Test
		void writeWhenNumericalKeysMapValue() {
			Map<Integer, String> map = new LinkedHashMap<>();
			map.put(1, "A");
			map.put(2, "B");
			assertThat(write(map)).isEqualTo("""
					{"1":"A","2":"B"}""");
		}

		@Test
		void writeWhenMixedMapValue() {
			Map<Object, Object> map = new LinkedHashMap<>();
			map.put("a", 1);
			map.put("b", 2.0);
			map.put("c", true);
			map.put("d", "d");
			map.put("e", null);
			assertThat(write(map)).isEqualTo("""
					{"a":1,"b":2.0,"c":true,"d":"d","e":null}""");
		}

		@Test
		void writeObject() {
			Map<String, String> map = Map.of("a", "A");
			String actual = doWrite((valueWriter) -> valueWriter.writePairs(map::forEach));
			assertThat(actual).isEqualTo("""
					{"a":"A"}""");
		}

		@Test
		void writeObjectWhenExtracted() {
			Map<String, String> map = Map.of("a", "A");
			String actual = doWrite((valueWriter) -> valueWriter.writeEntries(map.entrySet()::forEach,
					Map.Entry::getKey, Map.Entry::getValue));
			assertThat(actual).isEqualTo("""
					{"a":"A"}""");
		}

		private <V> String write(V value) {
			return doWrite((valueWriter) -> valueWriter.write(value));
		}

		private String doWrite(Consumer<ValueWriter> action) {
			StringBuilder out = new StringBuilder();
			action.accept(new ValueWriter(out));
			return out.toString();
		}

		private String quoted(String string) {
			return "\"" + string + "\"";
		}

	}

	record Person(String firstName, String lastName, int age) {
	}

}
