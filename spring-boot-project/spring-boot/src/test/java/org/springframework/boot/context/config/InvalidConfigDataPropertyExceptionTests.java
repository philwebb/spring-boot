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

package org.springframework.boot.context.config;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.origin.MockOrigin;
import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link InvalidConfigDataPropertyException}.
 *
 * @author Phillip Webb
 */
class InvalidConfigDataPropertyExceptionTests {

	private ConfigDataLocation location = new TestConfigDataLocation();

	private ConfigurationPropertyName replacement = ConfigurationPropertyName.of("replacement");

	private ConfigurationPropertyName invaid = ConfigurationPropertyName.of("invalid");

	private ConfigurationProperty property = new ConfigurationProperty(this.invaid, "bad", MockOrigin.of("origin"));

	@Test
	void createHasCorrectMessage() {
		assertThat(new InvalidConfigDataPropertyException(this.property, this.replacement, this.location)).hasMessage(
				"Property 'invalid' imported from location 'test' is invalid and should be replaced with 'replacement' [origin: origin]");
	}

	@Test
	void createWhenNoLocationHasCorrectMessage() {
		assertThat(new InvalidConfigDataPropertyException(this.property, this.replacement, null))
				.hasMessage("Property 'invalid' is invalid and should be replaced with 'replacement' [origin: origin]");
	}

	@Test
	void createWhenNoReplacementHasCorrectMessage() {
		assertThat(new InvalidConfigDataPropertyException(this.property, null, this.location))
				.hasMessage("Property 'invalid' imported from location 'test' is invalid [origin: origin]");
	}

	@Test
	void createWhenNoOriginHasCorrectMessage() {
		ConfigurationProperty property = new ConfigurationProperty(this.invaid, "bad", null);
		assertThat(new InvalidConfigDataPropertyException(property, this.replacement, this.location)).hasMessage(
				"Property 'invalid' imported from location 'test' is invalid and should be replaced with 'replacement'");
	}

	@Test
	void getPropertyReturnsProperty() {
		InvalidConfigDataPropertyException exception = new InvalidConfigDataPropertyException(this.property,
				this.replacement, this.location);
		assertThat(exception.getProperty()).isEqualTo(this.property);
	}

	@Test
	void getLocationReturnsLocation() {
		InvalidConfigDataPropertyException exception = new InvalidConfigDataPropertyException(this.property,
				this.replacement, this.location);
		assertThat(exception.getLocation()).isEqualTo(this.location);
	}

	@Test
	void getReplacementReturnsReplacement() {
		InvalidConfigDataPropertyException exception = new InvalidConfigDataPropertyException(this.property,
				this.replacement, this.location);
		assertThat(exception.getReplacement()).isEqualTo(this.replacement);
	}

	@Test
	void throwIfInvalidPropertyFoundWhenHasInvalidPropertyThrowsException() {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.profile", "a");
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofExisting(propertySource);
		assertThatExceptionOfType(InvalidConfigDataPropertyException.class)
				.isThrownBy(() -> InvalidConfigDataPropertyException.throwIfInvalidPropertyFound(contributor))
				.withMessageStartingWith("Property 'spring.profile' is invalid and should be replaced with "
						+ "'spring.config.activate.on-profile'");
	}

	@Test
	void throwIfInvalidPropertyFoundWhenHasNoInvalidPropertyDoesNothing() {
		ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor
				.ofExisting(new MockPropertySource());
		InvalidConfigDataPropertyException.throwIfInvalidPropertyFound(contributor);
	}

	private static class TestConfigDataLocation extends ConfigDataLocation {

		@Override
		public String toString() {
			return "test";
		}

	}

}
