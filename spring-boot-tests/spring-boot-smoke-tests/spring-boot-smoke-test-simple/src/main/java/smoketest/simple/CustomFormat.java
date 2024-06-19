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

package smoketest.simple;

import org.springframework.boot.logging.structured.JsonWriter;
import org.springframework.boot.logging.structured.LogEvent;
import org.springframework.boot.logging.structured.StructuredLoggingFormat;

/**
 * Custom {@link StructuredLoggingFormat}.
 *
 * @author Moritz Halbritter
 */
public class CustomFormat implements StructuredLoggingFormat {

	@Override
	public String format(LogEvent event) {
		JsonWriter writer = new JsonWriter();
		writer.objectStart();
		writer.attribute("epoch", event.getTimestamp().toEpochMilli());
		writer.attribute("msg", event.getFormattedMessage());
		if (event.hasThrowable()) {
			writer.attribute("error", event.getThrowableStackTraceAsString());
		}
		writer.objectEnd();
		writer.newLine();
		return writer.finish();
	}

}
