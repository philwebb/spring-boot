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
import java.util.Map;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.ApplicationMetadata;
import org.springframework.boot.logging.structured.StructuredLoggingFormatter;
import org.springframework.util.CollectionUtils;

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
		JsonWriter<LogEvent> writer = JsonWriter.of((x, y) -> {
			x.add("@timestamp", event::getInstant)
				.as((instant) -> Instant.ofEpochMilli(instant.getEpochMillisecond())
					.plusNanos(instant.getNanoOfMillisecond()));
			x.add("log.level", () -> event.getLevel().name());
			x.add("process.pid", this.metadata::pid).whenNotNull();
			x.add("process.thread.name", event::getThreadName);
			x.add("service.name", this.metadata::name).whenHasLength();
			x.add("service.version", this.metadata::version).whenHasLength();
			x.add("service.environment", this.metadata::environment).whenHasLength();
			x.add("service.node.name", this.metadata::nodeName).whenHasLength();
			x.add("log.logger", event::getLoggerName);
			x.add("message", event::getMessage).as(Message::getFormattedMessage);
			x.add(event::getContextData)
				.asJson((xx, contextData) -> contextData.forEach(xx::add))
				.whenNot(ReadOnlyStringMap::isEmpty);
			x.add(event::getThrownProxy).asJson((xx) -> {
				xx.add("error.type", ThrowableProxy::getThrowable).whenNotNull().as((ex) -> ex.getClass().getName());
				xx.add("error.message", ThrowableProxy::getMessage);
				xx.add("error.stack_trace", ThrowableProxy::getExtendedStackTraceAsString);
			}).whenNotNull();
			x.add("ecs.version", "8.11");
		});
		return writer.write(event).toString(); // FIXME new line?
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

}
