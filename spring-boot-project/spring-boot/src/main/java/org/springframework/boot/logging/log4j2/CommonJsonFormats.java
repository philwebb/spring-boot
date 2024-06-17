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
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import org.springframework.boot.logging.json.Fields;
import org.springframework.boot.logging.json.Key;
import org.springframework.boot.logging.json.Value;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Common JSON formats.
 *
 * @author Moritz Halbritter
 */
final class CommonJsonFormats {

	private static final Map<String, Supplier<Log4jJsonFormat>> FORMATS = Map.of("ecs", CommonJsonFormats::ecs,
			"logstash", CommonJsonFormats::logstash);

	private CommonJsonFormats() {
	}

	static Log4jJsonFormat ecs() {
		return new EcsJsonFormat();
	}

	static Log4jJsonFormat logstash() {
		return new LogstashJsonFormat();
	}

	static Set<String> names() {
		return FORMATS.keySet();
	}

	/**
	 * Returns a new instance of the requested {@link Log4jJsonFormat}. Returns
	 * {@code null} if the format isn't known.
	 * @param format the requested format
	 * @return a new instance of the requested format or{@code null} if the format isn't
	 * known.
	 */
	static Log4jJsonFormat create(String format) {
		Assert.notNull(format, "Format must not be null");
		Supplier<Log4jJsonFormat> factory = FORMATS.get(format.toLowerCase(Locale.ENGLISH));
		if (factory == null) {
			return null;
		}
		return factory.get();
	}

	abstract static class BaseLog4jJsonFormat implements Log4jJsonFormat {

		private Long pid;

		private String serviceName;

		private String serviceVersion;

		private String serviceNodeName;

		private String serviceEnvironment;

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

		String getServiceNodeName() {
			return this.serviceNodeName;
		}

		String getServiceEnvironment() {
			return this.serviceEnvironment;
		}

		protected Instant toInstant(org.apache.logging.log4j.core.time.Instant instant) {
			return Instant.ofEpochMilli(instant.getEpochMillisecond()).plusNanos(instant.getNanoOfMillisecond());
		}

	}

	/**
	 * <a href="https://www.elastic.co/guide/en/ecs/current/ecs-log.html">ECS logging
	 * format</a>.
	 */
	private static final class EcsJsonFormat extends BaseLog4jJsonFormat {

		@Override
		public Fields getFields(LogEvent event) {
			Fields fields = new Fields();
			fields.add(Key.verbatim("@timestamp"), Value.verbatim(toInstant(event.getInstant()).toString()));
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
			fields.add(Key.verbatim("message"), Value.escaped(event.getMessage().getFormattedMessage()));
			addMdc(event, fields);
			ThrowableProxy throwable = event.getThrownProxy();
			if (throwable != null) {
				fields.add(Key.verbatim("error.type"), Value.verbatim(throwable.getThrowable().getClass().getName()));
				fields.add(Key.verbatim("error.message"), Value.escaped(throwable.getMessage()));
				fields.add(Key.verbatim("error.stack_trace"), Value.escaped(throwable.getExtendedStackTraceAsString()));
			}
			fields.add(Key.verbatim("ecs.version"), Value.verbatim("8.11"));
			return fields;
		}

		private static void addMdc(LogEvent event, Fields fields) {
			ReadOnlyStringMap mdc = event.getContextData();
			if (mdc == null || mdc.isEmpty()) {
				return;
			}
			mdc.forEach(
					(key, value) -> fields.add(Key.escaped(key), Value.escaped(ObjectUtils.nullSafeToString(value))));
		}

	}

	private static final class LogstashJsonFormat extends BaseLog4jJsonFormat {

		@Override
		public Fields getFields(LogEvent event) {
			Fields fields = new Fields();
			OffsetDateTime time = OffsetDateTime.ofInstant(toInstant(event.getInstant()), ZoneId.systemDefault());
			fields.add(Key.verbatim("@timestamp"), Value.verbatim(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time)));
			fields.add(Key.verbatim("@version"), Value.verbatim("1"));
			fields.add(Key.verbatim("message"), Value.escaped(event.getMessage().getFormattedMessage()));
			fields.add(Key.verbatim("logger_name"), Value.escaped(event.getLoggerName()));
			fields.add(Key.verbatim("thread_name"), Value.escaped(event.getThreadName()));
			fields.add(Key.verbatim("level"), Value.escaped(event.getLevel().name()));
			fields.add(Key.verbatim("level_value"), Value.of(event.getLevel().intLevel()));
			addMdc(event, fields);
			addMarkers(event, fields);
			ThrowableProxy throwable = event.getThrownProxy();
			if (throwable != null) {
				fields.add(Key.verbatim("stack_trace"), Value.escaped(throwable.getExtendedStackTraceAsString()));
			}
			return fields;
		}

		private void addMarkers(LogEvent event, Fields fields) {
			Marker marker = event.getMarker();
			if (marker == null) {
				return;
			}
			Set<String> tags = new TreeSet<>();
			addTag(marker, tags);
			fields.add(Key.verbatim("tags"), Value.escaped(tags));
		}

		private void addTag(Marker marker, Collection<String> tags) {
			tags.add(marker.getName());
			if (marker.hasParents()) {
				for (Marker parent : marker.getParents()) {
					addTag(parent, tags);
				}
			}
		}

		private static void addMdc(LogEvent event, Fields fields) {
			ReadOnlyStringMap mdc = event.getContextData();
			if (mdc == null || mdc.isEmpty()) {
				return;
			}
			mdc.forEach(
					(key, value) -> fields.add(Key.escaped(key), Value.escaped(ObjectUtils.nullSafeToString(value))));
		}

	}

}
