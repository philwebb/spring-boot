/*
 * Copyright 2012-present the original author or authors.
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

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.boot.health.contributor.CompositeHealth;
import org.springframework.boot.health.contributor.ContributedHealth;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for health endpoints and health endpoint extensions.
 *
 * @param <T> the contributed health component type
 * @author Phillip Webb
 * @author Scott Frederick
 */
abstract class HealthEndpointSupport<T> {

	private static final Log logger = LogFactory.getLog(HealthEndpointSupport.class);

	static final Health DEFAULT_HEALTH = Health.up().build();

	private final HealthEndpointContributor<T> root;

	private final HealthEndpointGroups groups;

	private final Duration slowIndicatorLoggingThreshold;

	/**
	 * Create a new {@link HealthEndpointSupport} instance.
	 * @param root the root contributor
	 * @param groups the health endpoint groups
	 * @param slowIndicatorLoggingThreshold duration after which slow health indicator
	 * logging should occur
	 */
	HealthEndpointSupport(HealthEndpointContributor<T> root, HealthEndpointGroups groups,
			Duration slowIndicatorLoggingThreshold) {
		Assert.notNull(root, "'root' must not be null");
		Assert.notNull(groups, "'groups' must not be null");
		this.root = root;
		this.groups = groups;
		this.slowIndicatorLoggingThreshold = slowIndicatorLoggingThreshold;
	}

	HealthResult<T> getHealth(ApiVersion apiVersion, WebServerNamespace serverNamespace,
			SecurityContext securityContext, boolean showAll, String... path) {
		if (path.length > 0) {
			HealthEndpointGroup group = getHealthGroup(serverNamespace, path);
			if (group != null) {
				return getHealth(apiVersion, group, securityContext, showAll, path, 1);
			}
		}
		return getHealth(apiVersion, this.groups.getPrimary(), securityContext, showAll, path, 0);
	}

	private HealthEndpointGroup getHealthGroup(WebServerNamespace serverNamespace, String... path) {
		if (this.groups.get(path[0]) != null) {
			return this.groups.get(path[0]);
		}
		if (serverNamespace != null) {
			AdditionalHealthEndpointPath additionalPath = AdditionalHealthEndpointPath.of(serverNamespace, path[0]);
			return this.groups.get(additionalPath);
		}
		return null;
	}

	private HealthResult<T> getHealth(ApiVersion apiVersion, HealthEndpointGroup group, SecurityContext securityContext,
			boolean showAll, String[] path, int pathOffset) {
		boolean showComponents = showAll || group.showComponents(securityContext);
		boolean showDetails = showAll || group.showDetails(securityContext);
		boolean isSystemHealth = group == this.groups.getPrimary() && pathOffset == 0;
		boolean isRoot = path.length - pathOffset == 0;
		if (!showComponents && !isRoot) {
			return null;
		}
		HealthEndpointContributor<T> contributor = getContributor(path, pathOffset);
		if (contributor == null) {
			return null;
		}
		String name = getName(path, pathOffset);
		Set<String> groupNames = isSystemHealth ? this.groups.getNames() : null;
		T health = getContribution(apiVersion, group, name, contributor, showComponents, showDetails, groupNames);
		return (health != null) ? new HealthResult<>(health, group) : null;
	}

	@SuppressWarnings("unchecked")
	private HealthEndpointContributor<T> getContributor(String[] path, int pathOffset) {
		HealthEndpointContributor<T> contributor = this.root;
		while (pathOffset < path.length) {
			if (!contributor.isComposite()) {
				return null;
			}
			contributor = contributor.getChild(path[pathOffset]);
			pathOffset++;
		}
		return contributor;
	}

	private String getName(String[] path, int pathOffset) {
		StringBuilder name = new StringBuilder();
		while (pathOffset < path.length) {
			name.append((!name.isEmpty()) ? "/" : "");
			name.append(path[pathOffset]);
			pathOffset++;
		}
		return name.toString();
	}

	@SuppressWarnings("unchecked")
	private T getContribution(ApiVersion apiVersion, HealthEndpointGroup group, String name,
			HealthEndpointContributor<T> contributor, boolean showComponents, boolean showDetails,
			Set<String> groupNames) {
		if (contributor.isComposite()) {
			return getAggregateContribution(apiVersion, group, name, contributor, showComponents, showDetails,
					groupNames);
		}
		if (contributor != null && (name.isEmpty() || group.isMember(name))) {
			return getLoggedHealth(contributor, name, showDetails);
		}
		return null;
	}

	private T getAggregateContribution(ApiVersion apiVersion, HealthEndpointGroup group, String name,
			HealthEndpointContributor<T> contributor, boolean showComponents, boolean showDetails,
			Set<String> groupNames) {
		String prefix = (StringUtils.hasText(name)) ? name + "/" : "";
		Map<String, T> contributions = new LinkedHashMap<>();
		for (HealthEndpointContributor.Child<T> child : contributor) {
			T contribution = getContribution(apiVersion, group, prefix + child.name(), child.contributor(),
					showComponents, showDetails, null);
			if (contribution != null) {
				contributions.put(child.name(), contribution);
			}
		}
		if (contributions.isEmpty()) {
			return null;
		}
		return aggregateContributions(apiVersion, contributions, group.getStatusAggregator(), showComponents,
				groupNames);
	}

	private T getLoggedHealth(HealthEndpointContributor<T> contributor, String name, boolean showDetails) {
		Instant start = Instant.now();
		try {
			return contributor.getHealth(showDetails);
		}
		finally {
			if (logger.isWarnEnabled() && this.slowIndicatorLoggingThreshold != null) {
				Duration duration = Duration.between(start, Instant.now());
				if (duration.compareTo(this.slowIndicatorLoggingThreshold) > 0) {
					String contributorClassName = contributor.getClass().getName();
					Object contributorIdentifier = (!StringUtils.hasLength(name)) ? contributorClassName
							: contributorClassName + " (" + name + ")";
					logger.warn(LogMessage.format("Health contributor %s took %s to respond", contributorIdentifier,
							DurationStyle.SIMPLE.print(duration)));
				}
			}
		}
	}

	protected abstract T aggregateContributions(ApiVersion apiVersion, Map<String, T> contributions,
			StatusAggregator statusAggregator, boolean showComponents, Set<String> groupNames);

	protected final CompositeHealth getCompositeHealth(ApiVersion apiVersion, Map<String, ContributedHealth> components,
			StatusAggregator statusAggregator, boolean showComponents, Set<String> groupNames) {
		Status status = statusAggregator
			.getAggregateStatus(components.values().stream().map(this::getStatus).collect(Collectors.toSet()));
		Map<String, ContributedHealth> instances = showComponents ? components : null;
		return new SystemHealth(status, instances, groupNames, apiVersion);
	}

	private Status getStatus(ContributedHealth component) {
		return (component != null) ? component.getStatus() : Status.UNKNOWN;
	}

	/**
	 * A health result containing health and the group that created it.
	 *
	 * @param <T> the contributed health component
	 */
	static class HealthResult<T> {

		private final T health;

		private final HealthEndpointGroup group;

		HealthResult(T health, HealthEndpointGroup group) {
			this.health = health;
			this.group = group;
		}

		T getHealth() {
			return this.health;
		}

		HealthEndpointGroup getGroup() {
			return this.group;
		}

	}

}
