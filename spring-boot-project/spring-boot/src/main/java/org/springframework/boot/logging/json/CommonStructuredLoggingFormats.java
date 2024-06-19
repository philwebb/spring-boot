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

package org.springframework.boot.logging.json;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Common structured logging formats.
 *
 * @author Moritz Halbritter
 * @since 3.4.0
 */
public final class CommonStructuredLoggingFormats {

	private static final Map<String, StructuredLoggingFormat> FORMATS = Map.of("ecs", new EcsJsonFormat(), "logstash",
			new LogstashJsonFormat(), "logfmt", new LogfmtStructuredLoggingFormat());

	private CommonStructuredLoggingFormats() {
	}

	/**
	 * Returns the supported formats.
	 * @return the supported formats
	 */
	public static Set<String> getSupportedFormats() {
		return FORMATS.keySet();
	}

	/**
	 * Returns the requested {@link StructuredLoggingFormat}. Returns {@code null} if the
	 * format isn't known.
	 * @param format the requested format
	 * @return the requested format or{@code null} if the format isn't known.
	 */
	public static StructuredLoggingFormat get(String format) {
		Assert.notNull(format, "Format must not be null");
		return FORMATS.get(format.toLowerCase(Locale.ENGLISH));
	}

	/**
	 * <a href="https://www.elastic.co/guide/en/ecs/current/ecs-log.html">ECS logging
	 * format</a>.
	 */
	private static final class EcsJsonFormat implements StructuredLoggingFormat {

		@Override
		public JsonWriter createWriter(StringBuilder stringBuilder) {
			return new JsonWriter(stringBuilder);
		}

		@Override
		public void write(LogEvent event, StructuredLoggingWriter builder) {
			if (!(builder instanceof JsonWriter jsonBuilder)) {
				throw new IllegalArgumentException("Builder is not a JsonBuilder");
			}
			jsonBuilder.objectStart();
			jsonBuilder.attribute("@timestamp", event.getTimestamp().toString());
			jsonBuilder.attribute("log.level", event.getLevel());
			if (event.getPid() != null) {
				jsonBuilder.attribute("process.pid", event.getPid());
			}
			jsonBuilder.attribute("process.thread.name", event.getThreadName());
			if (event.getServiceName() != null) {
				jsonBuilder.attribute("service.name", event.getServiceName());
			}
			if (event.getServiceVersion() != null) {
				jsonBuilder.attribute("service.version", event.getServiceVersion());
			}
			if (event.getServiceEnvironment() != null) {
				jsonBuilder.attribute("service.environment", event.getServiceEnvironment());
			}
			if (event.getServiceNodeName() != null) {
				jsonBuilder.attribute("service.node.name", event.getServiceNodeName());
			}
			jsonBuilder.attribute("log.logger", event.getLoggerName());
			jsonBuilder.attribute("message", event.getFormattedMessage());
			addMdc(event, jsonBuilder);
			addKeyValuePairs(event, jsonBuilder);
			if (event.hasThrowable()) {
				jsonBuilder.attribute("error.type", event.getThrowableClassName());
				jsonBuilder.attribute("error.message", event.getThrowableMessage());
				jsonBuilder.attribute("error.stack_trace", event.getThrowableStackTraceAsString());
			}
			jsonBuilder.attribute("ecs.version", "8.11");
			jsonBuilder.objectEnd();
		}

		private void addKeyValuePairs(LogEvent event, JsonWriter jsonBuilder) {
			Map<String, Object> keyValuePairs = event.getKeyValuePairs();
			if (CollectionUtils.isEmpty(keyValuePairs)) {
				return;
			}
			keyValuePairs.forEach((key, value) -> jsonBuilder.attribute(key, ObjectUtils.nullSafeToString(value)));
		}

		private static void addMdc(LogEvent event, JsonWriter jsonBuilder) {
			Map<String, String> mdc = event.getMdc();
			if (CollectionUtils.isEmpty(mdc)) {
				return;
			}
			mdc.forEach(jsonBuilder::attribute);
		}

	}

	private static final class LogstashJsonFormat implements StructuredLoggingFormat {

		@Override
		public JsonWriter createWriter(StringBuilder stringBuilder) {
			return new JsonWriter(stringBuilder);
		}

		@Override
		public void write(LogEvent event, StructuredLoggingWriter builder) {
			if (!(builder instanceof JsonWriter jsonBuilder)) {
				throw new IllegalArgumentException("Builder is not a JsonBuilder");
			}
			jsonBuilder.objectStart();
			OffsetDateTime time = OffsetDateTime.ofInstant(event.getTimestamp(), ZoneId.systemDefault());
			jsonBuilder.attribute("@timestamp", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time));
			jsonBuilder.attribute("@version", "1");
			jsonBuilder.attribute("message", event.getFormattedMessage());
			jsonBuilder.attribute("logger_name", event.getLoggerName());
			jsonBuilder.attribute("thread_name", event.getThreadName());
			jsonBuilder.attribute("level", event.getLevel());
			jsonBuilder.attribute("level_value", event.getLevelValue());
			addMdc(event, jsonBuilder);
			addKeyValuePairs(event, jsonBuilder);
			addMarkers(event, jsonBuilder);
			if (event.hasThrowable()) {
				jsonBuilder.attribute("stack_trace", event.getThrowableStackTraceAsString());
			}
			jsonBuilder.objectEnd();
		}

		private void addMarkers(LogEvent event, JsonWriter jsonBuilder) {
			Set<String> markers = event.getMarkers();
			if (CollectionUtils.isEmpty(markers)) {
				return;
			}
			jsonBuilder.attribute("tags", markers);
		}

		private void addKeyValuePairs(LogEvent event, JsonWriter jsonBuilder) {
			Map<String, Object> keyValuePairs = event.getKeyValuePairs();
			if (CollectionUtils.isEmpty(keyValuePairs)) {
				return;
			}
			keyValuePairs.forEach((key, value) -> jsonBuilder.attribute(key, ObjectUtils.nullSafeToString(value)));
		}

		private static void addMdc(LogEvent event, JsonWriter jsonBuilder) {
			Map<String, String> mdc = event.getMdc();
			if (CollectionUtils.isEmpty(mdc)) {
				return;
			}
			mdc.forEach(jsonBuilder::attribute);
		}

	}

	private static final class LogfmtStructuredLoggingFormat implements StructuredLoggingFormat {

		@Override
		public KeyValueWriter createWriter(StringBuilder stringBuilder) {
			return new KeyValueWriter(stringBuilder);
		}

		@Override
		public void write(LogEvent event, StructuredLoggingWriter builder) {
			if (!(builder instanceof KeyValueWriter keyValueBuilder)) {
				throw new IllegalArgumentException("Builder is not a KeyValueBuilder");
			}
			OffsetDateTime time = OffsetDateTime.ofInstant(event.getTimestamp(), ZoneId.systemDefault());
			keyValueBuilder.attribute("time", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time));
			keyValueBuilder.attribute("level", event.getLevel());
			keyValueBuilder.attribute("msg", event.getFormattedMessage());
			keyValueBuilder.attribute("logger", event.getLoggerName());
			addMdc(event, keyValueBuilder);
			addKeyValuePairs(event, keyValueBuilder);
			if (event.hasThrowable()) {
				keyValueBuilder.attribute("exception_class", event.getThrowableClassName());
				keyValueBuilder.attribute("exception_msg", event.getThrowableMessage());
				keyValueBuilder.attribute("error", event.getThrowableStackTraceAsString());
			}
		}

		private void addKeyValuePairs(LogEvent event, KeyValueWriter keyValueBuilder) {
			Map<String, Object> keyValuePairs = event.getKeyValuePairs();
			if (CollectionUtils.isEmpty(keyValuePairs)) {
				return;
			}
			keyValuePairs.forEach((key, value) -> keyValueBuilder.attribute(key, ObjectUtils.nullSafeToString(value)));
		}

		private static void addMdc(LogEvent event, KeyValueWriter keyValueBuilder) {
			Map<String, String> mdc = event.getMdc();
			if (CollectionUtils.isEmpty(mdc)) {
				return;
			}
			mdc.forEach(keyValueBuilder::attribute);
		}

	}

}
