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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.event.KeyValuePair;

import org.springframework.boot.logging.logback.JsonEncoder.CommonFormats;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonEncoder}.
 *
 * @author Moritz Halbritter
 */
class JsonEncoderTests {

	private static final Instant NOW = Instant.parse("2020-02-02T01:01:01Z");

	private JsonEncoder jsonEncoder;

	private LoggerContext loggerContext;

	@BeforeEach
	void setUp() {
		this.loggerContext = new LoggerContext();
		this.loggerContext.setMDCAdapter(new LogbackMDCAdapter());
		this.jsonEncoder = new JsonEncoder(CommonFormats.ECS);
		this.jsonEncoder.setContext(this.loggerContext);
		this.jsonEncoder.start();
	}

	@AfterEach
	void tearDown() {
		this.jsonEncoder.stop();
	}

	@Test
	void shouldEncodeBasicMessage() {
		String message = encode(event(Level.INFO, "Some message"));
		assertThat(message).isEqualTo(
				"""
						{"@timestamp":"2020-02-02T01:01:01Z","log.level":"INFO","message":"Some message","ecs.version":"1.2.0","process.thread.name":"main","log.logger":"org.springframework.boot.logging.logback.JsonEncoderTests"}
						"""
					.trim());
	}

	@Test
	void shouldEscapeMessage() {
		String message = encode(event(Level.TRACE, "Some message\r\n\t\f\b\\\""));
		assertThat(message).isEqualTo(
				"""
						{"@timestamp":"2020-02-02T01:01:01Z","log.level":"TRACE","message":"Some message\\r\\n\\t\\f\\b\\\\\\"","ecs.version":"1.2.0","process.thread.name":"main","log.logger":"org.springframework.boot.logging.logback.JsonEncoderTests"}
						"""
					.trim());
	}

	@Test
	void shouldAddMdcKeys() {
		String message = encode(event(Level.WARN, "Some message", Map.of("mdc-k1", "mdc-v1")));
		assertThat(message).isEqualTo(
				"""
						{"@timestamp":"2020-02-02T01:01:01Z","log.level":"WARN","message":"Some message","ecs.version":"1.2.0","process.thread.name":"main","log.logger":"org.springframework.boot.logging.logback.JsonEncoderTests","mdc-k1":"mdc-v1"}
						"""
					.trim());
	}

	@Test
	void shouldAddKeyValuePairs() {
		String message = encode(event(Level.ERROR, "Some message", Collections.emptyMap(), Map.of("kvp-k1", "kvp-v1")));
		assertThat(message).isEqualTo(
				"""
						{"@timestamp":"2020-02-02T01:01:01Z","log.level":"ERROR","message":"Some message","ecs.version":"1.2.0","process.thread.name":"main","log.logger":"org.springframework.boot.logging.logback.JsonEncoderTests","kvp-k1":"kvp-v1"}
						"""
					.trim());
	}

	private String encode(ILoggingEvent event) {
		return new String(this.jsonEncoder.encode(event), StandardCharsets.UTF_8);
	}

	private ILoggingEvent event(Level level, String message) {
		return event(level, message, Collections.emptyMap());
	}

	private ILoggingEvent event(Level level, String message, Map<String, String> mdc) {
		return event(level, message, mdc, Collections.emptyMap());
	}

	private ILoggingEvent event(Level level, String message, Map<String, String> mdc,
			Map<String, Object> keyValuePairs) {
		LoggingEvent event = new LoggingEvent();
		event.setMDCPropertyMap(mdc);
		event.setLoggerContext(this.loggerContext);
		event.setInstant(NOW);
		event.setLoggerName(JsonEncoderTests.class.getName());
		event.setThreadName("main");
		event.setMessage(message);
		event.setLevel(level);
		event.setKeyValuePairs(keyValuePairs.entrySet()
			.stream()
			.map((entry) -> new KeyValuePair(entry.getKey(), entry.getValue()))
			.toList());
		return event;
	}

}
