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

import org.springframework.boot.health.contributor.ContributedHealth;
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
 * @param <T> the contributed health component type
 * @author Phillip Webb
 */
interface HealthEndpointContributor<T> extends Iterable<HealthEndpointContributor.Child<T>> {

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
	HealthEndpointContributor<T> getChild(String name);

	/**
	 * Get the health. Must only be called if {@link #isComposite()} returns
	 * {@code false}.
	 * @param includeDetails if details are to be included.
	 * @return the health
	 */
	T getHealth(boolean includeDetails);

	/**
	 * A child consisting of a name and a contributor.
	 *
	 * @param <T> the contributed health component type
	 * @param name the child name
	 * @param contributor the contributor
	 */
	record Child<T>(String name, HealthEndpointContributor<T> contributor) {

	}

	/**
	 * {@link HealthEndpointContributor} to adapt the blocking {@link HealthContributor}
	 * and {@link HealthContributors} types.
	 *
	 * @param contributor the underlying contributor
	 */
	record Blocking(Object contributor) implements HealthEndpointContributor<ContributedHealth> {

		@Override
		public boolean isComposite() {
			return contributor() instanceof HealthContributors;
		}

		@Override
		public HealthEndpointContributor<ContributedHealth> getChild(String name) {
			HealthContributor child = ((HealthContributors) contributor()).getContributor(name);
			return (child != null) ? new Blocking(child) : null;
		}

		@Override
		public Iterator<Child<ContributedHealth>> iterator() {
			return ((HealthContributors) contributor()).stream()
				.map((entry) -> new Child<>(entry.name(), new Blocking(entry.contributor())))
				.iterator();
		}

		@Override
		public ContributedHealth getHealth(boolean includeDetails) {
			return ((HealthIndicator) contributor()).getHealth(includeDetails);
		}

	}

	/**
	 * {@link HealthEndpointContributor} to adapt the reactive
	 * {@link ReactiveHealthContributor} and {@link ReactiveHealthContributors} types.
	 *
	 * @param contributor the underlying contributor
	 */
	record Reactive(Object contributor) implements HealthEndpointContributor<Mono<? extends ContributedHealth>> {

		@Override
		public boolean isComposite() {
			return contributor() instanceof ReactiveHealthContributors;
		}

		@Override
		public HealthEndpointContributor<Mono<? extends ContributedHealth>> getChild(String name) {
			ReactiveHealthContributor child = ((ReactiveHealthContributors) contributor()).getContributor(name);
			return (child != null) ? new Reactive(child) : null;
		}

		@Override
		public Iterator<Child<Mono<? extends ContributedHealth>>> iterator() {
			return ((ReactiveHealthContributors) contributor()).stream()
				.map((entry) -> new Child<>(entry.name(), new Reactive(entry.contributor())))
				.iterator();
		}

		@Override
		public Mono<? extends ContributedHealth> getHealth(boolean includeDetails) {
			return ((ReactiveHealthIndicator) this.contributor).getHealth(includeDetails);
		}

	}

}
