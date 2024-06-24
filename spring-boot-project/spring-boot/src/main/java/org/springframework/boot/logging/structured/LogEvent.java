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
import java.util.Map;
import java.util.Set;

/**
 * A log event.
 *
 * @author Moritz Halbritter
 * @since 3.4.0
 */
public interface LogEvent {

	Instant getTimestamp();

	String getLevel();

	int getLevelValue();

	String getThreadName();

	String getLoggerName();

	String getFormattedMessage();

	boolean hasThrowable();

	String getThrowableClassName();

	String getThrowableMessage();

	String getThrowableStackTraceAsString();

	Map<String, Object> getKeyValuePairs();

	Map<String, String> getMdc();

	Set<String> getMarkers();

	Long getPid();

	// TODO: Rename to application name
	String getServiceName();

	// TODO: Rename to application version
	String getServiceVersion();

	String getServiceEnvironment();

	String getServiceNodeName();

}
