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

import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CorrelationIdConverter}.
 *
 * @author Phillip Webb
 */
class CorrelationIdConverterTests {

	private final CorrelationIdConverter converter = new CorrelationIdConverter();

	private final LoggingEvent event = new LoggingEvent();

	@Test
	void defaultPattern() {
		addMdcProperties(this.event);
		this.converter.start();
		String converted = this.converter.convert(this.event);
		this.converter.stop();
		assertThat(converted).isEqualTo("[TRACE-SPAN                                       ] ");
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
