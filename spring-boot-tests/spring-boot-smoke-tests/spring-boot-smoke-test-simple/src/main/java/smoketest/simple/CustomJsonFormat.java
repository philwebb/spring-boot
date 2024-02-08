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

import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;

import org.springframework.boot.logging.json.Field;
import org.springframework.boot.logging.json.Key;
import org.springframework.boot.logging.json.Value;
import org.springframework.boot.logging.logback.JsonEncoder.LogbackJsonFormat;

/**
 * A custom implementation of a JSON format.
 *
 * @author Moritz Halbritter
 */
class CustomJsonFormat implements LogbackJsonFormat {

	@Override
	public Iterable<Field> getFields(ILoggingEvent event) {
		return List.of(Field.of(Key.verbatim("epoch"), Value.of(event.getInstant().toEpochMilli())),
				Field.of(Key.verbatim("message"), Value.escaped(event.getFormattedMessage())));
	}

}
