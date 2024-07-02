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

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingFormatter;

/**
 * Logstash logging format.
 *
 * @author Moritz Halbritter
 */
class Log4j2LogstashStructuredLoggingFormatter implements StructuredLoggingFormatter<LogEvent> {

	private JsonWriter<LogEvent> writer;

	Log4j2LogstashStructuredLoggingFormatter() {
		this.writer = JsonWriter.of(this::logEventJson);
	}

	private void logEventJson(JsonWriter.Members<LogEvent> members) {
		members.add("@timestamp", LogEvent::getInstant).as(this::asTimestamp);
		members.add("@version", "1");
		members.add("message", LogEvent::getMessage).as(Message::getFormattedMessage);
		members.add("logger_name", LogEvent::getLoggerName);
		members.add("thread_name", LogEvent::getThreadName);
		members.add("level", LogEvent::getLevel).as(Level::name);
		members.add("level_value", LogEvent::getLevel).as(Level::intLevel);
		members.add(LogEvent::getContextData)
			.whenNot(ReadOnlyStringMap::isEmpty)
			.asWrittenJson(contextJsonDataWriter());
		members.add("tags", LogEvent::getMarker).whenNotNull().as(this::getMarkers);
		members.add("stack_trace", LogEvent::getThrownProxy)
			.whenNotNull()
			.as(ThrowableProxy::getExtendedStackTraceAsString);
	}

	private String asTimestamp(Instant instant) {
		java.time.Instant javaInstant = java.time.Instant.ofEpochMilli(instant.getEpochMillisecond())
			.plusNanos(instant.getNanoOfMillisecond());
		OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(javaInstant, ZoneId.systemDefault());
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime);
	}

	private JsonWriter<ReadOnlyStringMap> contextJsonDataWriter() {
		return JsonWriter.using((contextData, memberWriter) -> contextData.forEach(memberWriter::write));
	}

	private Set<String> getMarkers(Marker marker) {
		Set<String> result = new HashSet<>();
		addMarkers(result, marker);
		return result;
	}

	private void addMarkers(Set<String> result, Marker marker) {
		result.add(marker.getName());
		if (marker.hasParents()) {
			for (Marker parent : marker.getParents()) {
				addMarkers(result, parent);
			}
		}
	}

	@Override
	public String format(LogEvent event) {
		return this.writer.write(event).toStringWithNewLine();
	}

}
