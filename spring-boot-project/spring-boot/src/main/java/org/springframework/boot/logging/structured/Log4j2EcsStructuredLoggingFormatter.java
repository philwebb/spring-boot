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

package org.springframework.boot.logging.structured;

import java.time.Instant;
import java.util.Map;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * <a href="https://www.elastic.co/guide/en/ecs/current/ecs-log.html">ECS logging
 * format</a>.
 *
 * @author Moritz Halbritter
 * @since 3.4.0
 */
// TODO MH: This would be better in the log4j2 package, but then we have to make the
// writers public
public class Log4j2EcsStructuredLoggingFormatter implements StructuredLoggingFormatter<LogEvent> {

	private final ApplicationMetadata metadata;

	public Log4j2EcsStructuredLoggingFormatter(ApplicationMetadata metadata) {
		this.metadata = metadata;
	}

	@Override
	public String format(LogEvent event) {
		JsonWriter writer = new JsonWriter();
		writer.objectStart();
		Instant instant = Instant.ofEpochMilli(event.getInstant().getEpochMillisecond())
			.plusNanos(event.getInstant().getNanoOfMillisecond());
		writer.attribute("@timestamp", instant.toString());
		writer.attribute("log.level", event.getLevel().name());
		if (this.metadata.getPid() != null) {
			writer.attribute("process.pid", this.metadata.getPid());
		}
		writer.attribute("process.thread.name", event.getThreadName());
		if (StringUtils.hasLength(this.metadata.getName())) {
			writer.attribute("service.name", this.metadata.getName());
		}
		if (StringUtils.hasLength(this.metadata.getVersion())) {
			writer.attribute("service.version", this.metadata.getVersion());
		}
		if (StringUtils.hasLength(this.metadata.getEnvironment())) {
			writer.attribute("service.environment", this.metadata.getEnvironment());
		}
		if (StringUtils.hasLength(this.metadata.getNodeName())) {
			writer.attribute("service.node.name", this.metadata.getNodeName());
		}
		writer.attribute("log.logger", event.getLoggerName());
		writer.attribute("message", event.getMessage().getFormattedMessage());
		addMdc(event, writer);
		ThrowableProxy throwable = event.getThrownProxy();
		if (throwable != null) {
			writer.attribute("error.type", throwable.getThrowable().getClass().getName());
			writer.attribute("error.message", throwable.getMessage());
			writer.attribute("error.stack_trace", throwable.getExtendedStackTraceAsString());
		}
		writer.attribute("ecs.version", "8.11");
		writer.objectEnd();
		writer.newLine();
		return writer.finish();
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
		mdc.forEach(writer::attribute);
	}

}
