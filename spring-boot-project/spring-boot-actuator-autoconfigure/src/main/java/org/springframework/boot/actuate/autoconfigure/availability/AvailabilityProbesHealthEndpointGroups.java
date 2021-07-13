/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.availability;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.util.Assert;

/**
 * {@link HealthEndpointGroups} decorator to support availability probes.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 */
class AvailabilityProbesHealthEndpointGroups implements HealthEndpointGroups {

	private final Map<String, AvailabilityProbesHealthEndpointGroup> groups;

	private static final Map<String, String> GROUPS;

	static {
		Map<String, String> groups = new LinkedHashMap<>();
		groups.put("liveness", "/livez");
		groups.put("readiness", "/readyz");
		GROUPS = Collections.unmodifiableMap(groups);
	}

	private final HealthEndpointGroups additionalGroups;

	private final Set<String> names;

	AvailabilityProbesHealthEndpointGroups(HealthEndpointGroups additionalGroups, boolean additionalPathsEnabled) {
		Assert.notNull(additionalGroups, "Groups must not be null");
		this.additionalGroups = additionalGroups;
		this.groups = setupGroups(additionalPathsEnabled);
		Set<String> names = new LinkedHashSet<>(additionalGroups.getNames());
		names.addAll(this.groups.keySet());
		this.names = Collections.unmodifiableSet(names);
	}

	private Map<String, AvailabilityProbesHealthEndpointGroup> setupGroups(boolean additionalPathsEnabled) {
		Map<String, AvailabilityProbesHealthEndpointGroup> groups = new LinkedHashMap<>();
		for (Map.Entry<String, String> group : GROUPS.entrySet())
			groups.put(group.getKey(), new AvailabilityProbesHealthEndpointGroup(
					getAdditionalPath(additionalPathsEnabled, group.getValue()), group.getKey()));
		return Collections.unmodifiableMap(groups);
	}

	private String getAdditionalPath(boolean additionalPathsEnabled, String path) {
		return (additionalPathsEnabled) ? "server:" + path : null;
	}

	@Override
	public HealthEndpointGroup getPrimary() {
		return this.additionalGroups.getPrimary();
	}

	@Override
	public Set<String> getNames() {
		return this.names;
	}

	@Override
	public HealthEndpointGroup get(String name) {
		HealthEndpointGroup group = this.additionalGroups.get(name);
		if (group == null) {
			group = this.groups.get(name);
		}
		return group;
	}

	static boolean containsAllProbeGroups(HealthEndpointGroups groups) {
		return groups.getNames().containsAll(GROUPS.keySet());
	}

}
