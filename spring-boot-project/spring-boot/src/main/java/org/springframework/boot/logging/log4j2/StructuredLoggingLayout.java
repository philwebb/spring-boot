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

import java.nio.charset.Charset;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.logging.structured.ApplicationMetadata;
import org.springframework.boot.logging.structured.Log4j2EcsStructuredLoggingFormatter;
import org.springframework.boot.logging.structured.Log4j2LogfmtStructuredLoggingFormatter;
import org.springframework.boot.logging.structured.Log4j2LogstashStructuredLoggingFormatter;
import org.springframework.boot.logging.structured.StructuredLoggingFormatter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link Layout} which writes log events in structured format.
 *
 * @author Moritz Halbritter
 * @see StructuredLoggingFormatter
 */
@Plugin(name = "StructuredLoggingLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE)
final class StructuredLoggingLayout extends AbstractStringLayout {

	private final StructuredLoggingFormatter<LogEvent> format;

	private StructuredLoggingLayout(StructuredLoggingFormatter<LogEvent> format, Charset charset) {
		super(charset);
		Assert.notNull(format, "Format must not be null");
		this.format = format;
	}

	@Override
	public String toSerializable(LogEvent event) {
		return this.format.format(event);
	}

	@PluginBuilderFactory
	static StructuredLoggingLayout.Builder newBuilder() {
		return new StructuredLoggingLayout.Builder();
	}

	static final class Builder implements org.apache.logging.log4j.core.util.Builder<StructuredLoggingLayout> {

		@PluginBuilderAttribute
		private String format;

		@PluginBuilderAttribute
		private String charset;

		@PluginBuilderAttribute
		private Long pid;

		@PluginBuilderAttribute
		private String serviceName;

		@PluginBuilderAttribute
		private String serviceVersion;

		@PluginBuilderAttribute
		private String serviceNodeName;

		@PluginBuilderAttribute
		private String serviceEnvironment;

		@Override
		public StructuredLoggingLayout build() {
			ApplicationMetadata metadata = new ApplicationMetadata(this.pid, this.serviceName, this.serviceVersion,
					this.serviceEnvironment, this.serviceNodeName);
			StructuredLoggingFormatter<LogEvent> format = createFormat(this.format, metadata);
			return new StructuredLoggingLayout(format, Charset.forName(this.charset));
		}

		@SuppressWarnings("unchecked")
		private StructuredLoggingFormatter<LogEvent> createFormat(String format, ApplicationMetadata metadata) {
			StructuredLoggingFormatter<LogEvent> commonFormat = getCommonFormat(format, metadata);
			if (commonFormat != null) {
				return commonFormat;
			}
			else if (ClassUtils.isPresent(format, null)) {
				StructuredLoggingFormatter<LogEvent> structuredLoggingFormatter = BeanUtils
					.instantiateClass(ClassUtils.resolveClassName(format, null), StructuredLoggingFormatter.class);
				// TODO MH: Check if generic is LogEvent
				// TODO MH: Inject ApplicationMetadata?
				return structuredLoggingFormatter;
			}
			else {
				throw new IllegalArgumentException(
						"Unknown format '%s'. Common formats are: ecs, logfmt, logstash".formatted(format));
			}
		}

		private StructuredLoggingFormatter<LogEvent> getCommonFormat(String format, ApplicationMetadata metadata) {
			return switch (format) {
				case "ecs" -> new Log4j2EcsStructuredLoggingFormatter(metadata);
				case "logstash" -> new Log4j2LogstashStructuredLoggingFormatter();
				case "logfmt" -> new Log4j2LogfmtStructuredLoggingFormatter();
				default -> null;
			};
		}

	}

}
