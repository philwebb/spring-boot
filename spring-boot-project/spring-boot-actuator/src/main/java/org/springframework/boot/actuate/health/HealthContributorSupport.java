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

import java.util.Iterator;

import reactor.core.publisher.Mono;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthContributors;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.ReactiveHealthContributor;
import org.springframework.boot.health.contributor.ReactiveHealthContributors;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;

/**
 * Allows {@link HealthEndpointSupport} to access blocking or reactive contributors and
 * registries in a uniform way.
 *
 * @param <H> the health type
 * @param <D> the descriptor type
 * @author Phillip Webb
 */
interface HealthContributorSupport<H, D> extends Iterable<HealthContributorSupport.Child<H, D>> {

	/**
	 * Return if this contributor is a composite and may have children.
	 * @return if the contributor is a composite
	 */
	boolean isComposite();

	/**
	 * Get the child with the given name. Must only be called if {@link #isComposite()}
	 * returns {@code true}.
	 * @param name the child name
	 * @return the child or {@code null}
	 */
	HealthContributorSupport<H, D> getChild(String name);

	/**
	 * Get the health. Must only be called if {@link #isComposite()} returns
	 * {@code false}.
	 * @param includeDetails if details are to be included.
	 * @return the health
	 */
	D getDescriptor(boolean includeDetails);

	/**
	 * A child consisting of a name and a contributor.
	 *
	 * @param <H> the health type
	 * @param <D> the descriptor type
	 * @param name the child name
	 * @param contributor the contributor
	 */
	record Child<H, D>(String name, HealthContributorSupport<H, D> contributor) {

	}

	/**
	 * {@link HealthContributorSupport} to adapt the blocking {@link HealthContributor}
	 * and {@link HealthContributors} types.
	 *
	 * @param contributor the underlying contributor
	 */
	record Blocking(Object contributor) implements HealthContributorSupport<Health, HealthComponentDescriptor> {

		@Override
		public boolean isComposite() {
			return contributor() instanceof HealthContributors;
		}

		@Override
		public Blocking getChild(String name) {
			HealthContributor child = ((HealthContributors) contributor()).getContributor(name);
			return (child != null) ? new Blocking(child) : null;
		}

		@Override
		public Iterator<Child<Health, HealthComponentDescriptor>> iterator() {
			return ((HealthContributors) contributor()).stream()
				.map((entry) -> new Child<>(entry.name(), new Blocking(entry.contributor())))
				.iterator();
		}

		@Override
		public HealthComponentDescriptor getDescriptor(boolean includeDetails) {
			return HealthDescriptor.of(((HealthIndicator) contributor()).getHealth(includeDetails));
		}

	}

	/**
	 * {@link HealthContributorSupport} to adapt the reactive
	 * {@link ReactiveHealthContributor} and {@link ReactiveHealthContributors} types.
	 *
	 * @param contributor the underlying contributor
	 */
	record Reactive(Object contributor)
			implements
				HealthContributorSupport<Mono<? extends Health>, Mono<? extends HealthComponentDescriptor>> {

		@Override
		public boolean isComposite() {
			return contributor() instanceof ReactiveHealthContributors;
		}

		@Override
		public Reactive getChild(String name) {
			ReactiveHealthContributor child = ((ReactiveHealthContributors) contributor()).getContributor(name);
			return (child != null) ? new Reactive(child) : null;
		}

		@Override
		public Iterator<Child<Mono<? extends Health>, Mono<? extends HealthComponentDescriptor>>> iterator() {
			return ((ReactiveHealthContributors) contributor()).stream()
				.map((entry) -> new Child<>(entry.name(), new Reactive(entry.contributor())))
				.iterator();
		}

		@Override
		public Mono<? extends HealthComponentDescriptor> getDescriptor(boolean includeDetails) {
			return ((ReactiveHealthIndicator) this.contributor).getHealth(includeDetails).map(HealthDescriptor::of);
		}

	}

}
