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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import org.springframework.boot.logging.json.Field;
import org.springframework.boot.logging.json.Fields;
import org.springframework.boot.logging.json.Key;
import org.springframework.boot.logging.json.Value;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Common JSON formats.
 *
 * @author Moritz Halbritter
 */
final class CommonJsonFormats {

	private static final Map<String, Supplier<LogbackJsonFormat>> FORMATS = Map.of("ecs", CommonJsonFormats::ecs,
			"logstash", CommonJsonFormats::logstash);

	private CommonJsonFormats() {
	}

	static LogbackJsonFormat ecs() {
		return new EcsJsonFormat();
	}

	static LogbackJsonFormat logstash() {
		return new LogstashJsonFormat();
	}

	static Set<String> names() {
		return FORMATS.keySet();
	}

	/**
	 * Returns a new instance of the requested {@link LogbackJsonFormat}. Returns
	 * {@code null} if the format isn't known.
	 * @param format the requested format
	 * @return a new instance of the requested format or{@code null} if the format isn't
	 * known.
	 */
	static LogbackJsonFormat create(String format) {
		Assert.notNull(format, "Format must not be null");
		Supplier<LogbackJsonFormat> factory = FORMATS.get(format.toLowerCase(Locale.ENGLISH));
		if (factory == null) {
			return null;
		}
		return factory.get();
	}

	abstract static class BaseLogbackJsonFormat implements LogbackJsonFormat {

		private Long pid;

		private String serviceName;

		private String serviceVersion;

		private String serviceNodeName;

		private String serviceEnvironment;

		private ThrowableProxyConverter throwableProxyConverter;

		@Override
		public void setPid(Long pid) {
			this.pid = pid;
		}

		@Override
		public void setServiceName(String serviceName) {
			this.serviceName = serviceName;
		}

		@Override
		public void setServiceVersion(String serviceVersion) {
			this.serviceVersion = serviceVersion;
		}

		@Override
		public void setThrowableProxyConverter(ThrowableProxyConverter throwableProxyConverter) {
			this.throwableProxyConverter = throwableProxyConverter;
		}

		@Override
		public void setServiceNodeName(String serviceNodeName) {
			this.serviceNodeName = serviceNodeName;
		}

		@Override
		public void setServiceEnvironment(String serviceEnvironment) {
			this.serviceEnvironment = serviceEnvironment;
		}

		Long getPid() {
			return this.pid;
		}

		String getServiceName() {
			return this.serviceName;
		}

		String getServiceVersion() {
			return this.serviceVersion;
		}

		ThrowableProxyConverter getThrowableProxyConverter() {
			return this.throwableProxyConverter;
		}

		String getServiceNodeName() {
			return this.serviceNodeName;
		}

		String getServiceEnvironment() {
			return this.serviceEnvironment;
		}

	}

	/**
	 * <a href="https://www.elastic.co/guide/en/ecs/current/ecs-log.html">ECS logging
	 * format</a>.
	 */
	private static final class EcsJsonFormat extends BaseLogbackJsonFormat {

		@Override
		public Fields getFields(ILoggingEvent event) {
			Fields fields = new Fields();
			fields.add(Key.verbatim("@timestamp"), Value.verbatim(event.getInstant().toString()));
			fields.add(Key.verbatim("log.level"), Value.verbatim(event.getLevel().toString()));
			if (getPid() != null) {
				fields.add(Key.verbatim("process.pid"), Value.of(getPid()));
			}
			fields.add(Key.verbatim("process.thread.name"), Value.escaped(event.getThreadName()));
			if (getServiceName() != null) {
				fields.add(Key.verbatim("service.name"), Value.escaped(getServiceName()));
			}
			if (getServiceVersion() != null) {
				fields.add(Key.verbatim("service.version"), Value.escaped(getServiceVersion()));
			}
			if (getServiceEnvironment() != null) {
				fields.add(Key.verbatim("service.environment"), Value.escaped(getServiceEnvironment()));
			}
			if (getServiceNodeName() != null) {
				fields.add(Key.verbatim("service.node.name"), Value.escaped(getServiceNodeName()));
			}
			fields.add(Key.verbatim("log.logger"), Value.escaped(event.getLoggerName()));
			fields.add(Key.verbatim("message"), Value.escaped(event.getFormattedMessage()));
			addMdc(event, fields);
			addKeyValuePairs(event, fields);
			IThrowableProxy throwable = event.getThrowableProxy();
			if (throwable != null) {
				fields.add(Key.verbatim("error.type"), Value.verbatim(throwable.getClassName()));
				fields.add(Key.verbatim("error.message"), Value.escaped(throwable.getMessage()));
				fields.add(Key.verbatim("error.stack_trace"),
						Value.escaped(getThrowableProxyConverter().convert(event)));
			}
			fields.add(Key.verbatim("ecs.version"), Value.verbatim("8.11"));
			return fields;
		}

		private void addKeyValuePairs(ILoggingEvent event, Fields fields) {
			List<KeyValuePair> keyValuePairs = event.getKeyValuePairs();
			if (CollectionUtils.isEmpty(keyValuePairs)) {
				return;
			}
			for (KeyValuePair keyValuePair : keyValuePairs) {
				fields.add(Field.of(Key.escaped(keyValuePair.key),
						Value.escaped(ObjectUtils.nullSafeToString(keyValuePair.value))));
			}
		}

		private static void addMdc(ILoggingEvent event, Fields fields) {
			Map<String, String> mdc = event.getMDCPropertyMap();
			if (CollectionUtils.isEmpty(mdc)) {
				return;
			}
			for (Entry<String, String> entry : mdc.entrySet()) {
				fields.add(Field.of(Key.escaped(entry.getKey()), Value.escaped(entry.getValue())));
			}
		}

	}

	private static final class LogstashJsonFormat extends BaseLogbackJsonFormat {

		@Override
		public Fields getFields(ILoggingEvent event) {
			Fields fields = new Fields();
			OffsetDateTime time = OffsetDateTime.ofInstant(event.getInstant(), ZoneId.systemDefault());
			fields.add(Key.verbatim("@timestamp"), Value.verbatim(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time)));
			fields.add(Key.verbatim("@version"), Value.verbatim("1"));
			fields.add(Key.verbatim("message"), Value.escaped(event.getFormattedMessage()));
			fields.add(Key.verbatim("logger_name"), Value.escaped(event.getLoggerName()));
			fields.add(Key.verbatim("thread_name"), Value.escaped(event.getThreadName()));
			fields.add(Key.verbatim("level"), Value.escaped(event.getLevel().toString()));
			fields.add(Key.verbatim("level_value"), Value.of(event.getLevel().toInt()));
			addMdc(event, fields);
			addKeyValuePairs(event, fields);
			addMarkers(event, fields);
			IThrowableProxy throwable = event.getThrowableProxy();
			if (throwable != null) {
				fields.add(Key.verbatim("stack_trace"), Value.escaped(getThrowableProxyConverter().convert(event)));
			}
			return fields;
		}

		private void addMarkers(ILoggingEvent event, Fields fields) {
			List<Marker> markers = event.getMarkerList();
			if (CollectionUtils.isEmpty(markers)) {
				return;
			}
			Set<String> tags = new TreeSet<>();
			for (Marker marker : markers) {
				addTag(marker, tags);
			}
			fields.add(Field.of(Key.verbatim("tags"), Value.escaped(tags)));
		}

		private void addTag(Marker marker, Collection<String> tags) {
			tags.add(marker.getName());
			if (marker.hasReferences()) {
				Iterator<Marker> iterator = marker.iterator();
				while (iterator.hasNext()) {
					addTag(iterator.next(), tags);
				}
			}
		}

		private void addKeyValuePairs(ILoggingEvent event, Fields fields) {
			List<KeyValuePair> keyValuePairs = event.getKeyValuePairs();
			if (CollectionUtils.isEmpty(keyValuePairs)) {
				return;
			}
			for (KeyValuePair keyValuePair : keyValuePairs) {
				fields.add(Field.of(Key.escaped(keyValuePair.key),
						Value.escaped(ObjectUtils.nullSafeToString(keyValuePair.value))));
			}
		}

		private static void addMdc(ILoggingEvent event, Fields fields) {
			Map<String, String> mdc = event.getMDCPropertyMap();
			if (CollectionUtils.isEmpty(mdc)) {
				return;
			}
			for (Entry<String, String> entry : mdc.entrySet()) {
				fields.add(Field.of(Key.escaped(entry.getKey()), Value.escaped(entry.getValue())));
			}
		}

	}

}
