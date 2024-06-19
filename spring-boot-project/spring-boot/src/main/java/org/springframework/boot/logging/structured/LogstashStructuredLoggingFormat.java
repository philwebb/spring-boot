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
import java.util.Map;
import java.util.Set;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Logstash logging format.
 *
 * @author Moritz Halbritter
 */
class LogstashStructuredLoggingFormat implements StructuredLoggingFormat {

	@Override
	public String getId() {
		return "logstash";
	}

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
