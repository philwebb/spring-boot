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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingFormatter;

/**
 * Logstash logging format.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class LogbackLogstashStructuredLoggingFormatter implements StructuredLoggingFormatter<ILoggingEvent> {

	private JsonWriter<ILoggingEvent> writer;

	LogbackLogstashStructuredLoggingFormatter(ThrowableProxyConverter throwableProxyConverter) {
		this.writer = JsonWriter.<ILoggingEvent>of((members) -> loggingEventJson(throwableProxyConverter, members))
			.endingWithNewLine();
	}

	private void loggingEventJson(ThrowableProxyConverter throwableProxyConverter,
			JsonWriter.Members<ILoggingEvent> members) {
		members.add("@timestamp", ILoggingEvent::getInstant).as(this::asTimestamp);
		members.add("@version", "1");
		members.add("message", ILoggingEvent::getFormattedMessage);
		members.add("logger_name", ILoggingEvent::getLoggerName);
		members.add("thread_name", ILoggingEvent::getThreadName);
		members.add("level", ILoggingEvent::getLevel);
		members.add("level_value", ILoggingEvent::getLevel).as(Level::toInt);
		members.add(ILoggingEvent::getMDCPropertyMap).whenNotEmpty();
		members.add(ILoggingEvent::getKeyValuePairs)
			.whenNotEmpty()
			.usingElements(Iterable::forEach, KeyValuePair.class, (pair) -> pair.key, (pair) -> pair.value);
		members.add("tags", ILoggingEvent::getMarkerList).whenNotNull().as(this::getMarkers).whenNotEmpty();
		members.add("stack_trace", (event) -> event)
			.whenNotNull(ILoggingEvent::getThrowableProxy)
			.as(throwableProxyConverter::convert);
	}

	private String asTimestamp(Instant instant) {
		OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime);
	}

	private Set<String> getMarkers(List<Marker> markers) {
		Set<String> result = new LinkedHashSet<>();
		addMarkers(result, markers.iterator());
		return result;
	}

	private void addMarkers(Set<String> result, Iterator<Marker> iterator) {
		while (iterator.hasNext()) {
			Marker marker = iterator.next();
			result.add(marker.getName());
			if (marker.hasReferences()) {
				addMarkers(result, marker.iterator());
			}
		}
	}

	@Override
	public String format(ILoggingEvent event) {
		return this.writer.writeToString(event);
	}

}
