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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
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
 */
class Log4j2EcsStructuredLoggingFormatter implements StructuredLoggingFormatter<LogEvent> {

	private final ApplicationMetadata metadata;

	Log4j2EcsStructuredLoggingFormatter(ApplicationMetadata metadata) {
		this.metadata = metadata;
	}

	@Override
	public String format(LogEvent event) {
		JsonWriter<LogEvent> writer = JsonWriter.of((members) -> {
			extracted(event, members);
		});
		return writer.write(event).toString(); // FIXME new line?
	}

	/**
	 * @param event
	 * @param members
	 */
	private void extracted(LogEvent event, Members<LogEvent> members) {
		members.add("@timestamp", event::getInstant)
			.as((instant) -> Instant.ofEpochMilli(instant.getEpochMillisecond())
				.plusNanos(instant.getNanoOfMillisecond()));
		members.add("log.level", LogEvent::getLevel).as(Level::name);
		members.add("process.pid", this.metadata::pid).whenNotNull();
		members.add("process.thread.name", event::getThreadName);
		members.add("service.name", this.metadata::name).whenHasLength();
		members.add("service.version", this.metadata::version).whenHasLength();
		members.add("service.environment", this.metadata::environment).whenHasLength();
		members.add("service.node.name", this.metadata::nodeName).whenHasLength();
		members.add("log.logger", event::getLoggerName);
		members.add("message", event::getMessage).as(Message::getFormattedMessage);
		members.add(event::getContextData)
			.whenNot(ReadOnlyStringMap::isEmpty)
			.asJson(JsonWriter.using((contextData, memberWriter) -> contextData.forEach(memberWriter::write)));
		members.add(event::getThrownProxy).whenNotNull().asJson((thrownProxyMembers) -> {
			thrownProxyMembers.add("error.type", ThrowableProxy::getThrowable)
				.whenNotNull()
				.as((ex) -> ex.getClass().getName());
			thrownProxyMembers.add("error.message", ThrowableProxy::getMessage);
			thrownProxyMembers.add("error.stack_trace", ThrowableProxy::getExtendedStackTraceAsString);
		});
		members.add("ecs.version", "8.11");
	}

}
