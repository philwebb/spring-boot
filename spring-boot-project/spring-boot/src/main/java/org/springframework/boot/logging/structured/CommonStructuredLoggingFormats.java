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

package org.springframework.boot.logging.structured;

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
		public String format(LogEvent event) {
			JsonWriter writer = new JsonWriter();
			writer.objectStart();
			writer.attribute("@timestamp", event.getTimestamp().toString());
			writer.attribute("log.level", event.getLevel());
			if (event.getPid() != null) {
				writer.attribute("process.pid", event.getPid());
			}
			writer.attribute("process.thread.name", event.getThreadName());
			if (event.getServiceName() != null) {
				writer.attribute("service.name", event.getServiceName());
			}
			if (event.getServiceVersion() != null) {
				writer.attribute("service.version", event.getServiceVersion());
			}
			if (event.getServiceEnvironment() != null) {
				writer.attribute("service.environment", event.getServiceEnvironment());
			}
			if (event.getServiceNodeName() != null) {
				writer.attribute("service.node.name", event.getServiceNodeName());
			}
			writer.attribute("log.logger", event.getLoggerName());
			writer.attribute("message", event.getFormattedMessage());
			addMdc(event, writer);
			addKeyValuePairs(event, writer);
			if (event.hasThrowable()) {
				writer.attribute("error.type", event.getThrowableClassName());
				writer.attribute("error.message", event.getThrowableMessage());
				writer.attribute("error.stack_trace", event.getThrowableStackTraceAsString());
			}
			writer.attribute("ecs.version", "8.11");
			writer.objectEnd();
			writer.newLine();
			return writer.finish();
		}

		private void addKeyValuePairs(LogEvent event, JsonWriter writer) {
			Map<String, Object> keyValuePairs = event.getKeyValuePairs();
			if (CollectionUtils.isEmpty(keyValuePairs)) {
				return;
			}
			keyValuePairs.forEach((key, value) -> writer.attribute(key, ObjectUtils.nullSafeToString(value)));
		}

		private static void addMdc(LogEvent event, JsonWriter writer) {
			Map<String, String> mdc = event.getMdc();
			if (CollectionUtils.isEmpty(mdc)) {
				return;
			}
			mdc.forEach(writer::attribute);
		}

	}

	private static final class LogstashJsonFormat implements StructuredLoggingFormat {

		@Override
		public String format(LogEvent event) {
			JsonWriter writer = new JsonWriter();
			writer.objectStart();
			OffsetDateTime time = OffsetDateTime.ofInstant(event.getTimestamp(), ZoneId.systemDefault());
			writer.attribute("@timestamp", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time));
			writer.attribute("@version", "1");
			writer.attribute("message", event.getFormattedMessage());
			writer.attribute("logger_name", event.getLoggerName());
			writer.attribute("thread_name", event.getThreadName());
			writer.attribute("level", event.getLevel());
			writer.attribute("level_value", event.getLevelValue());
			addMdc(event, writer);
			addKeyValuePairs(event, writer);
			addMarkers(event, writer);
			if (event.hasThrowable()) {
				writer.attribute("stack_trace", event.getThrowableStackTraceAsString());
			}
			writer.objectEnd();
			writer.newLine();
			return writer.finish();
		}

		private void addMarkers(LogEvent event, JsonWriter writer) {
			Set<String> markers = event.getMarkers();
			if (CollectionUtils.isEmpty(markers)) {
				return;
			}
			writer.attribute("tags", markers);
		}

		private void addKeyValuePairs(LogEvent event, JsonWriter writer) {
			Map<String, Object> keyValuePairs = event.getKeyValuePairs();
			if (CollectionUtils.isEmpty(keyValuePairs)) {
				return;
			}
			keyValuePairs.forEach((key, value) -> writer.attribute(key, ObjectUtils.nullSafeToString(value)));
		}

		private static void addMdc(LogEvent event, JsonWriter writer) {
			Map<String, String> mdc = event.getMdc();
			if (CollectionUtils.isEmpty(mdc)) {
				return;
			}
			mdc.forEach(writer::attribute);
		}

	}

	private static final class LogfmtStructuredLoggingFormat implements StructuredLoggingFormat {

		@Override
		public String format(LogEvent event) {
			KeyValueWriter writer = new KeyValueWriter();
			OffsetDateTime time = OffsetDateTime.ofInstant(event.getTimestamp(), ZoneId.systemDefault());
			writer.attribute("time", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time));
			writer.attribute("level", event.getLevel());
			writer.attribute("msg", event.getFormattedMessage());
			writer.attribute("logger", event.getLoggerName());
			addMdc(event, writer);
			addKeyValuePairs(event, writer);
			if (event.hasThrowable()) {
				writer.attribute("exception_class", event.getThrowableClassName());
				writer.attribute("exception_msg", event.getThrowableMessage());
				writer.attribute("error", event.getThrowableStackTraceAsString());
			}
			writer.newLine();
			return writer.finish();
		}

		private void addKeyValuePairs(LogEvent event, KeyValueWriter writer) {
			Map<String, Object> keyValuePairs = event.getKeyValuePairs();
			if (CollectionUtils.isEmpty(keyValuePairs)) {
				return;
			}
			keyValuePairs.forEach((key, value) -> writer.attribute(key, ObjectUtils.nullSafeToString(value)));
		}

		private static void addMdc(LogEvent event, KeyValueWriter writer) {
			Map<String, String> mdc = event.getMdc();
			if (CollectionUtils.isEmpty(mdc)) {
				return;
			}
			mdc.forEach(writer::attribute);
		}

	}

}
