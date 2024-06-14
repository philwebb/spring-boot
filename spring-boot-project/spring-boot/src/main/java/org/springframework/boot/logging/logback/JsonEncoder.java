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

import java.nio.charset.StandardCharsets;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.EncoderBase;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.logging.json.Field;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link Encoder Logback encoder} which encodes to JSON-based formats.
 *
 * @author Moritz Halbritter
 * @since 3.4.0
 */
public class JsonEncoder extends EncoderBase<ILoggingEvent> {

	private final ThrowableProxyConverter throwableProxyConverter = new ThrowableProxyConverter();

	private LogbackJsonFormat format;

	private Long pid;

	private String serviceName;

	private String serviceVersion;

	private String serviceNodeName;

	private String serviceEnvironment;

	public JsonEncoder() {
		// Constructor needed for Logback XML configuration
		this(null);
	}

	public JsonEncoder(LogbackJsonFormat format) {
		this(format, null, null, null, null, null);
	}

	public JsonEncoder(LogbackJsonFormat format, Long pid, String serviceName, String serviceVersion,
			String serviceNodeName, String serviceEnvironment) {
		this.format = format;
		this.pid = pid;
		this.serviceName = serviceName;
		this.serviceVersion = serviceVersion;
		this.serviceNodeName = serviceNodeName;
		this.serviceEnvironment = serviceEnvironment;
	}

	/**
	 * Sets the format. Accepts either a common format ID, or a fully qualified class
	 * name.
	 * @param format the format
	 */
	public void setFormat(String format) {
		LogbackJsonFormat commonFormat = CommonJsonFormats.create(format);
		if (commonFormat != null) {
			this.format = commonFormat;
		}
		else if (ClassUtils.isPresent(format, null)) {
			this.format = BeanUtils.instantiateClass(ClassUtils.resolveClassName(format, null),
					LogbackJsonFormat.class);
		}
		else {
			throw new IllegalArgumentException(
					"Unknown format '%s'. Common formats are: %s".formatted(format, CommonJsonFormats.names()));
		}
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

	@Override
	public void start() {
		Assert.state(this.format != null, "Format has not been set");
		super.start();
		this.throwableProxyConverter.start();
		this.format.setPid(this.pid);
		this.format.setServiceName(this.serviceName);
		this.format.setServiceVersion(this.serviceVersion);
		this.format.setServiceEnvironment(this.serviceEnvironment);
		this.format.setServiceNodeName(this.serviceNodeName);
		this.format.setThrowableProxyConverter(this.throwableProxyConverter);
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
		StringBuilder output = new StringBuilder();
		output.append('{');
		for (Field field : this.format.getFields(event)) {
			field.write(output);
			output.append(',');
		}
		removeTrailingComma(output);
		output.append('}');
		output.append('\n');
		return output.toString().getBytes(StandardCharsets.UTF_8);
	}

	private void removeTrailingComma(StringBuilder output) {
		int length = output.length();
		char end = output.charAt(length - 1);
		if (end == ',') {
			output.setLength(length - 1);
		}
	}

	@Override
	public byte[] footerBytes() {
		return null;
	}

}
