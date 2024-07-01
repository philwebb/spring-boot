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
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.ApplicationMetadata;
import org.springframework.boot.logging.structured.StructuredLoggingFormatter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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
		JsonWriter writer = new JsonWriter();
		writer.object(() -> {
			Instant instant = Instant.ofEpochMilli(event.getInstant().getEpochMillisecond())
				.plusNanos(event.getInstant().getNanoOfMillisecond());
			writer.stringMember("@timestamp", instant.toString());
			writer.stringMember("log.level", event.getLevel().name());
			if (this.metadata.pid() != null) {
				writer.numberMember("process.pid", this.metadata.pid());
			}
			writer.stringMember("process.thread.name", event.getThreadName());
			if (StringUtils.hasLength(this.metadata.name())) {
				writer.stringMember("service.name", this.metadata.name());
			}
			if (StringUtils.hasLength(this.metadata.version())) {
				writer.stringMember("service.version", this.metadata.version());
			}
			if (StringUtils.hasLength(this.metadata.environment())) {
				writer.stringMember("service.environment", this.metadata.environment());
			}
			if (StringUtils.hasLength(this.metadata.nodeName())) {
				writer.stringMember("service.node.name", this.metadata.nodeName());
			}
			writer.stringMember("log.logger", event.getLoggerName());
			writer.stringMember("message", event.getMessage().getFormattedMessage());
			addMdc(event, writer);
			ThrowableProxy throwable = event.getThrownProxy();
			if (throwable != null) {
				writer.stringMember("error.type", throwable.getThrowable().getClass().getName());
				writer.stringMember("error.message", throwable.getMessage());
				writer.stringMember("error.stack_trace", throwable.getExtendedStackTraceAsString());
			}
			writer.stringMember("ecs.version", "8.11");
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

}
