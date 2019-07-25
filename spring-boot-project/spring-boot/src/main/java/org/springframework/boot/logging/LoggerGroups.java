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
public class LoggerGroups implements Iterable<LoggerGroups.LoggerGroup> {

	private final LoggingSystem loggingSystem;

	private final Map<String, LoggerGroup> groups = new ConcurrentHashMap<>();

	public LoggerGroups(LoggingSystem loggingSystem, Map<String, LoggerGroup> groups) {
		this.loggingSystem = loggingSystem;
		this.groups.putAll(groups);
	}

	/**
	 * Returns the logger group with the specified name or {@code null} if the group isn't
	 * configured.
	 * @param name the logger group name
	 * @return the logger group
	 */
	public LoggerGroup getGroup(String name) {
		return this.groups.get(name);
	}

	@Override
	public Iterator<LoggerGroup> iterator() {
		return this.groups.values().iterator();
	}

	public Stream<LoggerGroup> stream() {
		return this.groups.values().stream();
	}

	/**
	 * Update the log level for an existing logger group.
	 * @param name the name of the group
	 * @param updatedLevel the new level for the group
	 */
	public void updateGroupLevel(String name, LogLevel updatedLevel) {
		LoggerGroup group = this.groups.get(name);
		if (group != null) {
			List<String> members = group.getMembers();
			LogLevel configuredLevel = (updatedLevel != null) ? updatedLevel : group.getConfiguredLevel();
			this.groups.put(name, new LoggerGroup(name, members, configuredLevel));
			members.forEach((logger) -> this.loggingSystem.setLogLevel(logger, configuredLevel));
		}
	}

	/**
	 * A single logger group.
	 */
	public static class LoggerGroup {

		private final String name;

		private final List<String> members;

		private final LogLevel configuredLevel;

		public LoggerGroup(String name, List<String> members, LogLevel configuredLevel) {
			this.name = name;
			this.members = members;
			this.configuredLevel = configuredLevel;
		}

		public String getName() {
			return this.name;
		}

		public List<String> getMembers() {
			return this.members;
		}

		public LogLevel getConfiguredLevel() {
			return this.configuredLevel;
		}

	}

}
