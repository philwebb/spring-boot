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

package org.springframework.boot.logging.logback;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LogbackLogstashStructuredLoggingFormatter}.
 *
 * @author Moritz Halbritter
 */
class LogbackLogstashStructuredLoggingFormatterTests extends AbstractStructuredLoggingTests {

	private LogbackLogstashStructuredLoggingFormatter formatter;

	@Override
	@BeforeEach
	void setUp() {
		super.setUp();
		this.formatter = new LogbackLogstashStructuredLoggingFormatter(getThrowableProxyConverter());
	}

	@Test
	void shouldFormat() {
		LoggingEvent event = createEvent();
		event.setMDCPropertyMap(Map.of("mdc-1", "mdc-v-1"));
		event.setKeyValuePairs(keyValuePairs("kv-1", "kv-v-1"));
		Marker marker1 = getMarker("marker-1");
		marker1.add(getMarker("marker-2"));
		event.addMarker(marker1);
		String json = this.formatter.format(event);
		assertThat(json).endsWith("\n");
		Map<String, Object> deserialized = deserialize(json);
		assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(
				map("@timestamp", "2024-07-02T10:49:53+02:00", "@version", "1", "message", "message", "logger_name",
						"org.example.Test", "thread_name", "main", "level", "INFO", "level_value", 20000, "mdc-1",
						"mdc-v-1", "kv-1", "kv-v-1", "tags", List.of("marker-1", "marker-2")));
	}

	@Test
	void shouldFormatException() {
		LoggingEvent event = createEvent();
		event.setThrowableProxy(new ThrowableProxy(new RuntimeException("Boom")));
		event.setMDCPropertyMap(Collections.emptyMap());
		String json = this.formatter.format(event);
		Map<String, Object> deserialized = deserialize(json);
		String stackTrace = (String) deserialized.get("stack_trace");
		assertThat(stackTrace).startsWith(
				"""
						java.lang.RuntimeException: Boom
						\tat org.springframework.boot.logging.logback.LogbackLogstashStructuredLoggingFormatterTests.shouldFormatException
						"""
					.trim());
		assertThat(json).contains(
				"""
						java.lang.RuntimeException: Boom\\n\\tat org.springframework.boot.logging.logback.LogbackLogstashStructuredLoggingFormatterTests.shouldFormatException
						"""
					.trim());
	}

}
