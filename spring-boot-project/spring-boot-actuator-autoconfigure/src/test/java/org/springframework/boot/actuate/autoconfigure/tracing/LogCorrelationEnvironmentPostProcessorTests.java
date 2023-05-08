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

package org.springframework.boot.actuate.autoconfigure.tracing;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LogCorrelationEnvironmentPostProcessor}.
 *
 * @author Jonatan Ivanov
 */
@Disabled // FIXME
class LogCorrelationEnvironmentPostProcessorTests {

	private final ConfigurableEnvironment environment = new StandardEnvironment();

	private final SpringApplication application = new SpringApplication();

	private final LogCorrelationEnvironmentPostProcessor postProcessor = new LogCorrelationEnvironmentPostProcessor();

	@Test
	void shouldSupplyDefaultCorrelationPattern() {
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		PropertySource<?> extraLoggingProperties = this.environment.getPropertySources().get("extraLoggingProperties");
		assertThat(extraLoggingProperties).isNotNull();
		assertThat(extraLoggingProperties.getProperty("logging.pattern.correlation"))
			.isEqualTo("[${spring.application.name:},%X{traceId:-},%X{spanId:-}]");
	}

	@Test
	@ClassPathExclusions("micrometer-tracing-*.jar")
	void shouldNotSupplyCorrelationPatternIfMicrometerTracingIsMissing() {
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		PropertySource<?> extraLoggingProperties = this.environment.getPropertySources().get("extraLoggingProperties");
		assertThat(extraLoggingProperties).isNull();
	}

	@Test
	void shouldNotSupplyCorrelationPatternIfTracingIsDisabled() {
		TestPropertyValues.of("management.tracing.enabled=false").applyTo(this.environment);
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		PropertySource<?> extraLoggingProperties = this.environment.getPropertySources().get("extraLoggingProperties");
		assertThat(extraLoggingProperties).isNull();
	}

	@Test
	void shouldNotSupplyCorrelationPatternIfOneAlreadyPresent() {
		TestPropertyValues.of("logging.pattern.correlation=correlation").applyTo(this.environment);
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty("logging.pattern.correlation")).isEqualTo("correlation");
	}

	// @formatter:off
	/*

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

	 */
	// @formatter:on

}
