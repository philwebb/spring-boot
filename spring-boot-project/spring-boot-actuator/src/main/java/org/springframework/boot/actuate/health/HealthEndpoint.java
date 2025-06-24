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
import java.util.Map;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.Selector.Match;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.registry.HealthContributorRegistry;

/**
 * {@link Endpoint @Endpoint} to expose application health information.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @since 2.0.0
 */
@Endpoint(id = "health")
public class HealthEndpoint extends HealthEndpointSupport<Health, AbstractHealthDescriptor> {

	/**
	 * Health endpoint id.
	 */
	public static final EndpointId ID = EndpointId.of("health");

	private static final String[] EMPTY_PATH = {};

	/**
	 * Create a new {@link HealthEndpoint} instance.
	 * @param registry the health contributor registry
	 * @param groups the health endpoint groups
	 * @param slowIndicatorLoggingThreshold duration after which slow health indicator
	 * logging should occur
	 * @since 4.0.0
	 */
	public HealthEndpoint(HealthContributorRegistry registry, HealthEndpointGroups groups,
			Duration slowIndicatorLoggingThreshold) {
		super(new HealthContributorSupport.Blocking(registry), groups, slowIndicatorLoggingThreshold);
	}

	@ReadOperation
	public AbstractHealthDescriptor health() {
		AbstractHealthDescriptor health = health(ApiVersion.V3, EMPTY_PATH);
		return (health != null) ? health : HealthDescriptor.UP;
	}

	@ReadOperation
	public AbstractHealthDescriptor healthForPath(@Selector(match = Match.ALL_REMAINING) String... path) {
		return health(ApiVersion.V3, path);
	}

	private AbstractHealthDescriptor health(ApiVersion apiVersion, String... path) {
		DescriptorAndGroup<AbstractHealthDescriptor> descriptorAndGroup = getHealth(apiVersion, null,
				SecurityContext.NONE, true, path);
		return (descriptorAndGroup != null) ? descriptorAndGroup.descriptor() : null;
	}

	@Override
	protected AbstractHealthDescriptor aggregateContributions(ApiVersion apiVersion,
			Map<String, AbstractHealthDescriptor> contributions, StatusAggregator statusAggregator,
			boolean showComponents, Set<String> groupNames) {
		return getCompositeHealth(apiVersion, contributions, statusAggregator, showComponents, groupNames);
	}

}
