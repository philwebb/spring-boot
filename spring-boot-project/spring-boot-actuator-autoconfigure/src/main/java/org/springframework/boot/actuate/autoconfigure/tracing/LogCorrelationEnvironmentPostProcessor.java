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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.CorrelationIdFormatter;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.ClassUtils;

/**
 * {@link EnvironmentPostProcessor} to add a {@link PropertySource} to support log
 * correlation IDs.
 * <p>
 * Support for following properties are added:
 * <ul>
 * <li>{@value LoggingSystem#EXPECT_CORRELATION_ID_PROPERTY} for {@link LoggingSystem}
 * support.</li>
 * <li>{@value CorrelationIdFormatter#APPLICATION_PROPERTY} for
 * {@link CorrelationIdFormatter} support.</li>
 * </ul>
 *
 * @author Jonatan Ivanov
 * @author Phillip Webb
 */
class LogCorrelationEnvironmentPostProcessor implements EnvironmentPostProcessor {

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (ClassUtils.isPresent("io.micrometer.tracing.Tracer", application.getClassLoader())) {
			environment.getPropertySources().addLast(new LogCorrelationPropertySource(this, environment));
		}
	}

	/**
	 * Log correlation {@link PropertySource}.
	 */
	private static class LogCorrelationPropertySource extends PropertySource<Object> {

		private static final String NAME = "logCorrelation";

		private final Environment environment;

		private final Map<String, String> hashes = new HashMap<>();

		LogCorrelationPropertySource(Object source, Environment environment) {
			super(NAME, source);
			this.environment = environment;
		}

		@Override
		public Object getProperty(String name) {
			if (name.equals(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY)) {
				return this.environment.getProperty("management.tracing.enabled", Boolean.class, Boolean.TRUE);
			}
			if (name.equals(CorrelationIdFormatter.APPLICATION_PROPERTY)) {
				String applicationName = this.environment.getProperty("spring.application.name");
				return this.hashes.computeIfAbsent(applicationName, this::computeHash);
			}
			return null;
		}

		private String computeHash(String input) {
			try {
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
				String hex = HexFormat.of().formatHex(bytes);
				return hex.substring(0, 10);
			}
			catch (NoSuchAlgorithmException ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

}
