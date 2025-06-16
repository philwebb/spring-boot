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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Internal base class for health contributor registries.
 *
 * @param <C> the contributor type
 * @param <E> the entry type
 * @author Phillip Webb
 */
abstract class AbstractHealthContributorRegistry<C, E> {

	private final Collection<HealthContributorNameValidator> nameValidators;

	private final BiFunction<String, C, E> entryAdapter;

	private volatile Map<String, C> contributors;

	private final Object monitor = new Object();

	AbstractHealthContributorRegistry(Map<String, C> contributors,
			Collection<? extends HealthContributorNameValidator> nameValidators,
			BiFunction<String, C, E> entryAdapter) {
		this.nameValidators = List.copyOf(nameValidators);
		this.entryAdapter = entryAdapter;
		Assert.notNull(contributors, "'contributors' must not be null");
		contributors.keySet().forEach(this::verifyName);
		this.contributors = Collections.unmodifiableMap(new LinkedHashMap<>(contributors));
	}

	void registerContributor(String name, C contributor) {
		Assert.hasText(name, "'name' must not be empty");
		Assert.notNull(contributor, "'contributor' must not be null");
		verifyName(name);
		synchronized (this.monitor) {
			Assert.state(!this.contributors.containsKey(name),
					() -> "A contributor named \"" + name + "\" has already been registered");
			Map<String, C> contributors = new LinkedHashMap<>(this.contributors);
			contributors.put(name, contributor);
			this.contributors = Collections.unmodifiableMap(contributors);
		}
	}

	C unregisterContributor(String name) {
		Assert.notNull(name, "'name' must not be null");
		synchronized (this.monitor) {
			C unregistered = this.contributors.get(name);
			if (unregistered != null) {
				Map<String, C> contributors = new LinkedHashMap<>(this.contributors);
				contributors.remove(name);
				this.contributors = Collections.unmodifiableMap(contributors);
			}
			return unregistered;
		}
	}

	C getContributor(String name) {
		return this.contributors.get(name);
	}

	Iterator<E> iterator() {
		Iterator<Map.Entry<String, C>> iterator = this.contributors.entrySet().iterator();
		return new Iterator<>() {

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public E next() {
				Map.Entry<String, C> entry = iterator.next();
				return AbstractHealthContributorRegistry.this.entryAdapter.apply(entry.getKey(), entry.getValue());
			}

		};
	}

	private void verifyName(String name) {
		Assert.state(StringUtils.hasText(name), "Contributor name must not be empty");
		if (!CollectionUtils.isEmpty(this.nameValidators)) {
			this.nameValidators.forEach((nameValidator) -> nameValidator.validate(name));
		}
	}

}
