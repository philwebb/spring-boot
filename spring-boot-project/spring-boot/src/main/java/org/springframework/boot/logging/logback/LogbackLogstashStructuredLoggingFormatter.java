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

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingFormatter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Logstash logging format.
 *
 * @author Moritz Halbritter
 */
class LogbackLogstashStructuredLoggingFormatter implements StructuredLoggingFormatter<ILoggingEvent> {

	private final ThrowableProxyConverter throwableProxyConverter;

	LogbackLogstashStructuredLoggingFormatter(ThrowableProxyConverter throwableProxyConverter) {
		this.throwableProxyConverter = throwableProxyConverter;
	}

	@Override
	public String format(ILoggingEvent event) {
		JsonWriter writer = new JsonWriter();
		writer.object(() -> {
			OffsetDateTime time = OffsetDateTime.ofInstant(event.getInstant(), ZoneId.systemDefault());
			writer.stringMember("@timestamp", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time));
			writer.stringMember("@version", "1");
			writer.stringMember("message", event.getFormattedMessage());
			writer.stringMember("logger_name", event.getLoggerName());
			writer.stringMember("thread_name", event.getThreadName());
			writer.stringMember("level", event.getLevel().toString());
			writer.numberMember("level_value", event.getLevel().toInt());
			addMdc(event, writer);
			addKeyValuePairs(event, writer);
			addMarkers(event, writer);
			IThrowableProxy throwable = event.getThrowableProxy();
			if (throwable != null) {
				writer.stringMember("stack_trace", this.throwableProxyConverter.convert(event));
			}
		});
		writer.newLine();
		return writer.toJson();
	}

	private void addKeyValuePairs(ILoggingEvent event, JsonWriter writer) {
		List<KeyValuePair> keyValuePairs = event.getKeyValuePairs();
		if (CollectionUtils.isEmpty(keyValuePairs)) {
			return;
		}
		for (KeyValuePair pair : keyValuePairs) {
			writer.stringMember(pair.key, ObjectUtils.nullSafeToString(pair.value));
		}
	}

	private static void addMdc(ILoggingEvent event, JsonWriter writer) {
		Map<String, String> mdc = event.getMDCPropertyMap();
		if (CollectionUtils.isEmpty(mdc)) {
			return;
		}
		mdc.forEach(writer::stringMember);
	}

	private void addMarkers(ILoggingEvent event, JsonWriter writer) {
		Set<String> markers = getMarkers(event);
		if (CollectionUtils.isEmpty(markers)) {
			return;
		}
		writer.member("tags", () -> writer.stringArray(markers));
	}

	private Set<String> getMarkers(ILoggingEvent event) {
		if (CollectionUtils.isEmpty(event.getMarkerList())) {
			return Collections.emptySet();
		}
		Set<String> result = new TreeSet<>();
		for (Marker marker : event.getMarkerList()) {
			addMarker(result, marker);
		}
		return result;
	}

	private void addMarker(Set<String> result, Marker marker) {
		result.add(marker.getName());
		if (marker.hasReferences()) {
			Iterator<Marker> iterator = marker.iterator();
			while (iterator.hasNext()) {
				Marker reference = iterator.next();
				addMarker(result, reference);
			}
		}
	}

}
