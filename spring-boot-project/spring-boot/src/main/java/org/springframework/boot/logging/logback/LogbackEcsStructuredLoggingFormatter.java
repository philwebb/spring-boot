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
import java.util.Map;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import org.slf4j.event.KeyValuePair;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.ApplicationMetadata;
import org.springframework.boot.logging.structured.StructuredLoggingFormatter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.as;

/**
 * <a href="https://www.elastic.co/guide/en/ecs/current/ecs-log.html">ECS logging
 * format</a>.
 *
 * @author Moritz Halbritter
 */
class LogbackEcsStructuredLoggingFormatter implements StructuredLoggingFormatter<ILoggingEvent> {

	private final ThrowableProxyConverter throwableProxyConverter;

	private final ApplicationMetadata metadata;

	LogbackEcsStructuredLoggingFormatter(ThrowableProxyConverter throwableProxyConverter,
			ApplicationMetadata metadata) {
		this.throwableProxyConverter = throwableProxyConverter;
		this.metadata = metadata;
	}

	@Override
	public String format(ILoggingEvent event) {
		JsonWriter<ILoggingEvent> writer = JsonWriter2.of((json)-> {
			json.add("@timestamp", event::getInstant);
		});


		write.from(event::getInstant).whenHasLength().to("@timestamp");
		write.from(event::getLevel).to("log.level");
		// write.from(...).as(..).to("field");
		write.from(event::getThrowableProxy).asJson((throwable) -> {
			throwableProxy.member(throwableProxy::getClassName).to("")
		});

		return writer.write(event).toString();



		JsonWriter writer = new JsonWriter();
		writer.object(() -> {
			writer.stringMember("@timestamp", event.getInstant().toString());
			writer.stringMember("log.level", event.getLevel().toString());
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
			writer.stringMember("message", event.getFormattedMessage());
			addMdc(event, writer);
			addKeyValuePairs(event, writer);
			IThrowableProxy throwable = event.getThrowableProxy();
			if (throwable != null) {
				writer.stringMember("error.type", throwable.getClassName());
				writer.stringMember("error.message", throwable.getMessage());
				writer.stringMember("error.stack_trace", this.throwableProxyConverter.convert(event));
			}
			writer.stringMember("ecs.version", "8.11");
		});
		writer.newLine();
		return writer.toJson();
	}

	private void addKeyValuePairs(ILoggingEvent event, JsonWriter writer) {
		List<KeyValuePair> keyValuePairs = event.getKeyValuePairs();
		if (CollectionUtils.isEmpty(keyValuePairs)) {
			return;
		}
		for (KeyValuePair pair : keyValuePairs) {
			writer.stringMember(pair.key, ObjectUtils.nullSafeToString(pair.value));
		}
	}

	private static void addMdc(ILoggingEvent event, JsonWriter writer) {
		Map<String, String> mdc = event.getMDCPropertyMap();
		if (CollectionUtils.isEmpty(mdc)) {
			return;
		}
		mdc.forEach(writer::stringMember);
	}

}
