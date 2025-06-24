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
import org.springframework.boot.health.contributor.Status;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for health endpoints and health endpoint extensions.
 *
 * @param <H> the health type
 * @param <D> the descriptor type
 * @author Phillip Webb
 * @author Scott Frederick
 */
abstract class HealthEndpointSupport<H, D> {

	private static final Log logger = LogFactory.getLog(HealthEndpointSupport.class);

	private final HealthContributorSupport<H, D> registry;

	private final HealthEndpointGroups groups;

	private final Duration slowIndicatorLoggingThreshold;

	/**
	 * Create a new {@link HealthEndpointSupport} instance.
	 * @param registry the health contributor registry
	 * @param groups the health endpoint groups
	 * @param slowIndicatorLoggingThreshold duration after which slow health indicator
	 * logging should occur
	 */
	HealthEndpointSupport(HealthContributorSupport<H, D> registry, HealthEndpointGroups groups,
			Duration slowIndicatorLoggingThreshold) {
		Assert.notNull(registry, "'registry' must not be null");
		Assert.notNull(groups, "'groups' must not be null");
		this.registry = registry;
		this.groups = groups;
		this.slowIndicatorLoggingThreshold = slowIndicatorLoggingThreshold;
	}

	DescriptorAndGroup<D> getHealth(ApiVersion apiVersion, WebServerNamespace serverNamespace,
			SecurityContext securityContext, boolean showAll, String... path) {
		HealthEndpointGroup group = (path.length > 0) ? getHealthGroup(serverNamespace, path) : null;
		if (group != null) {
			return getHealth(apiVersion, group, securityContext, showAll, path, 1);
		}
		return getHealth(apiVersion, this.groups.getPrimary(), securityContext, showAll, path, 0);
	}

	private HealthEndpointGroup getHealthGroup(WebServerNamespace serverNamespace, String... path) {
		if (this.groups.get(path[0]) != null) {
			return this.groups.get(path[0]);
		}
		if (serverNamespace != null) {
			return this.groups.get(AdditionalHealthEndpointPath.of(serverNamespace, path[0]));
		}
		return null;
	}

	private DescriptorAndGroup<D> getHealth(ApiVersion apiVersion, HealthEndpointGroup group,
			SecurityContext securityContext, boolean showAll, String[] path, int pathOffset) {
		boolean showComponents = showAll || group.showComponents(securityContext);
		boolean showDetails = showAll || group.showDetails(securityContext);
		boolean isSystemHealth = group == this.groups.getPrimary() && pathOffset == 0;
		boolean isRoot = path.length - pathOffset == 0;
		if (!showComponents && !isRoot) {
			return null;
		}
		HealthContributorSupport<H, D> contributor = getContributor(path, pathOffset);
		if (contributor == null) {
			return null;
		}
		String name = getName(path, pathOffset);
		Set<String> groupNames = (!isSystemHealth) ? null : this.groups.getNames();
		D health = getContribution(apiVersion, group, name, contributor, showComponents, showDetails, groupNames);
		return (health != null) ? new DescriptorAndGroup<>(health, group) : null;
	}

	@SuppressWarnings("unchecked")
	private HealthContributorSupport<H, D> getContributor(String[] path, int pathOffset) {
		HealthContributorSupport<H, D> contributor = this.registry;
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
	private D getContribution(ApiVersion apiVersion, HealthEndpointGroup group, String name,
			HealthContributorSupport<H, D> contributor, boolean showComponents, boolean showDetails,
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

	private D getAggregateContribution(ApiVersion apiVersion, HealthEndpointGroup group, String name,
			HealthContributorSupport<H, D> contributor, boolean showComponents, boolean showDetails,
			Set<String> groupNames) {
		String prefix = (StringUtils.hasText(name)) ? name + "/" : "";
		Map<String, D> contributions = new LinkedHashMap<>();
		for (HealthContributorSupport.Child<H, D> child : contributor) {
			D contribution = getContribution(apiVersion, group, prefix + child.name(), child.contributor(),
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

	private D getLoggedHealth(HealthContributorSupport<H, D> contributor, String name, boolean showDetails) {
		Instant start = Instant.now();
		try {
			return contributor.getDescriptor(showDetails);
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

	protected abstract D aggregateContributions(ApiVersion apiVersion, Map<String, D> contributions,
			StatusAggregator statusAggregator, boolean showComponents, Set<String> groupNames);

	protected final CompositeHealthDetails getCompositeHealth(ApiVersion apiVersion,
			Map<String, HealthComponentDescriptor> components, StatusAggregator statusAggregator,
			boolean showComponents, Set<String> groupNames) {
		Status status = statusAggregator
			.getAggregateStatus(components.values().stream().map(this::getStatus).collect(Collectors.toSet()));
		components = (!showComponents) ? null : components;
		return (groupNames != null) ? new SystemHealthDetails(apiVersion, status, components, groupNames)
				: new CompositeHealthDetails(apiVersion, status, components);
	}

	private Status getStatus(HealthComponentDescriptor component) {
		return (component != null) ? component.getStatus() : Status.UNKNOWN;
	}

	/**
	 * A health result containing descriptor and the group that created it.
	 *
	 * @param descriptor the health descriptor
	 * @param group the group used to create the health
	 * @param <D> the details type
	 */
	record DescriptorAndGroup<D>(D descriptor, HealthEndpointGroup group) {

	}

}
