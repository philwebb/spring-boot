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

package org.springframework.boot.health.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.springframework.boot.health.contributor.ReactiveHealthContributor;
import org.springframework.boot.health.contributor.ReactiveHealthContributors;

/**
 * Default {@link ReactiveHealthContributorRegistry} implementation.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class DefaultReactiveHealthContributorRegistry
		extends AbstractHealthContributorRegistry<ReactiveHealthContributor, ReactiveHealthContributors.Entry>
		implements ReactiveHealthContributorRegistry {

	/**
	 * Create a new empty {@link DefaultReactiveHealthContributorRegistry} instance.
	 */
	public DefaultReactiveHealthContributorRegistry() {
		this(Collections.emptyMap(), Collections.emptyList());
	}

	/**
	 * Create a new {@link DefaultReactiveHealthContributorRegistry} instance.
	 * @param contributors the initial set of health contributors
	 * @param nameValidators the name validators to apply
	 */
	public DefaultReactiveHealthContributorRegistry(Map<String, ReactiveHealthContributor> contributors,
			Collection<? extends HealthContributorNameValidator> nameValidators) {
		super(contributors, nameValidators, ReactiveHealthContributors.Entry::new);
	}

	@Override
	public ReactiveHealthContributor getContributor(String name) {
		return super.getContributor(name);
	}

	@Override
	public Iterator<ReactiveHealthContributors.Entry> iterator() {
		return super.iterator();
	}

	@Override
	public void registerContributor(String name, ReactiveHealthContributor contributor) {
		super.registerContributor(name, contributor);
	}

	@Override
	public ReactiveHealthContributor unregisterContributor(String name) {
		return super.unregisterContributor(name);
	}

}
