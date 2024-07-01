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

package org.springframework.boot.logging.logback;

import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.EncoderBase;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.logging.structured.ApplicationMetadata;
import org.springframework.boot.logging.structured.LogbackLogfmtStructuredLoggingFormatter;
import org.springframework.boot.logging.structured.StructuredLoggingFormatter;
import org.springframework.core.GenericTypeResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link Encoder Logback encoder} which encodes to structured logging based formats.
 *
 * @author Moritz Halbritter
 * @since 3.4.0
 * @see StructuredLoggingFormatter
 */
public class StructuredLoggingEncoder extends EncoderBase<ILoggingEvent> {

	private final ThrowableProxyConverter throwableProxyConverter = new ThrowableProxyConverter();

	private String format;

	private StructuredLoggingFormatter<ILoggingEvent> formatter;

	private Long pid;

	private String serviceName;

	private String serviceVersion;

	private String serviceNodeName;

	private String serviceEnvironment;

	private Charset charset = StandardCharsets.UTF_8;

	/**
	 * Sets the format. Accepts either a common format ID, or a fully qualified class
	 * name.
	 * @param format the format
	 */
	public void setFormat(String format) {
		this.format = format;
	}

	public void setPid(Long pid) {
		this.pid = pid;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void setServiceVersion(String serviceVersion) {
		this.serviceVersion = serviceVersion;
	}

	public void setServiceNodeName(String serviceNodeName) {
		this.serviceNodeName = serviceNodeName;
	}

	public void setServiceEnvironment(String serviceEnvironment) {
		this.serviceEnvironment = serviceEnvironment;
	}

	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	@Override
	public void start() {
		Assert.state(this.format != null, "Format has not been set");
		this.formatter = createFormatter(this.format);
		super.start();
		this.throwableProxyConverter.start();
	}

	private StructuredLoggingFormatter<ILoggingEvent> createFormatter(String format) {
		ApplicationMetadata metadata = new ApplicationMetadata(this.pid, this.serviceName, this.serviceVersion,
				this.serviceEnvironment, this.serviceNodeName);
		StructuredLoggingFormatter<ILoggingEvent> commonFormatter = getCommonFormatter(format, metadata);
		if (commonFormatter != null) {
			return commonFormatter;
		}
		if (ClassUtils.isPresent(format, null)) {
			return new CustomStructuredLoggingFormatterFactory(metadata, this.throwableProxyConverter)
				.create(ClassUtils.resolveClassName(format, null));
		}
		throw new IllegalArgumentException(
				"Unknown format '%s'. Supported common formats are: ecs, logfmt, logstash".formatted(format));
	}

	private StructuredLoggingFormatter<ILoggingEvent> getCommonFormatter(String format, ApplicationMetadata metadata) {
		return switch (format) {
			case "ecs" -> new LogbackEcsStructuredLoggingFormatter(this.throwableProxyConverter, metadata);
			case "logstash" -> new LogbackLogstashStructuredLoggingFormatter(this.throwableProxyConverter);
			case "logfmt" -> new LogbackLogfmtStructuredLoggingFormatter(this.throwableProxyConverter);
			default -> null;
		};
	}

	@Override
	public void stop() {
		this.throwableProxyConverter.stop();
		super.stop();
	}

	@Override
	public byte[] headerBytes() {
		return null;
	}

	@Override
	public byte[] encode(ILoggingEvent event) {
		String line = this.formatter.format(event);
		return line.getBytes(this.charset);
	}

	@Override
	public byte[] footerBytes() {
		return null;
	}

	private static class CustomStructuredLoggingFormatterFactory {

		private final Map<Class<?>, Object> supportedConstructorParameters;

		CustomStructuredLoggingFormatterFactory(ApplicationMetadata metadata,
				ThrowableProxyConverter throwableProxyConverter) {
			this.supportedConstructorParameters = Map.of(ApplicationMetadata.class, metadata,
					ThrowableProxyConverter.class, throwableProxyConverter);
		}

		@SuppressWarnings("unchecked")
		StructuredLoggingFormatter<ILoggingEvent> create(Class<?> clazz) {
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
			return (StructuredLoggingFormatter<ILoggingEvent>) formatter;
		}

		private static void checkType(Object formatter) {
			Assert.isInstanceOf(StructuredLoggingFormatter.class, formatter,
					() -> "Formatter must be of type %s, but was %s"
						.formatted(StructuredLoggingFormatter.class.getName(), formatter.getClass().getName()));
		}

		private static void checkTypeArgument(Object formatter) {
			Class<?> typeArgument = GenericTypeResolver.resolveTypeArgument(formatter.getClass(),
					StructuredLoggingFormatter.class);
			Assert.isTrue(typeArgument == ILoggingEvent.class,
					() -> "Type argument of %s must be %s, but was %s".formatted(formatter.getClass().getName(),
							ILoggingEvent.class.getName(), (typeArgument != null) ? typeArgument.getName() : "null"));
		}

	}

}
