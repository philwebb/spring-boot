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

import java.util.function.BooleanSupplier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.ClassUtils;

/**
 * {@link EnvironmentPostProcessor} to add a
 * {@value LogCorrelationEnvironmentPostProcessor#EXPECT_CORRELATION_ID_PROPERTY} to
 * support adding correlation identifiers to the log.
 *
 * @author Jonatan Ivanov
 */
class LogCorrelationEnvironmentPostProcessor implements EnvironmentPostProcessor {

	static final String EXPECT_CORRELATION_ID_PROPERTY = "logging.mdc.expect-correlation-id";

	private static final String TRACING_ENABLED_KEY = "management.tracing.enabled";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (ClassUtils.isPresent("io.micrometer.tracing.Tracer", application.getClassLoader())) {
			BooleanSupplier expected = () -> environment.getProperty(TRACING_ENABLED_KEY, Boolean.class, Boolean.TRUE);
			environment.getPropertySources().addLast(new LogCorrelationPropertySource(expected));
		}
	}

	/**
	 * {@link PropertySource} to resolve
	 * {@value LogCorrelationEnvironmentPostProcessor#EXPECT_CORRELATION_ID_PROPERTY}.
	 */
	private static class LogCorrelationPropertySource extends PropertySource<BooleanSupplier> {

		private static final String NAME = "logCorrelation";

		public LogCorrelationPropertySource(BooleanSupplier expected) {
			super(NAME, expected);
		}

		@Override
		public Object getProperty(String name) {
			if (EXPECT_CORRELATION_ID_PROPERTY.equals(name) && getSource().getAsBoolean()) {
				return Boolean.TRUE;
			}
			return null;
		}

	}

}
