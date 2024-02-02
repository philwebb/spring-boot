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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.EncoderBase;
import org.slf4j.event.KeyValuePair;

import org.springframework.boot.logging.logback.JsonEncoder.JsonFormat.Context;
import org.springframework.boot.logging.logback.JsonEncoder.JsonFormat.Field;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link Encoder Logback encoder} which encodes to JSON-based formats.
 *
 * @author Moritz Halbritter
 */
class JsonEncoder extends EncoderBase<ILoggingEvent> {

	private final ThrowableProxyConverter throwableProxyConverter = new ThrowableProxyConverter();

	private final JsonFormat format;

	private final Context formatContext;

	JsonEncoder(JsonFormat format) {
		this(format, null, null, null);
	}

	JsonEncoder(JsonFormat format, Long pid, String serviceName, String serviceVersion) {
		this.format = format;
		this.formatContext = new Context(pid, serviceName, serviceVersion, this.throwableProxyConverter);
	}

	@Override
	public void start() {
		super.start();
		this.throwableProxyConverter.start();
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
		for (Field field : this.format.getFields(this.formatContext, event)) {
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

	interface JsonFormat {

		Iterable<Field> getFields(Context context, ILoggingEvent event);

		record Context(Long pid, String serviceName, String serviceVersion,
				ThrowableProxyConverter throwableProxyConverter) {
		}

		interface Field {

			void write(StringBuilder output);

		}

	}

	static final class StandardField implements JsonFormat.Field {

		private final Key key;

		private final Value value;

		private StandardField(Key key, Value value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public void write(StringBuilder output) {
			output.append('\"');
			this.key.write(output);
			output.append("\":");
			this.value.write(output);
		}

		static StandardField of(Key key, Value value) {
			return new StandardField(key, value);
		}

	}

	interface Key {

		void write(StringBuilder output);

		static Key verbatim(String key) {
			return (output) -> output.append(key);
		}

		static Key escaped(String key) {
			return (output) -> JsonHelper.escape(key, output);
		}

	}

	interface Value {

		void write(StringBuilder output);

		static Value verbatim(String value) {
			return (output) -> output.append('\"').append(value).append('\"');
		}

		static Value escaped(String value) {
			return (output) -> {
				output.append('\"');
				JsonHelper.escape(value, output);
				output.append('\"');
			};
		}

		static Value of(long value) {
			return (output) -> output.append(value);
		}

	}

	private static final class JsonHelper {

		private JsonHelper() {
		}

		static void escape(String text, StringBuilder output) {
			for (int i = 0; i < text.length(); i++) {
				char c = text.charAt(i);
				escape(c, output);
			}
		}

		static void escape(char c, StringBuilder output) {
			// TODO: More JSON \\u escaping, see
			// co.elastic.logging.JsonUtils#quoteAsString(java.lang.CharSequence, int,
			// int,
			// java.lang.StringBuilder)
			switch (c) {
				case '\b' -> output.append("\\b");
				case '\t' -> output.append("\\t");
				case '\f' -> output.append("\\f");
				case '\n' -> output.append("\\n");
				case '\r' -> output.append("\\r");
				case '"' -> output.append("\\\"");
				case '\\' -> output.append("\\\\");
				default -> output.append(c);
			}
		}

	}

	enum CommonFormats implements JsonFormat {

		ECS {
			@Override
			public Iterable<Field> getFields(Context context, ILoggingEvent event) {
				List<Field> fields = new ArrayList<>();
				fields.add(StandardField.of(Key.verbatim("@timestamp"), Value.verbatim(event.getInstant().toString())));
				fields.add(StandardField.of(Key.verbatim("log.level"), Value.verbatim(event.getLevel().toString())));
				if (context.pid() != null) {
					fields.add(StandardField.of(Key.verbatim("process.pid"), Value.of(context.pid())));
				}
				fields.add(StandardField.of(Key.verbatim("process.thread.name"), Value.escaped(event.getThreadName())));
				if (context.serviceName() != null) {
					fields.add(StandardField.of(Key.verbatim("service.name"), Value.escaped(context.serviceName())));
				}
				if (context.serviceVersion() != null) {
					fields.add(
							StandardField.of(Key.verbatim("service.version"), Value.escaped(context.serviceVersion())));
				}
				fields.add(StandardField.of(Key.verbatim("log.logger"), Value.escaped(event.getLoggerName())));
				fields.add(StandardField.of(Key.verbatim("message"), Value.escaped(event.getFormattedMessage())));
				addMdc(event, fields);
				addKeyValuePairs(event, fields);
				IThrowableProxy throwable = event.getThrowableProxy();
				if (throwable != null) {
					fields.add(StandardField.of(Key.verbatim("error.type"), Value.verbatim(throwable.getClassName())));
					fields.add(StandardField.of(Key.verbatim("error.message"), Value.escaped(throwable.getMessage())));
					fields.add(StandardField.of(Key.verbatim("error.stack_trace"),
							Value.escaped(context.throwableProxyConverter().convert(event))));
				}
				fields.add(StandardField.of(Key.verbatim("ecs.version"), Value.verbatim("1.2.0")));
				return fields;
				// TODO: Service name, service version, service env, service node name,
				// event dataset
			}

			private void addKeyValuePairs(ILoggingEvent event, List<Field> fields) {
				List<KeyValuePair> keyValuePairs = event.getKeyValuePairs();
				if (CollectionUtils.isEmpty(keyValuePairs)) {
					return;
				}
				for (KeyValuePair keyValuePair : keyValuePairs) {
					fields.add(StandardField.of(Key.escaped(keyValuePair.key),
							Value.escaped(ObjectUtils.nullSafeToString(keyValuePair.value))));
				}
			}

			private static void addMdc(ILoggingEvent event, List<Field> fields) {
				Map<String, String> mdc = event.getMDCPropertyMap();
				if (CollectionUtils.isEmpty(mdc)) {
					return;
				}
				for (Entry<String, String> entry : mdc.entrySet()) {
					fields.add(StandardField.of(Key.escaped(entry.getKey()), Value.escaped(entry.getValue())));
				}
			}
		};

		static JsonFormat parse(String input) {
			for (CommonFormats value : values()) {
				if (value.name().equalsIgnoreCase(input)) {
					return value;
				}
			}
			throw new IllegalArgumentException(
					"Unknown format '%s'. Known formats: %s".formatted(input, Arrays.toString(values())));
		}

	}

}
