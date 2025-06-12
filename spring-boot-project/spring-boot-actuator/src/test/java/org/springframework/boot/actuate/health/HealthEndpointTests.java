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
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.actuate.health.HealthEndpointSupport.HealthResult;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.ContributedHealth;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.health.registry.DefaultHealthContributorRegistry;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HealthEndpoint}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
@ExtendWith(OutputCaptureExtension.class)
class HealthEndpointTests extends
		HealthEndpointSupportTests<HealthEndpoint, HealthContributorRegistry, HealthContributor, ContributedHealth> {

	@Test
	void healthReturnsSystemHealth() {
		HealthContributorRegistry registry = createRegistry("test", createContributor(this.up));
		HealthEndpoint endpoint = create(registry, this.groups);
		ContributedHealth health = endpoint.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health).isInstanceOf(SystemHealth.class);
	}

	@Test
	void healthWithNoContributorReturnsUp() {
		HealthContributorRegistry registry = createRegistry(Collections.emptyMap());
		HealthEndpointGroups groups = HealthEndpointGroups.of(mock(HealthEndpointGroup.class), Collections.emptyMap());
		ContributedHealth health = create(registry, groups).health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health).isInstanceOf(Health.class);
	}

	@Test
	void healthWhenPathDoesNotExistReturnsNull() {
		HealthContributorRegistry registry = createRegistry("test", createContributor(this.up));
		ContributedHealth health = create(registry, this.groups).healthForPath("missing");
		assertThat(health).isNull();
	}

	@Test
	void healthWhenPathExistsReturnsHealth() {
		HealthContributorRegistry registry = createRegistry("test", createContributor(this.up));
		ContributedHealth health = create(registry, this.groups).healthForPath("test");
		assertThat(health).isEqualTo(this.up);
	}

	@Test
	void healthWhenIndicatorIsSlow(CapturedOutput output) {
		HealthIndicator indicator = () -> {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException ex) {
				// Ignore
			}
			return this.up;
		};
		HealthContributorRegistry registry = createRegistry("test", indicator);
		create(registry, this.groups, Duration.ofMillis(10)).health();
		assertThat(output).contains("Health contributor");
		assertThat(output).contains("to respond");
	}

	@Override
	protected HealthEndpoint create(HealthContributorRegistry registry, HealthEndpointGroups groups,
			Duration slowIndicatorLoggingThreshold) {
		return new HealthEndpoint(registry, groups, slowIndicatorLoggingThreshold);
	}

	@Override
	protected HealthContributorRegistry createRegistry(Map<String, HealthContributor> contributors) {
		return new DefaultHealthContributorRegistry(contributors, Collections.emptyList());
	}

	@Override
	protected HealthContributor createContributor(Health health) {
		return (HealthIndicator) () -> health;
	}

	@Override
	protected HealthContributor createCompositeContributor(Map<String, HealthContributor> contributors) {
		return CompositeHealthContributor.fromMap(contributors);
	}

	@Override
	protected ContributedHealth getHealth(HealthResult<ContributedHealth> result) {
		return result.getHealth();
	}

}
