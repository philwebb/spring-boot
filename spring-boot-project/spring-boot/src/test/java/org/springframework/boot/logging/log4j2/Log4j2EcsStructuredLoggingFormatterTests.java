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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.logging.structured.ApplicationMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Log4j2EcsStructuredLoggingFormatter}.
 *
 * @author Moritz Halbritter
 */
class Log4j2EcsStructuredLoggingFormatterTests {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private Log4j2EcsStructuredLoggingFormatter formatter;

	@BeforeEach
	void setUp() {
		this.formatter = new Log4j2EcsStructuredLoggingFormatter(
				new ApplicationMetadata(1L, "name", "1.0.0", "test", "node-1"));
	}

	@Test
	void shouldFormat() throws JsonProcessingException {
		MutableLogEvent event = createEvent();
		event.setContextData(new JdkMapAdapterStringMap(Map.of("mdc-1", "mdc-v-1"), true));
		String format = this.formatter.format(event);
		assertThat(format).endsWith("\n");
		Map<String, Object> deserialized = OBJECT_MAPPER.readValue(format, new TypeReference<>() {
		});
		assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(map("@timestamp", "2024-07-02T08:49:53Z",
				"log.level", "INFO", "process.pid", 1, "process.thread.name", "main", "service.name", "name",
				"service.version", "1.0.0", "service.environment", "test", "service.node.name", "node-1", "log.logger",
				"org.example.Test", "message", "message", "mdc-1", "mdc-v-1", "ecs.version", "8.11"));
	}

	@Test
	void shouldFormatException() throws JsonProcessingException {
		MutableLogEvent event = createEvent();
		event.setThrown(new RuntimeException("Boom"));
		String format = this.formatter.format(event);
		Map<String, Object> deserialized = OBJECT_MAPPER.readValue(format, new TypeReference<>() {
		});
		assertThat(deserialized)
			.containsAllEntriesOf(map("error.type", "java.lang.RuntimeException", "error.message", "Boom"));
		String stackTrace = (String) deserialized.get("error.stack_trace");
		assertThat(stackTrace).startsWith(
				"""
						java.lang.RuntimeException: Boom
						\tat org.springframework.boot.logging.log4j2.Log4j2EcsStructuredLoggingFormatterTests.shouldFormatException
						"""
					.trim());
		assertThat(format).contains(
				"""
						java.lang.RuntimeException: Boom\\n\\tat org.springframework.boot.logging.log4j2.Log4j2EcsStructuredLoggingFormatterTests.shouldFormatException
						"""
					.trim());
	}

	private Map<String, Object> map(Object... values) {
		assertThat(values.length).isEven();
		Map<String, Object> result = new HashMap<>();
		for (int i = 0; i < values.length; i += 2) {
			result.put(values[i].toString(), values[i + 1]);
		}
		return result;
	}

	private static MutableLogEvent createEvent() {
		MutableLogEvent event = new MutableLogEvent();
		event.setTimeMillis(1719910193000L);
		event.setLevel(Level.INFO);
		event.setThreadName("main");
		event.setLoggerName("org.example.Test");
		event.setMessage(new SimpleMessage("message"));
		return event;
	}

}
