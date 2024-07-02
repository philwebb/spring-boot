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

import java.util.List;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import org.slf4j.event.KeyValuePair;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.logging.structured.ApplicationMetadata;
import org.springframework.boot.logging.structured.StructuredLoggingFormatter;
import org.springframework.util.CollectionUtils;

/**
 * <a href="https://www.elastic.co/guide/en/ecs/current/ecs-log.html">ECS logging
 * format</a>.
 *
 * @author Moritz Halbritter
 */
class LogbackEcsStructuredLoggingFormatter implements StructuredLoggingFormatter<ILoggingEvent> {

	private JsonWriter<ILoggingEvent> writer;

	LogbackEcsStructuredLoggingFormatter(ApplicationMetadata metadata,
			ThrowableProxyConverter throwableProxyConverter) {
		this.writer = JsonWriter.of((members) -> loggingEventJson(metadata, throwableProxyConverter, members));
	}

	private void loggingEventJson(ApplicationMetadata metadata, ThrowableProxyConverter throwableProxyConverter,
			JsonWriter.Members<ILoggingEvent> members) {
		members.add("@timestamp", ILoggingEvent::getInstant);
		members.add("log.level", ILoggingEvent::getLevel);
		members.add("process.pid", metadata::pid).whenNotNull();
		members.add("process.thread.name", ILoggingEvent::getThreadName);
		members.add("service.name", metadata::name).whenHasLength();
		members.add("service.version", metadata::version).whenHasLength();
		members.add("service.environment", metadata::environment).whenHasLength();
		members.add("service.node.name", metadata::nodeName).whenHasLength();
		members.add("log.logger", ILoggingEvent::getLoggerName);
		members.add("message", ILoggingEvent::getFormattedMessage);
		members.add(ILoggingEvent::getMDCPropertyMap).whenNot(CollectionUtils::isEmpty);
		members.add(ILoggingEvent::getKeyValuePairs).asWrittenJson(keyValuePairsJsonDataWriter());
		members.add((event) -> event)
			.when((event) -> event.getThrowableProxy() != null)
			.asJson((throwableMembers) -> throwableJson(throwableProxyConverter, throwableMembers));
		members.add("ecs.version", "8.11");
	}

	private void throwableJson(ThrowableProxyConverter converter, Members<ILoggingEvent> members) {
		members.add("error.type", ILoggingEvent::getThrowableProxy).as(IThrowableProxy::getClassName);
		members.add("error.message", ILoggingEvent::getThrowableProxy).as(IThrowableProxy::getMessage);
		members.add("error.stack_trace", (event) -> converter.convert(event));
	}

	private JsonWriter<List<KeyValuePair>> keyValuePairsJsonDataWriter() {
		return JsonWriter.using((pairs, memberWriter) -> {
			if (!CollectionUtils.isEmpty(pairs)) {
				pairs.forEach((pair) -> memberWriter.write(pair.key, pair.value));
			}
		});
	}

	@Override
	public String format(ILoggingEvent event) {
		return this.writer.write(event).toStringWithNewLine();
	}

}
