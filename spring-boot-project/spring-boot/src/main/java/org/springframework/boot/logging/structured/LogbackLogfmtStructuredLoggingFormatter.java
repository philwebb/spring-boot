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

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import org.slf4j.event.KeyValuePair;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * <a href="https://brandur.org/logfmt">Logfmt logging format</a>.
 *
 * @author Moritz Halbritter
 * @since 3.4.0
 */
public class LogbackLogfmtStructuredLoggingFormatter implements StructuredLoggingFormatter<ILoggingEvent> {

	private final ThrowableProxyConverter throwableProxyConverter;

	public LogbackLogfmtStructuredLoggingFormatter(ThrowableProxyConverter throwableProxyConverter) {
		this.throwableProxyConverter = throwableProxyConverter;
	}

	@Override
	public String format(ILoggingEvent event) {
		KeyValueWriter writer = new KeyValueWriter();
		OffsetDateTime time = OffsetDateTime.ofInstant(event.getInstant(), ZoneId.systemDefault());
		writer.attribute("time", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time));
		writer.attribute("level", event.getLevel().toString());
		writer.attribute("msg", event.getFormattedMessage());
		writer.attribute("logger", event.getLoggerName());
		addMdc(event, writer);
		addKeyValuePairs(event, writer);
		IThrowableProxy throwable = event.getThrowableProxy();
		if (throwable != null) {
			writer.attribute("exception_class", throwable.getClassName());
			writer.attribute("exception_msg", throwable.getMessage());
			writer.attribute("error", this.throwableProxyConverter.convert(event));
		}
		writer.newLine();
		return writer.finish();
	}

	private void addKeyValuePairs(ILoggingEvent event, KeyValueWriter writer) {
		List<KeyValuePair> keyValuePairs = event.getKeyValuePairs();
		if (CollectionUtils.isEmpty(keyValuePairs)) {
			return;
		}
		for (KeyValuePair pair : keyValuePairs) {
			writer.attribute(pair.key, ObjectUtils.nullSafeToString(pair.value));
		}
	}

	private static void addMdc(ILoggingEvent event, KeyValueWriter writer) {
		Map<String, String> mdc = event.getMDCPropertyMap();
		if (CollectionUtils.isEmpty(mdc)) {
			return;
		}
		mdc.forEach(writer::attribute);
	}

}
