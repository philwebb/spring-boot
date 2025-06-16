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
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.health.HealthEndpointSupport.HealthResult;
import org.springframework.boot.health.contributor.CompositeReactiveHealthContributor;
import org.springframework.boot.health.contributor.ContributedHealth;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthContributor;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.health.registry.DefaultReactiveHealthContributorRegistry;
import org.springframework.boot.health.registry.ReactiveHealthContributorRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReactiveHealthEndpointWebExtension}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class ReactiveHealthEndpointWebExtensionTests extends
		HealthEndpointSupportTests<ReactiveHealthEndpointWebExtension, ReactiveHealthContributorRegistry, ReactiveHealthContributor, Mono<? extends ContributedHealth>> {

	@Test
	void healthReturnsSystemHealth() {
		ReactiveHealthContributorRegistry registry = createRegistry("test", createContributor(this.up));
		ReactiveHealthEndpointWebExtension endpoint = create(registry, this.groups);
		WebEndpointResponse<? extends ContributedHealth> response = endpoint
			.health(ApiVersion.LATEST, null, SecurityContext.NONE)
			.block();
		ContributedHealth health = response.getBody();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health).isInstanceOf(SystemHealth.class);
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void healthWithNoContributorReturnsUp() {
		ReactiveHealthContributorRegistry registry = createRegistry(Collections.emptyMap());
		HealthEndpointGroups groups = HealthEndpointGroups.of(mock(HealthEndpointGroup.class), Collections.emptyMap());
		ReactiveHealthEndpointWebExtension endpoint = create(registry, groups);
		WebEndpointResponse<? extends ContributedHealth> response = endpoint
			.health(ApiVersion.LATEST, null, SecurityContext.NONE)
			.block();
		assertThat(response.getStatus()).isEqualTo(200);
		ContributedHealth health = response.getBody();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health).isInstanceOf(Health.class);
	}

	@Test
	void healthWhenPathDoesNotExistReturnsHttp404() {
		ReactiveHealthContributorRegistry registry = createRegistry("test", createContributor(this.up));
		ReactiveHealthEndpointWebExtension endpoint = create(registry, this.groups);
		WebEndpointResponse<? extends ContributedHealth> response = endpoint
			.health(ApiVersion.LATEST, null, SecurityContext.NONE, "missing")
			.block();
		assertThat(response.getBody()).isNull();
		assertThat(response.getStatus()).isEqualTo(404);
	}

	@Test
	void healthWhenPathExistsReturnsHealth() {
		ReactiveHealthContributorRegistry registry = createRegistry("test", createContributor(this.up));
		ReactiveHealthEndpointWebExtension endpoint = create(registry, this.groups);
		WebEndpointResponse<? extends ContributedHealth> response = endpoint
			.health(ApiVersion.LATEST, null, SecurityContext.NONE, "test")
			.block();
		assertThat(response.getBody()).isEqualTo(this.up);
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Override
	protected ReactiveHealthEndpointWebExtension create(ReactiveHealthContributorRegistry registry,
			HealthEndpointGroups groups, Duration slowIndicatorLoggingThreshold) {
		return new ReactiveHealthEndpointWebExtension(registry, groups, slowIndicatorLoggingThreshold);
	}

	@Override
	protected ReactiveHealthContributorRegistry createRegistry(Map<String, ReactiveHealthContributor> contributors) {
		return new DefaultReactiveHealthContributorRegistry(contributors, Collections.emptyList());
	}

	@Override
	protected ReactiveHealthContributor createContributor(Health health) {
		return (ReactiveHealthIndicator) () -> Mono.just(health);
	}

	@Override
	protected ReactiveHealthContributor createCompositeContributor(
			Map<String, ReactiveHealthContributor> contributors) {
		return CompositeReactiveHealthContributor.fromMap(contributors);
	}

	@Override
	protected ContributedHealth getHealth(HealthResult<Mono<? extends ContributedHealth>> result) {
		return result.getHealth().block();
	}

}
