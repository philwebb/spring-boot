/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Context;
import org.junit.jupiter.api.Test;

import org.springframework.boot.logging.CorrelationIdFormatter;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CorrelationIdConverter}.
 *
 * @author Phillip Webb
 */
class CorrelationIdConverterTests {

	private final CorrelationIdConverter converter;

	private final LoggingEvent event = new LoggingEvent();

	CorrelationIdConverterTests() {
		MockEnvironment environment = new MockEnvironment();
		environment.withProperty(CorrelationIdFormatter.APPLICATION_PROPERTY, "APP");
		this.converter = new CorrelationIdConverter();
		Context context = new LoggerContext();
		context.putObject(Environment.class.getName(), environment);
		this.converter.setContext(context);
	}

	@Test
	void defaultPattern() {
		addMdcProperties(this.event);
		this.converter.start();
		String converted = this.converter.convert(this.event);
		this.converter.stop();
		assertThat(converted).isEqualTo("[APP|TRACE                           |SPAN            ] ");
	}

	@Test
	void customPattern() {
		this.converter.setOptionList(List.of("-", "traceId", "spanId"));
		addMdcProperties(this.event);
		this.converter.start();
		String converted = this.converter.convert(this.event);
		this.converter.stop();
		assertThat(converted).isEqualTo("[TRACE-SPAN] ");
	}

	private void addMdcProperties(LoggingEvent event) {
		event.setMDCPropertyMap(Map.of("traceId", "TRACE", "spanId", "SPAN"));
	}

}
