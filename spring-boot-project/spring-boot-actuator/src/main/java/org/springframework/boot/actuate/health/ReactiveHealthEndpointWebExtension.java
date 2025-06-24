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
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.Selector.Match;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.registry.ReactiveHealthContributorRegistry;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Reactive {@link EndpointWebExtension @EndpointWebExtension} for the
 * {@link HealthEndpoint}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.0.0
 */
@EndpointWebExtension(endpoint = HealthEndpoint.class)
@ImportRuntimeHints(HealthEndpointWebExtensionRuntimeHints.class)
public class ReactiveHealthEndpointWebExtension
		extends HealthEndpointSupport<Mono<? extends Health>, Mono<? extends AbstractHealthDescriptor>> {

	private static final String[] NO_PATH = {};

	/**
	 * Create a new {@link ReactiveHealthEndpointWebExtension} instance.
	 * @param registry the health contributor registry
	 * @param groups the health endpoint groups
	 * @param slowIndicatorLoggingThreshold duration after which slow health indicator
	 * logging should occur
	 * @since 4.0.0
	 */
	public ReactiveHealthEndpointWebExtension(ReactiveHealthContributorRegistry registry, HealthEndpointGroups groups,
			Duration slowIndicatorLoggingThreshold) {
		super(new HealthContributorSupport.Reactive(registry), groups, slowIndicatorLoggingThreshold);
	}

	@ReadOperation
	public Mono<WebEndpointResponse<? extends AbstractHealthDescriptor>> health(ApiVersion apiVersion,
			WebServerNamespace serverNamespace, SecurityContext securityContext) {
		return health(apiVersion, serverNamespace, securityContext, false, NO_PATH);
	}

	@ReadOperation
	public Mono<WebEndpointResponse<? extends AbstractHealthDescriptor>> health(ApiVersion apiVersion,
			WebServerNamespace serverNamespace, SecurityContext securityContext,
			@Selector(match = Match.ALL_REMAINING) String... path) {
		return health(apiVersion, serverNamespace, securityContext, false, path);
	}

	private Mono<WebEndpointResponse<? extends AbstractHealthDescriptor>> health(ApiVersion apiVersion,
			WebServerNamespace serverNamespace, SecurityContext securityContext, boolean showAll, String... path) {
		DescriptorAndGroup<Mono<? extends AbstractHealthDescriptor>> result = getHealth(apiVersion, serverNamespace,
				securityContext, showAll, path);
		if (result == null) {
			return (Arrays.equals(path, NO_PATH))
					? Mono.just(new WebEndpointResponse<>(HealthDescriptor.UP, WebEndpointResponse.STATUS_OK))
					: Mono.just(new WebEndpointResponse<>(WebEndpointResponse.STATUS_NOT_FOUND));
		}
		HealthEndpointGroup group = result.group();
		return result.descriptor().map((health) -> {
			int statusCode = group.getHttpCodeStatusMapper().getStatusCode(health.getStatus());
			return new WebEndpointResponse<>(health, statusCode);
		});
	}

	@Override
	protected Mono<? extends AbstractHealthDescriptor> aggregateContributions(ApiVersion apiVersion,
			Map<String, Mono<? extends AbstractHealthDescriptor>> contributions, StatusAggregator statusAggregator,
			boolean showComponents, Set<String> groupNames) {
		return Flux.fromIterable(contributions.entrySet())
			.flatMap(NamedHealthComponent::create)
			.collectMap(NamedHealthComponent::getName, NamedHealthComponent::getHealth)
			.map((components) -> this.getCompositeHealth(apiVersion, components, statusAggregator, showComponents,
					groupNames));
	}

	/**
	 * A named {@link AbstractHealthDescriptor}.
	 */
	private static final class NamedHealthComponent {

		private final String name;

		private final AbstractHealthDescriptor health;

		private NamedHealthComponent(Object... pair) {
			this.name = (String) pair[0];
			this.health = (AbstractHealthDescriptor) pair[1];
		}

		String getName() {
			return this.name;
		}

		AbstractHealthDescriptor getHealth() {
			return this.health;
		}

		static Mono<NamedHealthComponent> create(Map.Entry<String, Mono<? extends AbstractHealthDescriptor>> entry) {
			Mono<String> name = Mono.just(entry.getKey());
			Mono<? extends AbstractHealthDescriptor> health = entry.getValue();
			return Mono.zip(NamedHealthComponent::new, name, health);
		}

	}

}
