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
import java.time.Instant;
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

import org.springframework.boot.logging.logback.JsonEncoder.Format.Context;
import org.springframework.boot.logging.logback.JsonEncoder.Format.Field;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link Encoder Logback encoder} which encodes to JSON-based formats.
 *
 * @author Moritz Halbritter
 */
class JsonEncoder extends EncoderBase<ILoggingEvent> {

	private final ThrowableProxyConverter throwableProxyConverter = new ThrowableProxyConverter();

	private final Format format;

	private final Context formatContext;

	JsonEncoder(Format format) {
		this.format = format;
		this.formatContext = new Context(this.throwableProxyConverter);
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
			writeField(field, output);
		}
		removeTrailingComma(output);
		output.append('}');
		output.append('\n');
		return output.toString().getBytes(StandardCharsets.UTF_8);
	}

	private void writeField(Field field, StringBuilder output) {
		if (field.escapeKey()) {
			writeKey(field.key(), output);
		}
		else {
			writeVerbatimKey(field.key(), output);
		}
		output.append(':');
		if (field.escapeValue()) {
			writeValue(field.value(), output);
		}
		else {
			writeVerbatimValue(field.value(), output);
		}
		output.append(',');
	}

	private void writeValue(String value, StringBuilder output) {
		output.append('\"');
		if (value == null) {
			output.append("null");
		}
		else {
			escape(value, output);
		}
		output.append("\"");
	}

	private void writeVerbatimValue(String value, StringBuilder output) {
		output.append('\"');
		output.append(value);
		output.append('\"');
	}

	private void writeKey(String key, StringBuilder output) {
		output.append('\"');
		if (key == null) {
			output.append("null");
		}
		else {
			escape(key, output);
		}
		output.append("\"");
	}

	private void writeVerbatimKey(String key, StringBuilder output) {
		output.append('\"');
		output.append(key);
		output.append('\"');
	}

	private void removeTrailingComma(StringBuilder output) {
		int length = output.length();
		char end = output.charAt(length - 1);
		if (end == ',') {
			output.setLength(length - 1);
		}
	}

	private void escape(String text, StringBuilder output) {
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			escape(c, output);
		}
	}

	private void escape(char c, StringBuilder output) {
		// TODO: More JSON \\u escaping, see
		// co.elastic.logging.JsonUtils#quoteAsString(java.lang.CharSequence, int, int,
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

	@Override
	public byte[] footerBytes() {
		return null;
	}

	interface Format {

		Iterable<Field> getFields(Context context, ILoggingEvent event);

		record Field(String key, String value, boolean escapeKey, boolean escapeValue) {
			static Field escaped(String key, String value) {
				return new Field(key, value, true, true);
			}

			static Field escapedValue(String key, String value) {
				return new Field(key, value, false, true);
			}

			static Field verbatim(String key, String value) {
				return new Field(key, value, false, false);
			}
		}

		record Context(ThrowableProxyConverter throwableProxyConverter) {
		}

	}

	enum CommonFormats implements Format {

		ECS {
			@Override
			public Iterable<Field> getFields(Context context, ILoggingEvent event) {
				List<Field> fields = new ArrayList<>();
				fields.add(Field.verbatim("@timestamp", Instant.ofEpochMilli(event.getTimeStamp()).toString()));
				fields.add(Field.verbatim("log.level", event.getLevel().toString()));
				fields.add(Field.escapedValue("message", event.getFormattedMessage()));
				fields.add(Field.verbatim("ecs.version", "1.2.0"));
				fields.add(Field.escapedValue("process.thread.name", event.getThreadName()));
				fields.add(Field.escapedValue("log.logger", event.getLoggerName()));
				addMdc(event, fields);
				addKeyValuePairs(event, fields);
				IThrowableProxy throwable = event.getThrowableProxy();
				if (throwable != null) {
					fields.add(Field.verbatim("error.type", throwable.getClassName()));
					fields.add(Field.escapedValue("error.message", throwable.getMessage()));
					fields
						.add(Field.escapedValue("error.stack_trace", context.throwableProxyConverter().convert(event)));
				}
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
					fields.add(Field.escaped(keyValuePair.key, ObjectUtils.nullSafeToString(keyValuePair.value)));
				}
			}

			private static void addMdc(ILoggingEvent event, List<Field> fields) {
				Map<String, String> mdc = event.getMDCPropertyMap();
				if (CollectionUtils.isEmpty(mdc)) {
					return;
				}
				for (Entry<String, String> entry : mdc.entrySet()) {
					fields.add(Field.escaped(entry.getKey(), entry.getValue()));
				}
			}
		};

		static Format parse(String input) {
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
