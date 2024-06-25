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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import org.springframework.util.CollectionUtils;

/**
 * <a href="https://brandur.org/logfmt">Logfmt logging format</a>.
 *
 * @author Moritz Halbritter
 * @since 3.4.0
 */
public class Log4j2LogfmtStructuredLoggingFormatter implements StructuredLoggingFormatter<LogEvent> {

	@Override
	public String format(LogEvent event) {
		KeyValueWriter writer = new KeyValueWriter();
		Instant instant = Instant.ofEpochMilli(event.getInstant().getEpochMillisecond())
			.plusNanos(event.getInstant().getNanoOfMillisecond());
		OffsetDateTime time = OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
		writer.attribute("time", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time));
		writer.attribute("level", event.getLevel().name());
		writer.attribute("msg", event.getMessage().getFormattedMessage());
		writer.attribute("logger", event.getLoggerName());
		addMdc(event, writer);
		ThrowableProxy throwable = event.getThrownProxy();
		if (throwable != null) {
			writer.attribute("exception_class", throwable.getThrowable().getClass().getName());
			writer.attribute("exception_msg", throwable.getMessage());
			writer.attribute("error", throwable.getExtendedStackTraceAsString());
		}
		writer.newLine();
		return writer.finish();
	}

	private static void addMdc(LogEvent event, KeyValueWriter writer) {
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
