/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.HealthEndpointGroupsWithAdditionalPath;

/**
 * Default implementation of {@link HealthEndpointGroupsWithAdditionalPath} that supports
 * getting additional paths for health groups on the main and management server.
 *
 * @author Madhura Bhave
 */
public class DefaultHealthEndpointGroupsWithAdditionalPath implements HealthEndpointGroupsWithAdditionalPath {

	private final Map<String, Set<HealthEndpointGroup>> groupsWithAdditionaPath = new LinkedHashMap<>();

	DefaultHealthEndpointGroupsWithAdditionalPath(HealthEndpointGroups groups) {
		this.groupsWithAdditionaPath.put(HealthEndpointProperties.Group.SERVER_PREFIX,
				getPaths(groups, HealthEndpointProperties.Group.SERVER_PREFIX));
		this.groupsWithAdditionaPath.put(HealthEndpointProperties.Group.MANAGEMENT_PREFIX,
				getPaths(groups, HealthEndpointProperties.Group.MANAGEMENT_PREFIX));
	}

	private Set<HealthEndpointGroup> getPaths(HealthEndpointGroups groups, String prefix) {
		return groups.getNames().stream().map(groups::get)
				.filter((group) -> group.getAdditionalPath() != null && group.getAdditionalPath().startsWith(prefix))
				.collect(Collectors.toSet());
	}

	@Override
	public Set<HealthEndpointGroup> getAll(String prefix) {
		return this.groupsWithAdditionaPath.get(prefix);
	}

}
