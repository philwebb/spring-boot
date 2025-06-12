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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link AbstractHealthContributorRegistry}.
 *
 * @param <C> the contributor type
 * @param <E> the entry type
 * @author Phillip Webb
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
abstract class AbstractHealthContributorRegistryTests<C, E> {

	private AbstractHealthContributorRegistry<C, E> registry;

	@BeforeEach
	void setUp() {
		this.registry = createRegistry(Collections.emptyMap(), Collections.emptyList());
	}

	@Test
	void createWhenContributorsIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> createRegistry(null, Collections.emptyList()))
			.withMessage("'contributors' must not be null");
	}

	@Test
	void registerContributorWhenNameIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.registry.registerContributor(null, mockHealthIndicator()))
			.withMessage("'name' must not be empty");
	}

	@Test
	void registerContributorWhenContributorIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.registry.registerContributor("one", null))
			.withMessage("'contributor' must not be null");
	}

	@Test
	void registerContributorRegistersContributors() {
		C c1 = mockHealthIndicator();
		C c2 = mockHealthIndicator();
		this.registry.registerContributor("one", c1);
		this.registry.registerContributor("two", c2);
		assertThat((Iterable<?>) this.registry).hasSize(2);
		assertThat(this.registry.getContributor("one")).isSameAs(c1);
		assertThat(this.registry.getContributor("two")).isSameAs(c2);
	}

	@Test
	void registerContributorWhenNameAlreadyUsedThrowsException() {
		this.registry.registerContributor("one", mockHealthIndicator());
		assertThatIllegalStateException()
			.isThrownBy(() -> this.registry.registerContributor("one", mockHealthIndicator()))
			.withMessageContaining("A contributor named \"one\" has already been registered");
	}

	@Test
	void unregisterContributorUnregistersContributor() {
		C c1 = mockHealthIndicator();
		C c2 = mockHealthIndicator();
		this.registry.registerContributor("one", c1);
		this.registry.registerContributor("two", c2);
		assertThat((Iterable<?>) this.registry).hasSize(2);
		C two = this.registry.unregisterContributor("two");
		assertThat(two).isSameAs(c2);
		assertThat((Iterable<?>) this.registry).hasSize(1);
	}

	@Test
	void unregisterContributorWhenUnknownReturnsNull() {
		this.registry.registerContributor("one", mockHealthIndicator());
		assertThat((Iterable<?>) this.registry).hasSize(1);
		Object two = this.registry.unregisterContributor("two");
		assertThat(two).isNull();
		assertThat((Iterable<?>) this.registry).hasSize(1);
	}

	@Test
	void getContributorReturnsContributor() {
		C c1 = mockHealthIndicator();
		this.registry.registerContributor("one", c1);
		assertThat(this.registry.getContributor("one")).isEqualTo(c1);
	}

	@Test
	void iteratorIteratesContributors() {
		C c1 = mockHealthIndicator();
		C c2 = mockHealthIndicator();
		this.registry.registerContributor("one", c1);
		this.registry.registerContributor("two", c2);
		Iterator<E> iterator = this.registry.iterator();
		E first = iterator.next();
		E second = iterator.next();
		assertThat(iterator.hasNext()).isFalse();
		assertThat(name(first)).isEqualTo("one");
		assertThat(contributor(first)).isEqualTo(c1);
		assertThat(name(second)).isEqualTo("two");
		assertThat(contributor(second)).isEqualTo(c2);
	}

	@Test
	void nameValidatorsValidateMapKeys() {
		assertThatIllegalStateException()
			.isThrownBy(() -> createRegistry(Map.of("ok", mockHealthIndicator(), "fail", mockHealthIndicator()),
					testValidator()))
			.withMessage("Failed validation");
	}

	@Test
	void nameValidatorsValidateRegisteredName() {
		AbstractHealthContributorRegistry<C, E> registry = createRegistry(Collections.emptyMap(), testValidator());
		registry.registerContributor("ok", mockHealthIndicator());
		assertThatIllegalStateException().isThrownBy(() -> registry.registerContributor("fail", mockHealthIndicator()))
			.withMessage("Failed validation");
	}

	private List<HealthContributorNameValidator> testValidator() {
		return List.of((name) -> Assert.state(!"fail".equals(name), "Failed validation"));
	}

	protected abstract AbstractHealthContributorRegistry<C, E> createRegistry(Map<String, C> contributors,
			Collection<? extends HealthContributorNameValidator> nameValidators);

	protected abstract C mockHealthIndicator();

	protected abstract String name(E entry);

	protected abstract C contributor(E entry);

}
