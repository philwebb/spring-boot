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
 * Common JSON formats.
 *
 * @author Moritz Halbritter
 * @since 3.4.0
 */
public final class CommonJsonFormats {

	private static final Map<String, JsonFormat> FORMATS = Map.of("ecs", new EcsJsonFormat(), "logstash",
			new LogstashJsonFormat());

	private CommonJsonFormats() {
	}

	public static Set<String> getSupportedFormats() {
		return FORMATS.keySet();
	}

	/**
	 * Returns the requested {@link JsonFormat}. Returns {@code null} if the format isn't
	 * known.
	 * @param format the requested format
	 * @return the requested format or{@code null} if the format isn't known.
	 */
	public static JsonFormat get(String format) {
		Assert.notNull(format, "Format must not be null");
		return FORMATS.get(format.toLowerCase(Locale.ENGLISH));
	}

	/**
	 * <a href="https://www.elastic.co/guide/en/ecs/current/ecs-log.html">ECS logging
	 * format</a>.
	 */
	private static final class EcsJsonFormat implements JsonFormat {

		@Override
		public void write(LogEvent event, JsonBuilder builder) {
			builder.attribute("@timestamp", event.getTimestamp().toString());
			builder.attribute("log.level", event.getLevel());
			if (event.getPid() != null) {
				builder.attribute("process.pid", event.getPid());
			}
			builder.attribute("process.thread.name", event.getThreadName());
			if (event.getServiceName() != null) {
				builder.attribute("service.name", event.getServiceName());
			}
			if (event.getServiceVersion() != null) {
				builder.attribute("service.version", event.getServiceVersion());
			}
			if (event.getServiceEnvironment() != null) {
				builder.attribute("service.environment", event.getServiceEnvironment());
			}
			if (event.getServiceNodeName() != null) {
				builder.attribute("service.node.name", event.getServiceNodeName());
			}
			builder.attribute("log.logger", event.getLoggerName());
			builder.attribute("message", event.getFormattedMessage());
			addMdc(event, builder);
			addKeyValuePairs(event, builder);
			if (event.hasThrowable()) {
				builder.attribute("error.type", event.getThrowableClassName());
				builder.attribute("error.message", event.getThrowableMessage());
				builder.attribute("error.stack_trace", event.getThrowableStackTraceAsString());
			}
			builder.attribute("ecs.version", "8.11");
		}

		private void addKeyValuePairs(LogEvent event, JsonBuilder builder) {
			Map<String, Object> keyValuePairs = event.getKeyValuePairs();
			if (CollectionUtils.isEmpty(keyValuePairs)) {
				return;
			}
			keyValuePairs.forEach((key, value) -> builder.attribute(key, ObjectUtils.nullSafeToString(value)));
		}

		private static void addMdc(LogEvent event, JsonBuilder builder) {
			Map<String, String> mdc = event.getMdc();
			if (CollectionUtils.isEmpty(mdc)) {
				return;
			}
			mdc.forEach(builder::attribute);
		}

	}

	private static final class LogstashJsonFormat implements JsonFormat {

		@Override
		public void write(LogEvent event, JsonBuilder builder) {
			OffsetDateTime time = OffsetDateTime.ofInstant(event.getTimestamp(), ZoneId.systemDefault());
			builder.attribute("@timestamp", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time));
			builder.attribute("@version", "1");
			builder.attribute("message", event.getFormattedMessage());
			builder.attribute("logger_name", event.getLoggerName());
			builder.attribute("thread_name", event.getThreadName());
			builder.attribute("level", event.getLevel());
			builder.attribute("level_value", event.getLevelValue());
			addMdc(event, builder);
			addKeyValuePairs(event, builder);
			addMarkers(event, builder);
			if (event.hasThrowable()) {
				builder.attribute("stack_trace", event.getThrowableStackTraceAsString());
			}
		}

		private void addMarkers(LogEvent event, JsonBuilder builder) {
			Set<String> markers = event.getMarkers();
			if (CollectionUtils.isEmpty(markers)) {
				return;
			}
			builder.attribute("tags", markers);
		}

		private void addKeyValuePairs(LogEvent event, JsonBuilder builder) {
			Map<String, Object> keyValuePairs = event.getKeyValuePairs();
			if (CollectionUtils.isEmpty(keyValuePairs)) {
				return;
			}
			keyValuePairs.forEach((key, value) -> builder.attribute(key, ObjectUtils.nullSafeToString(value)));
		}

		private static void addMdc(LogEvent event, JsonBuilder builder) {
			Map<String, String> mdc = event.getMdc();
			if (CollectionUtils.isEmpty(mdc)) {
				return;
			}
			mdc.forEach(builder::attribute);
		}

	}

}
