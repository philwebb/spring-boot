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

import java.util.Map;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * <a href="https://www.elastic.co/guide/en/ecs/current/ecs-log.html">ECS logging
 * format</a>.
 *
 * @author Moritz Halbritter
 */
class EcsStructuredLoggingFormat implements StructuredLoggingFormat {

	@Override
	public String getId() {
		return "ecs";
	}

	@Override
	public String format(LogEvent event) {
		JsonWriter writer = new JsonWriter();
		writer.objectStart();
		writer.attribute("@timestamp", event.getTimestamp().toString());
		writer.attribute("log.level", event.getLevel());
		if (event.getPid() != null) {
			writer.attribute("process.pid", event.getPid());
		}
		writer.attribute("process.thread.name", event.getThreadName());
		if (event.getServiceName() != null) {
			writer.attribute("service.name", event.getServiceName());
		}
		if (event.getServiceVersion() != null) {
			writer.attribute("service.version", event.getServiceVersion());
		}
		if (event.getServiceEnvironment() != null) {
			writer.attribute("service.environment", event.getServiceEnvironment());
		}
		if (event.getServiceNodeName() != null) {
			writer.attribute("service.node.name", event.getServiceNodeName());
		}
		writer.attribute("log.logger", event.getLoggerName());
		writer.attribute("message", event.getFormattedMessage());
		addMdc(event, writer);
		addKeyValuePairs(event, writer);
		if (event.hasThrowable()) {
			writer.attribute("error.type", event.getThrowableClassName());
			writer.attribute("error.message", event.getThrowableMessage());
			writer.attribute("error.stack_trace", event.getThrowableStackTraceAsString());
		}
		writer.attribute("ecs.version", "8.11");
		writer.objectEnd();
		writer.newLine();
		return writer.finish();
	}

	private void addKeyValuePairs(LogEvent event, JsonWriter writer) {
		Map<String, Object> keyValuePairs = event.getKeyValuePairs();
		if (CollectionUtils.isEmpty(keyValuePairs)) {
			return;
		}
		keyValuePairs.forEach((key, value) -> writer.attribute(key, ObjectUtils.nullSafeToString(value)));
	}

	private static void addMdc(LogEvent event, JsonWriter writer) {
		Map<String, String> mdc = event.getMdc();
		if (CollectionUtils.isEmpty(mdc)) {
			return;
		}
		mdc.forEach(writer::attribute);
	}

}
