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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import org.springframework.boot.logging.structured.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingFormatter;
import org.springframework.util.CollectionUtils;

/**
 * Logstash logging format.
 *
 * @author Moritz Halbritter
 */
class Log4j2LogstashStructuredLoggingFormatter implements StructuredLoggingFormatter<LogEvent> {

	@Override
	public String format(LogEvent event) {
		JsonWriter writer = new JsonWriter();
		writer.object(() -> {
			Instant instant = Instant.ofEpochMilli(event.getInstant().getEpochMillisecond())
				.plusNanos(event.getInstant().getNanoOfMillisecond());
			OffsetDateTime time = OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
			writer.stringMember("@timestamp", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time));
			writer.stringMember("@version", "1");
			writer.stringMember("message", event.getMessage().getFormattedMessage());
			writer.stringMember("logger_name", event.getLoggerName());
			writer.stringMember("thread_name", event.getThreadName());
			writer.stringMember("level", event.getLevel().name());
			writer.numberMember("level_value", event.getLevel().intLevel());
			addMdc(event, writer);
			addMarkers(event, writer);
			ThrowableProxy throwable = event.getThrownProxy();
			if (throwable != null) {
				writer.stringMember("stack_trace", throwable.getExtendedStackTraceAsString());
			}
		});
		writer.newLine();
		return writer.toJson();
	}

	private static void addMdc(LogEvent event, JsonWriter writer) {
		ReadOnlyStringMap contextData = event.getContextData();
		if (contextData == null) {
			return;
		}
		Map<String, String> mdc = contextData.toMap();
		if (CollectionUtils.isEmpty(mdc)) {
			return;
		}
		mdc.forEach(writer::stringMember);
	}

	private void addMarkers(LogEvent event, JsonWriter writer) {
		Set<String> markers = getMarkers(event);
		if (CollectionUtils.isEmpty(markers)) {
			return;
		}
		writer.member("tags", () -> writer.stringArray(markers));
	}

	private Set<String> getMarkers(LogEvent event) {
		if (event.getMarker() == null) {
			return Collections.emptySet();
		}
		Set<String> result = new HashSet<>();
		addMarker(result, event.getMarker());
		return result;
	}

	private void addMarker(Set<String> result, org.apache.logging.log4j.Marker marker) {
		if (marker == null) {
			return;
		}
		result.add(marker.getName());
		if (marker.hasParents()) {
			for (org.apache.logging.log4j.Marker parent : marker.getParents()) {
				addMarker(result, parent);
			}
		}
	}

}
