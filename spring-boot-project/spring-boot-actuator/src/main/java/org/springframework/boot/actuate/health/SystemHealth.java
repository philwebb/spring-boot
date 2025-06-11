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

package org.springframework.boot.actuate.health;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.health.CompositeHealth;
import org.springframework.boot.health.HealthComponent;
import org.springframework.boot.health.Status;

/**
 * A {@link CompositeHealth} that represents the overall system health and the available
 * groups.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public final class SystemHealth extends CompositeHealth {

	private final ApiVersion apiVersion;

	private final Set<String> groups;

	SystemHealth(Status status, Map<String, HealthComponent> components, Set<String> groups, ApiVersion apiVersion) {
		super(status, components);
		this.groups = (groups != null) ? new TreeSet<>(groups) : null;
		this.apiVersion = apiVersion;
	}

	@Override
	public Map<String, HealthComponent> getComponents() {
		return (this.apiVersion == ApiVersion.V3) ? super.getComponents() : null;
	}

	@JsonProperty
	@JsonInclude(Include.NON_EMPTY)
	public Map<String, HealthComponent> getDetails() {
		return (this.apiVersion == ApiVersion.V2) ? super.getComponents() : null;
	}

	@JsonInclude(Include.NON_EMPTY)
	public Set<String> getGroups() {
		return this.groups;
	}

}
