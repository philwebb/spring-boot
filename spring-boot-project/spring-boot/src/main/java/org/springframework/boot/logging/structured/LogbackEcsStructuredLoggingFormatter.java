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

import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import org.slf4j.event.KeyValuePair;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * <a href="https://www.elastic.co/guide/en/ecs/current/ecs-log.html">ECS logging
 * format</a>.
 *
 * @author Moritz Halbritter
 * @since 3.4.0
 */
// TODO MH: This would be better in the logback package, but then we have to make the
// writers public
public class LogbackEcsStructuredLoggingFormatter implements StructuredLoggingFormatter<ILoggingEvent> {

	private final ThrowableProxyConverter throwableProxyConverter;

	private final ApplicationMetadata metadata;

	public LogbackEcsStructuredLoggingFormatter(ThrowableProxyConverter throwableProxyConverter,
			ApplicationMetadata metadata) {
		this.throwableProxyConverter = throwableProxyConverter;
		this.metadata = metadata;
	}

	@Override
	public String format(ILoggingEvent event) {
		JsonWriter writer = new JsonWriter();
		writer.objectStart();
		writer.attribute("@timestamp", event.getInstant().toString());
		writer.attribute("log.level", event.getLevel().toString());
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
		writer.attribute("message", event.getFormattedMessage());
		addMdc(event, writer);
		addKeyValuePairs(event, writer);
		IThrowableProxy throwable = event.getThrowableProxy();
		if (throwable != null) {
			writer.attribute("error.type", throwable.getClassName());
			writer.attribute("error.message", throwable.getMessage());
			writer.attribute("error.stack_trace", this.throwableProxyConverter.convert(event));
		}
		writer.attribute("ecs.version", "8.11");
		writer.objectEnd();
		writer.newLine();
		return writer.finish();
	}

	private void addKeyValuePairs(ILoggingEvent event, JsonWriter writer) {
		List<KeyValuePair> keyValuePairs = event.getKeyValuePairs();
		if (CollectionUtils.isEmpty(keyValuePairs)) {
			return;
		}
		for (KeyValuePair pair : keyValuePairs) {
			writer.attribute(pair.key, ObjectUtils.nullSafeToString(pair.value));
		}
	}

	private static void addMdc(ILoggingEvent event, JsonWriter writer) {
		Map<String, String> mdc = event.getMDCPropertyMap();
		if (CollectionUtils.isEmpty(mdc)) {
			return;
		}
		mdc.forEach(writer::attribute);
	}

}
