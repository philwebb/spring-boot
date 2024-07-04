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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.logging.structured.ApplicationMetadata;
import org.springframework.boot.logging.structured.StructuredLoggingFormatter;

/**
 * <a href="https://www.elastic.co/guide/en/ecs/current/ecs-log.html">ECS logging
 * format</a>.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class Log4j2EcsStructuredLoggingFormatter implements StructuredLoggingFormatter<LogEvent> {

	private final JsonWriter<LogEvent> writer;

	Log4j2EcsStructuredLoggingFormatter(ApplicationMetadata metadata) {
		this.writer = JsonWriter.of((members) -> logEventJson(metadata, members));
	}

	private void logEventJson(ApplicationMetadata metadata, JsonWriter.Members<LogEvent> members) {
		members.add("@timestamp", LogEvent::getInstant).as(this::asTimestamp);
		members.add("log.level", LogEvent::getLevel).as(Level::name);
		members.add("process.pid", metadata::pid).whenNotNull();
		members.add("process.thread.name", LogEvent::getThreadName);
		members.add("service.name", metadata::name).whenHasLength();
		members.add("service.version", metadata::version).whenHasLength();
		members.add("service.environment", metadata::environment).whenHasLength();
		members.add("service.node.name", metadata::nodeName).whenHasLength();
		members.add("log.logger", LogEvent::getLoggerName);
		members.add("message", LogEvent::getMessage).as(Message::getFormattedMessage);
		members.add(LogEvent::getContextData)
			.whenNot(ReadOnlyStringMap::isEmpty)
			.asWrittenJson(contextJsonDataWriter());
		members.add(LogEvent::getThrownProxy).whenNotNull().asJson(this::throwableProxyJson);
		members.add("ecs.version", "8.11");
	}

	private java.time.Instant asTimestamp(Instant instant) {
		return java.time.Instant.ofEpochMilli(instant.getEpochMillisecond()).plusNanos(instant.getNanoOfMillisecond());
	}

	private String getClassName(Object object) {
		return object.getClass().getName();
	}

	private JsonWriter<ReadOnlyStringMap> contextJsonDataWriter() {
		return JsonWriter
			.using((contextData, valueWriter) -> valueWriter.writeObject(pairs -> contextData.forEach(pairs::accept)));
	}

	private void throwableProxyJson(Members<ThrowableProxy> thrownProxyMembers) {
		thrownProxyMembers.add("error.type", ThrowableProxy::getThrowable).whenNotNull().as(this::getClassName);
		thrownProxyMembers.add("error.message", ThrowableProxy::getMessage);
		thrownProxyMembers.add("error.stack_trace", ThrowableProxy::getExtendedStackTraceAsString);
	}

	@Override
	public String format(LogEvent event) {
		return this.writer.writeToString(event, "\n");
	}

}
