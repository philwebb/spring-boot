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

package org.springframework.boot.logging.json;

/**
 * Structured logging format.
 *
 * @author Moritz Halbritter
 * @since 3.4.0
 */
public interface StructuredLoggingFormat {

	/**
	 * Creates the writer needed for the {@link #write(LogEvent, StructuredLoggingWriter)}
	 * method.
	 * @param stringBuilder the underlying {@code StringBuilder} to write to
	 * @return the writer
	 */
	StructuredLoggingWriter createWriter(StringBuilder stringBuilder);

	/**
	 * Writes the given log event to the writer.
	 * @param event the log event to write
	 * @param builder the writer to write to
	 */
	void write(LogEvent event, StructuredLoggingWriter builder);

}
