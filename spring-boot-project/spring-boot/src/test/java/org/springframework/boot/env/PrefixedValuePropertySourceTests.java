/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.env;

import org.junit.jupiter.api.Test;

import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link PrefixedValuePropertySource}.
 *
 * @author Madhura Bhave
 */
class PrefixedValuePropertySourceTests {

	private PrefixedValuePropertySource propertySource;

	private MockPropertySource source = new MockPropertySource();

	@Test
	void propertySourceWhenPrefixIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PrefixedValuePropertySource(null, this.source))
				.withMessageContaining("Prefix must contain at least one character");
	}

	@Test
	void containsPropertyShouldFindPrefixedProperty() {
		this.source.setProperty("foo.bar", "value");
		this.propertySource = new PrefixedValuePropertySource("test", this.source);
		assertThat(this.propertySource.containsProperty("test.foo.bar")).isTrue();
		assertThat(this.propertySource.containsProperty("foo.bar")).isFalse();
	}

	@Test
	void getPropertyShouldFindPrefixedProperty() {
		this.source.setProperty("foo.bar", "value");
		this.propertySource = new PrefixedValuePropertySource("test", this.source);
		assertThat(this.propertySource.getProperty("test.foo.bar")).isEqualTo("value");
		assertThat(this.propertySource.getProperty("foo.bar")).isNull();
	}

}
