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

package org.springframework.boot.logging;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link MappedDiagnosticContextInitializer}.
 *
 * @author Phillip Webb
 */
class MappedDiagnosticContextInitializerTests {

	private MockEnvironment environment = new MockEnvironment();

	private Map<String, String> mdc = new LinkedHashMap<>();

	private LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(
			this.environment);

	private MappedDiagnosticContextInitializer initializer = new MappedDiagnosticContextInitializer(this.mdc::put);

	@Test
	void initializeWithDefaultsWhenHasNoApplicationName() {
		this.initializer.initialize(this.loggingInitializationContext);
		assertThat(this.mdc).isEmpty();
	}

	@Test
	void initializeWithDefaultsWhenHasApplicationName() {
		this.environment.setProperty("spring.application.name", "test");
		this.initializer.initialize(this.loggingInitializationContext);
		assertThat(this.mdc).containsOnly(entry("applicationCorrelationId", "9f86d08188"));
	}

	@Test
	void initializeWithDefaultsWhenHasApplicationCorrelationIdentifier() {
		this.environment.setProperty("spring.application.name", "test");
		this.environment.setProperty("spring.application.correlation-id", "0000");
		this.initializer.initialize(this.loggingInitializationContext);
		assertThat(this.mdc).containsOnly(entry("applicationCorrelationId", "0000"));
	}

	@Test
	void initializeWithoutApplicationCorrelationId() {
		this.environment.setProperty("spring.application.correlation-id", "0000");
		this.environment.setProperty("logging.mdc.put.application-correlation-id", "false");
		this.initializer.initialize(this.loggingInitializationContext);
		assertThat(this.mdc).isEmpty();
	}

	@Test
	void initializeWithProperties() {
		this.environment.setProperty("logging.mdc.put.properties.spring", "boot");
		this.environment.setProperty("logging.mdc.put.properties.log", "back");
		this.initializer.initialize(this.loggingInitializationContext);
		assertThat(this.mdc).containsOnly(entry("spring", "boot"), entry("log", "back"));
	}

	@Test
	void intializeWithPlaceholder() {
		this.environment.setProperty("spring.application.name", "test");
		this.environment.setProperty("logging.mdc.put.properties.name", "${spring.application.name}");
		this.environment.setProperty("logging.mdc.put.properties.id", "${spring.application.correlation-id}");
		this.initializer.initialize(this.loggingInitializationContext);
		assertThat(this.mdc).containsOnly(entry("name", "test"), entry("id", "9f86d08188"));
	}

}
