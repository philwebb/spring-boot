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

package org.springframework.boot.logging.log4j2;

import java.util.Map;

import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;

import org.springframework.boot.logging.structured.ApplicationMetadata;
import org.springframework.boot.logging.structured.StructuredLoggingFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link StructuredLoggingLayout}.
 *
 * @author Moritz Halbritter
 */
class StructuredLoggingLayoutTests extends AbstractStructuredLoggingTests {

	@Test
	void shouldSupportEcsCommonFormat() {
		StructuredLoggingLayout layout = StructuredLoggingLayout.newBuilder().setFormat("ecs").build();
		String json = layout.toSerializable(createEvent());
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized).containsKey("ecs.version");
	}

	@Test
	void shouldSupportLogstashCommonFormat() {
		StructuredLoggingLayout layout = StructuredLoggingLayout.newBuilder().setFormat("logstash").build();
		String json = layout.toSerializable(createEvent());
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized).containsKey("@version");
	}

	@Test
	void shouldSupportCustomFormat() {
		StructuredLoggingLayout layout = StructuredLoggingLayout.newBuilder()
			.setFormat(CustomLog4j2StructuredLoggingFormatter.class.getName())
			.build();
		String format = layout.toSerializable(createEvent());
		assertThat(format).isEqualTo("custom-format");
	}

	@Test
	void shouldInjectCustomFormatConstructorParameters() {
		StructuredLoggingLayout layout = StructuredLoggingLayout.newBuilder()
			.setFormat(CustomLog4j2StructuredLoggingFormatterWithInjection.class.getName())
			.setPid(1L)
			.build();
		String format = layout.toSerializable(createEvent());
		assertThat(format).isEqualTo("custom-format-with-injection pid=1");
	}

	@Test
	void shouldCheckTypeArgument() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> StructuredLoggingLayout.newBuilder()
				.setFormat(CustomLog4j2StructuredLoggingFormatterWrongType.class.getName())
				.build())
			.withMessageContaining("must be org.apache.logging.log4j.core.LogEvent, but was java.lang.String");
	}

	@Test
	void shouldCheckTypeArgumentWithRawType() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> StructuredLoggingLayout.newBuilder()
				.setFormat(CustomLog4j2StructuredLoggingFormatterRawType.class.getName())
				.build())
			.withMessageContaining("must be org.apache.logging.log4j.core.LogEvent, but was null");
	}

	@Test
	void shouldFailIfNoCommonOrCustomFormatIsSet() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> StructuredLoggingLayout.newBuilder().setFormat("does-not-exist").build())
			.withMessageContaining("Unknown format 'does-not-exist'. Common formats are: ecs, logstash");
	}

	static final class CustomLog4j2StructuredLoggingFormatter implements StructuredLoggingFormatter<LogEvent> {

		@Override
		public String format(LogEvent event) {
			return "custom-format";
		}

	}

	static final class CustomLog4j2StructuredLoggingFormatterWithInjection
			implements StructuredLoggingFormatter<LogEvent> {

		private final ApplicationMetadata metadata;

		CustomLog4j2StructuredLoggingFormatterWithInjection(ApplicationMetadata metadata) {
			this.metadata = metadata;
		}

		@Override
		public String format(LogEvent event) {
			return "custom-format-with-injection pid=" + this.metadata.pid();
		}

	}

	static final class CustomLog4j2StructuredLoggingFormatterWrongType implements StructuredLoggingFormatter<String> {

		@Override
		public String format(String event) {
			return event;
		}

	}

	@SuppressWarnings("rawtypes")
	static final class CustomLog4j2StructuredLoggingFormatterRawType implements StructuredLoggingFormatter {

		@Override
		public String format(Object event) {
			return "";
		}

	}

}
