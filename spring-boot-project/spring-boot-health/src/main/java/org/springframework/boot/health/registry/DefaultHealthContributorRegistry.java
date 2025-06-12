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

import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthContributors;

/**
 * Default {@link HealthContributorRegistry} implementation.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class DefaultHealthContributorRegistry
		extends AbstractHealthContributorRegistry<HealthContributor, HealthContributors.Entry>
		implements HealthContributorRegistry {

	/**
	 * Create a new empty {@link DefaultHealthContributorRegistry} instance.
	 */
	public DefaultHealthContributorRegistry() {
		this(Collections.emptyMap(), Collections.emptyList());
	}

	/**
	 * Create a new {@link DefaultHealthContributorRegistry} instance.
	 * @param contributors the initial set of health contributors
	 * @param nameValidators the name validators to apply
	 */
	public DefaultHealthContributorRegistry(Map<String, HealthContributor> contributors,
			Collection<? extends HealthContributorNameValidator> nameValidators) {
		super(contributors, nameValidators, HealthContributors.Entry::new);
	}

	@Override
	public HealthContributor getContributor(String name) {
		return super.getContributor(name);
	}

	@Override
	public Iterator<HealthContributors.Entry> iterator() {
		return super.iterator();
	}

	@Override
	public void registerContributor(String name, HealthContributor contributor) {
		super.registerContributor(name, contributor);
	}

	@Override
	public HealthContributor unregisterContributor(String name) {
		return super.unregisterContributor(name);
	}

}
