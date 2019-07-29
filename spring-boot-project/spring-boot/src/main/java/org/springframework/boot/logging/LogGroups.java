/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.logging;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Logger groups configured via the Spring Environment.
 *
 * @author HaiTao Zhang
 * @since 2.2.0
 */
public class LogGroups implements Iterable<LogGroup> {

	private final LoggingSystem loggingSystem;

	private final Map<String, LogGroup> groups;

	public LogGroups(LoggingSystem loggingSystem, Map<String, LogGroup> groups) {
		this.loggingSystem = loggingSystem;
		this.groups = new ConcurrentHashMap<>(groups);
	}

	/**
	 * Returns the logger group with the specified name or {@code null} if the group isn't
	 * configured.
	 * @param name the logger group name
	 * @return the logger group
	 */
	public LogGroup getGroup(String name) {
		return this.groups.get(name);
	}

	@Override
	public Iterator<LogGroup> iterator() {
		return this.groups.values().iterator();
	}

	public Stream<LogGroup> stream() {
		return this.groups.values().stream();
	}

	/**
	 * Update the log level for an existing logger group.
	 * @param name the name of the group
	 * @param updatedLevel the new level for the group
	 */
	public void updateGroupLevel(String name, LogLevel updatedLevel) {
		LogGroup group = this.groups.get(name);
		if (group != null) {
			List<String> members = group.getMembers();
			LogLevel configuredLevel = (updatedLevel != null) ? updatedLevel : group.getConfiguredLevel();
			this.groups.put(name, new LogGroup(name, members, configuredLevel));
			members.forEach((logger) -> this.loggingSystem.setLogLevel(logger, configuredLevel));
		}
	}

}
