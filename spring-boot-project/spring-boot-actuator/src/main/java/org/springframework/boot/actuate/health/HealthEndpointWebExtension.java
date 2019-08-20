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

package org.springframework.boot.actuate.health;

import java.util.Map;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.Selector.Match;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;

/**
 * {@link EndpointWebExtension @EndpointWebExtension} for the {@link HealthEndpoint}.
 *
 * @author Christian Dupuis
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Madhura Bhave
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@EndpointWebExtension(endpoint = HealthEndpoint.class)
public class HealthEndpointWebExtension extends HealthEndpointSupport<HealthContributor, HealthComponent> {

	private static final String[] NO_PATH = {};

	/**
	 * Create a new {@link HealthEndpointWebExtension} instance using a delegate endpoint.
	 * @param delegate the delegate endpoint
	 * @param responseMapper the response mapper
	 * @deprecated since 2.2.0 in favor of
	 * {@link #HealthEndpointWebExtension(HealthContributorRegistry, HealthEndpointSettings)}
	 */
	@Deprecated
	public HealthEndpointWebExtension(HealthEndpoint delegate, HealthWebEndpointResponseMapper responseMapper) {
	}

	/**
	 * Create a new {@link HealthEndpointWebExtension} instance.
	 * @param registry the health contributor registry
	 * @param settings the health endpoint settings
	 */
	public HealthEndpointWebExtension(HealthContributorRegistry registry, HealthEndpointSettings settings) {
		super(registry, settings);
	}

	@ReadOperation
	public WebEndpointResponse<HealthComponent> health(SecurityContext securityContext) {
		return health(securityContext, NO_PATH);
	}

	@ReadOperation
	public WebEndpointResponse<HealthComponent> health(SecurityContext securityContext,
			@Selector(match = Match.ALL_REMAINING) String... path) {
		return health(securityContext, false, path);
	}

	public WebEndpointResponse<HealthComponent> health(SecurityContext securityContext, boolean alwaysIncludeDetails,
			String... path) {
		HealthComponent health = getHealth(securityContext, alwaysIncludeDetails, path);
		if (health == null) {
			return new WebEndpointResponse<>(WebEndpointResponse.STATUS_NOT_FOUND);
		}
		int statusCode = getSettings().getHttpCodeStatusMapper().getStatusCode(health.getStatus());
		return new WebEndpointResponse<>(health, statusCode);
	}

	@Override
	protected HealthComponent getHealth(HealthContributor contributor, boolean includeDetails) {
		return ((HealthIndicator) contributor).getHealth(includeDetails);
	}

	@Override
	protected HealthComponent aggregateContributions(Map<String, HealthComponent> contributions,
			StatusAggregator statusAggregator, boolean includeDetails) {
		return getCompositeHealth(contributions, statusAggregator, includeDetails);
	}

}
