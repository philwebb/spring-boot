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

	@Nested
	class ValueWriterTests {

		@Test
		void writeWhenNullValue() throws Exception {
			assertThat(write(null)).isEqualTo("null");
		}

		@Test
		void writeWhenStringValue() throws Exception {
			assertThat(write("test")).isEqualTo(quoted("test"));
		}

		@Test
		void writeWhenStringValueWithEscape() throws Exception {
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
		void writeWhenNumberValue() throws Exception {
			assertThat(write((byte) 123)).isEqualTo("123");
			assertThat(write(123)).isEqualTo("123");
			assertThat(write(123L)).isEqualTo("123");
			assertThat(write(Byte.valueOf((byte) 123))).isEqualTo("123");
			assertThat(write(Integer.valueOf(123))).isEqualTo("123");
			assertThat(write(Long.valueOf(123L))).isEqualTo("123");
		}

		@Test
		void writeWhenBooleanValue() throws Exception {
			assertThat(write(true)).isEqualTo("true");
			assertThat(write(Boolean.TRUE)).isEqualTo("true");
			assertThat(write(false)).isEqualTo("false");
			assertThat(write(Boolean.FALSE)).isEqualTo("false");
		}

		@Test
		void writeWhenStringArray() throws Exception {
			assertThat(write(new String[] { "a", "b", "c" })).isEqualTo("""
					["a","b","c"]""");
		}

		@Test
		void writeWhenNumberArray() throws Exception {
			assertThat(write(new int[] { 1, 2, 3 })).isEqualTo("[1,2,3]");
		}

		@Test
		void writeWhenBooleanArray() throws Exception {
			assertThat(write(new boolean[] { true, false, true })).isEqualTo("[true,false,true]");
		}

		@Test
		void writeWhenCollection() throws Exception {
			assertThat(write(List.of("a", "b", "c"))).isEqualTo("""
					["a","b","c"]""");
		}

		private <V> String write(V value) throws Exception {
			StringBuilder out = new StringBuilder();
			new ValueWriter(out).write(value);
			return out.toString();
		}

		private String quoted(String string) {
			return "\"" + string + "\"";
		}

	}

}
