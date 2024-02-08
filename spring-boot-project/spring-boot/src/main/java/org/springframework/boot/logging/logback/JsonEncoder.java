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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.EncoderBase;
import org.slf4j.event.KeyValuePair;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.logging.json.Field;
import org.springframework.boot.logging.json.JsonFormat;
import org.springframework.boot.logging.json.Key;
import org.springframework.boot.logging.json.Value;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link Encoder Logback encoder} which encodes to JSON-based formats.
 *
 * @author Moritz Halbritter
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

	public JsonEncoder(LogbackJsonFormat format, Long pid, String serviceName, String serviceVersion, String serviceNodeName, String serviceEnvironment) {
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
		if (this.pid != null) {
			this.format.setPid(this.pid);
		}
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

	public interface LogbackJsonFormat extends JsonFormat<ILoggingEvent> {
		void setThrowableProxyConverter(ThrowableProxyConverter throwableProxyConverter);
	}

	static abstract class BaseLogbackJsonFormat implements LogbackJsonFormat {

		private Long pid = null;

		private String serviceName;

		private String serviceVersion;

		private ThrowableProxyConverter throwableProxyConverter;

		private String serviceNodeName;

		private String serviceEnvironment;

		@Override
		public void setPid(long pid) {
			this.pid = pid;
		}

		@Override
		public void setServiceName(String serviceName) {
			this.serviceName = serviceName;
		}

		@Override
		public void setServiceVersion(String serviceVersion) {
			this.serviceVersion = serviceVersion;
		}

		@Override
		public void setThrowableProxyConverter(ThrowableProxyConverter throwableProxyConverter) {
			this.throwableProxyConverter = throwableProxyConverter;
		}

		@Override
		public void setServiceNodeName(String serviceNodeName) {
			this.serviceNodeName = serviceNodeName;
		}

		@Override
		public void setServiceEnvironment(String serviceEnvironment) {
			this.serviceEnvironment = serviceEnvironment;
		}

		Long getPid() {
			return this.pid;
		}

		String getServiceName() {
			return this.serviceName;
		}

		String getServiceVersion() {
			return this.serviceVersion;
		}

		ThrowableProxyConverter getThrowableProxyConverter() {
			return this.throwableProxyConverter;
		}

		String getServiceNodeName() {
			return this.serviceNodeName;
		}

		String getServiceEnvironment() {
			return this.serviceEnvironment;
		}
	}

	static final class CommonJsonFormats {

		private static final Map<String, Supplier<LogbackJsonFormat>> FORMATS = Map.of("ecs", CommonJsonFormats::ecs);

		static EcsJsonFormat ecs() {
			return new EcsJsonFormat();
		}

		static Set<String> names() {
			return FORMATS.keySet();
		}

		/**
		 * Returns a new instance of the requested {@link LogbackJsonFormat}. Returns
		 * {@code null} if the format isn't known.
		 * @param format the requested format
		 * @return a new instance of the request format or{@code null} if the format isn't
		 * known.
		 */
		static LogbackJsonFormat create(String format) {
			Assert.notNull(format, "Format must not be null");
			return FORMATS.get(format.toLowerCase()).get();
		}

		private static final class EcsJsonFormat extends BaseLogbackJsonFormat {

			@Override
			public Iterable<Field> getFields(ILoggingEvent event) {
				List<Field> fields = new ArrayList<>();
				fields.add(Field.of(Key.verbatim("@timestamp"), Value.verbatim(event.getInstant().toString())));
				fields.add(Field.of(Key.verbatim("log.level"), Value.verbatim(event.getLevel().toString())));
				if (getPid() != null) {
					fields.add(Field.of(Key.verbatim("process.pid"), Value.of(getPid())));
				}
				fields.add(Field.of(Key.verbatim("process.thread.name"), Value.escaped(event.getThreadName())));
				if (getServiceName() != null) {
					fields.add(Field.of(Key.verbatim("service.name"), Value.escaped(getServiceName())));
				}
				if (getServiceVersion() != null) {
					fields.add(Field.of(Key.verbatim("service.version"), Value.escaped(getServiceVersion())));
				}
				if (getServiceEnvironment() != null) {
					fields.add(Field.of(Key.verbatim("service.environment"), Value.escaped(getServiceEnvironment())));
				}
				if (getServiceNodeName() != null) {
					fields.add(Field.of(Key.verbatim("service.node.name"), Value.escaped(getServiceNodeName())));
				}
				fields.add(Field.of(Key.verbatim("log.logger"), Value.escaped(event.getLoggerName())));
				fields.add(Field.of(Key.verbatim("message"), Value.escaped(event.getFormattedMessage())));
				addMdc(event, fields);
				addKeyValuePairs(event, fields);
				IThrowableProxy throwable = event.getThrowableProxy();
				if (throwable != null) {
					fields.add(Field.of(Key.verbatim("error.type"), Value.verbatim(throwable.getClassName())));
					fields.add(Field.of(Key.verbatim("error.message"), Value.escaped(throwable.getMessage())));
					fields.add(Field.of(Key.verbatim("error.stack_trace"),
							Value.escaped(getThrowableProxyConverter().convert(event))));
				}
				fields.add(Field.of(Key.verbatim("ecs.version"), Value.verbatim("8.11")));
				return fields;
				// TODO: service env, service node name,
				// event dataset
			}

			private void addKeyValuePairs(ILoggingEvent event, List<Field> fields) {
				List<KeyValuePair> keyValuePairs = event.getKeyValuePairs();
				if (CollectionUtils.isEmpty(keyValuePairs)) {
					return;
				}
				for (KeyValuePair keyValuePair : keyValuePairs) {
					fields.add(Field.of(Key.escaped(keyValuePair.key),
							Value.escaped(ObjectUtils.nullSafeToString(keyValuePair.value))));
				}
			}

			private static void addMdc(ILoggingEvent event, List<Field> fields) {
				Map<String, String> mdc = event.getMDCPropertyMap();
				if (CollectionUtils.isEmpty(mdc)) {
					return;
				}
				for (Entry<String, String> entry : mdc.entrySet()) {
					fields.add(Field.of(Key.escaped(entry.getKey()), Value.escaped(entry.getValue())));
				}
			}

		}

	}

}
