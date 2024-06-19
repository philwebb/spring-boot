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
import java.util.Map;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * <a href="https://brandur.org/logfmt">Logfmt logging format</a>.
 *
 * @author Moritz Halbritter
 */
class LogfmtStructuredLoggingFormat implements StructuredLoggingFormat {

	@Override
	public String getId() {
		return "logfmt";
	}

	@Override
	public String format(LogEvent event) {
		KeyValueWriter writer = new KeyValueWriter();
		OffsetDateTime time = OffsetDateTime.ofInstant(event.getTimestamp(), ZoneId.systemDefault());
		writer.attribute("time", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time));
		writer.attribute("level", event.getLevel());
		writer.attribute("msg", event.getFormattedMessage());
		writer.attribute("logger", event.getLoggerName());
		addMdc(event, writer);
		addKeyValuePairs(event, writer);
		if (event.hasThrowable()) {
			writer.attribute("exception_class", event.getThrowableClassName());
			writer.attribute("exception_msg", event.getThrowableMessage());
			writer.attribute("error", event.getThrowableStackTraceAsString());
		}
		writer.newLine();
		return writer.finish();
	}

	private void addKeyValuePairs(LogEvent event, KeyValueWriter writer) {
		Map<String, Object> keyValuePairs = event.getKeyValuePairs();
		if (CollectionUtils.isEmpty(keyValuePairs)) {
			return;
		}
		keyValuePairs.forEach((key, value) -> writer.attribute(key, ObjectUtils.nullSafeToString(value)));
	}

	private static void addMdc(LogEvent event, KeyValueWriter writer) {
		Map<String, String> mdc = event.getMdc();
		if (CollectionUtils.isEmpty(mdc)) {
			return;
		}
		mdc.forEach(writer::attribute);
	}

}
