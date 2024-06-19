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

import java.util.Locale;

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.arbiters.Arbiter;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import org.springframework.boot.logging.LoggingSystemProperty;
import org.springframework.util.StringUtils;

/**
 * An {@link Arbiter} that includes children if structured logging for a given appender is
 * enabled.
 *
 * @author Moritz Halbritter
 */
@Plugin(name = "StructuredLoggingEnabledArbiter", category = Node.CATEGORY, elementType = Arbiter.ELEMENT_TYPE,
		deferChildren = true, printObject = true)
final class StructuredLoggingEnabledArbiter implements Arbiter {

	private final Appender appender;

	StructuredLoggingEnabledArbiter(Appender appender) {
		this.appender = appender;
	}

	@Override
	public boolean isCondition() {
		String format = System.getProperty(this.appender.getEnvironmentVariableName(), null);
		return StringUtils.hasLength(format);
	}

	@PluginFactory
	static StructuredLoggingEnabledArbiter create(@PluginAttribute("appender") String appender) {
		return new StructuredLoggingEnabledArbiter(Appender.parse(appender));
	}

	private enum Appender {

		CONSOLE() {
			@Override
			String getEnvironmentVariableName() {
				return LoggingSystemProperty.CONSOLE_STRUCTURED_LOGGING_FORMAT.getEnvironmentVariableName();
			}
		},
		FILE {
			@Override
			String getEnvironmentVariableName() {
				return LoggingSystemProperty.FILE_STRUCTURED_LOGGING_FORMAT.getEnvironmentVariableName();
			}
		};

		abstract String getEnvironmentVariableName();

		static Appender parse(String input) {
			return valueOf(input.toUpperCase(Locale.ENGLISH));
		}

	}

}
