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

package org.springframework.boot.logging.log4j2;

import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.core.AbstractLogEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertySource;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.logging.CorrelationIdFormatter;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CorrelationIdConverter}.
 *
 * @author Phillip Webb
 */
class CorrelationIdConverterTests {

	private CorrelationIdConverter converter = CorrelationIdConverter.newInstance(null);

	private final LogEvent event = new TestLogEvent();

	@BeforeEach
	void setup() {
		cleanUpPropertySources();
		MockEnvironment environment = new MockEnvironment();
		environment.withProperty(CorrelationIdFormatter.APPLICATION_PROPERTY, "APP");
		PropertiesUtil.getProperties().addPropertySource(new SpringEnvironmentPropertySource(environment));
	}

	@AfterEach
	void cleanUp() {
		cleanUpPropertySources();
	}

	@SuppressWarnings("unchecked")
	private void cleanUpPropertySources() { // https://issues.apache.org/jira/browse/LOG4J2-3618
		PropertiesUtil properties = PropertiesUtil.getProperties();
		Object environment = ReflectionTestUtils.getField(properties, "environment");
		Set<PropertySource> sources = (Set<PropertySource>) ReflectionTestUtils.getField(environment, "sources");
		sources.removeIf((candidate) -> candidate instanceof SpringEnvironmentPropertySource
				|| candidate instanceof SpringBootPropertySource);
	}

	@Test
	void defaultPattern() {
		StringBuilder result = new StringBuilder();
		this.converter.format(this.event, result);
		assertThat(result).hasToString("[APP|TRACE                           |SPAN            ] ");
	}

	@Test
	void customPattern() {
		this.converter = CorrelationIdConverter.newInstance(new String[] { "-", "traceId", "spanId" });
		StringBuilder result = new StringBuilder();
		this.converter.format(this.event, result);
		assertThat(result).hasToString("[TRACE-SPAN] ");
	}

	static class TestLogEvent extends AbstractLogEvent {

		@Override
		public ReadOnlyStringMap getContextData() {
			return new JdkMapAdapterStringMap(Map.of("traceId", "TRACE", "spanId", "SPAN"));
		}

	}

}
