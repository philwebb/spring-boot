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

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.ClassUtils;

/**
 * {@link EnvironmentPostProcessor} to add correlation pattern to the logging.
 *
 * @author Jonatan Ivanov
 */
class LogCorrelationEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private static final String CORRELATION_PATTERN_KEY = "logging.pattern.correlation";

	private static final String CORRELATION_PATTERN_DEFAULT_VALUE = "[${spring.application.name:},%X{traceId:-},%X{spanId:-}]";

	private static final String TRACER_CLASS_NAME = "io.micrometer.tracing.Tracer";

	private static final String TRACING_ENABLED_KEY = "management.tracing.enabled";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (shouldSetCorrelationPattern(environment, application.getClassLoader())) {
			Map<String, Object> properties = Map.of(CORRELATION_PATTERN_KEY, CORRELATION_PATTERN_DEFAULT_VALUE);
			environment.getPropertySources().addLast(new MapPropertySource("extraLoggingProperties", properties));
		}
	}

	private boolean shouldSetCorrelationPattern(ConfigurableEnvironment environment, ClassLoader classLoader) {
		return isTracerPresent(classLoader) && isTracingEnabled(environment)
				&& getCorrelationPattern(environment) == null;
	}

	private boolean isTracerPresent(ClassLoader classLoader) {
		return ClassUtils.isPresent(TRACER_CLASS_NAME, classLoader);
	}

	private boolean isTracingEnabled(ConfigurableEnvironment environment) {
		String tracingEnabledProperty = environment.getProperty(TRACING_ENABLED_KEY);
		return tracingEnabledProperty == null || Boolean.parseBoolean(tracingEnabledProperty);
	}

	private String getCorrelationPattern(ConfigurableEnvironment environment) {
		return environment.getProperty(CORRELATION_PATTERN_KEY);
	}

}
