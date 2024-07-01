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

import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.util.Map;

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
import org.springframework.core.GenericTypeResolver;
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

	private final StructuredLoggingFormatter<LogEvent> formatter;

	private StructuredLoggingLayout(StructuredLoggingFormatter<LogEvent> formatter, Charset charset) {
		super(charset);
		Assert.notNull(formatter, "Formatter must not be null");
		this.formatter = formatter;
	}

	@Override
	public String toSerializable(LogEvent event) {
		return this.formatter.format(event);
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
			StructuredLoggingFormatter<LogEvent> format = createFormatter(this.format, metadata);
			return new StructuredLoggingLayout(format, Charset.forName(this.charset));
		}

		private StructuredLoggingFormatter<LogEvent> createFormatter(String format, ApplicationMetadata metadata) {
			StructuredLoggingFormatter<LogEvent> commonFormatter = getCommonFormatter(format, metadata);
			if (commonFormatter != null) {
				return commonFormatter;
			}
			else if (ClassUtils.isPresent(format, null)) {
				return new CustomStructuredLoggingFormatterFactory(metadata)
					.create(ClassUtils.resolveClassName(format, null));
			}
			else {
				throw new IllegalArgumentException(
						"Unknown format '%s'. Common formats are: ecs, logfmt, logstash".formatted(format));
			}
		}

		private StructuredLoggingFormatter<LogEvent> getCommonFormatter(String format, ApplicationMetadata metadata) {
			return switch (format) {
				case "ecs" -> new Log4j2EcsStructuredLoggingFormatter(metadata);
				case "logstash" -> new Log4j2LogstashStructuredLoggingFormatter();
				case "logfmt" -> new Log4j2LogfmtStructuredLoggingFormatter();
				default -> null;
			};
		}

	}

	private static class CustomStructuredLoggingFormatterFactory {

		private final Map<Class<?>, Object> supportedConstructorParameters;

		CustomStructuredLoggingFormatterFactory(ApplicationMetadata metadata) {
			this.supportedConstructorParameters = Map.of(ApplicationMetadata.class, metadata);
		}

		@SuppressWarnings("unchecked")
		StructuredLoggingFormatter<LogEvent> create(Class<?> clazz) {
			Constructor<?> constructor = BeanUtils.getResolvableConstructor(clazz);
			Object[] arguments = new Object[constructor.getParameterCount()];
			int index = 0;
			for (Class<?> parameterType : constructor.getParameterTypes()) {
				Object argument = this.supportedConstructorParameters.get(parameterType);
				Assert.notNull(argument, () -> "Unable to supply value to %s constructor argument of type %s"
					.formatted(clazz.getName(), parameterType.getName()));
				arguments[index] = argument;
				index++;
			}
			Object formatter = BeanUtils.instantiateClass(constructor, arguments);
			checkType(formatter);
			checkTypeArgument(formatter);
			return (StructuredLoggingFormatter<LogEvent>) formatter;
		}

		private static void checkType(Object formatter) {
			Assert.isInstanceOf(StructuredLoggingFormatter.class, formatter,
					() -> "Formatter must be of type %s, but was %s"
						.formatted(StructuredLoggingFormatter.class.getName(), formatter.getClass().getName()));
		}

		private static void checkTypeArgument(Object formatter) {
			Class<?> typeArgument = GenericTypeResolver.resolveTypeArgument(formatter.getClass(),
					StructuredLoggingFormatter.class);
			Assert.isTrue(typeArgument == LogEvent.class,
					() -> "Type argument of %s must be %s, but was %s".formatted(formatter.getClass().getName(),
							LogEvent.class.getName(), (typeArgument != null) ? typeArgument.getName() : "null"));
		}

	}

}
