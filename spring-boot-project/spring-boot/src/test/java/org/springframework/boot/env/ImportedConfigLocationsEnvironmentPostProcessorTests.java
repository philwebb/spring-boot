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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ImportedConfigLocationsEnvironmentPostProcessor}.
 *
 * @author Madhura Bhave
 */
public class ImportedConfigLocationsEnvironmentPostProcessorTests {

	private ImportedConfigLocationsEnvironmentPostProcessor postProcessor = new ImportedConfigLocationsEnvironmentPostProcessor();

	private StandardEnvironment environment;

	private final SpringApplication application = new SpringApplication();

	@BeforeEach
	void setup() {
		this.environment = new StandardEnvironment();
	}

	@Test
	void postProcessorShouldProcessPropertiesFiles() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.environment.import.foo.locations=file:src/test/resources/additional-config/test.properties");
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty("foo.something")).isEqualTo("bucket");
		assertThat(this.environment.getProperty("foo.value")).isEqualTo("1234");
		assertThat(this.environment.getProperty("foo.my.property")).isEqualTo("fromtestproperties");
	}

	@Test
	void postProcessorShouldProcessYamlFiles() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.environment.import.foo.locations=file:src/test/resources/additional-config/test.yaml");
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty("foo.something")).isEqualTo("bucket");
		assertThat(this.environment.getProperty("foo.value")).isEqualTo("1234");
		assertThat(this.environment.getProperty("foo.my.property")).isEqualTo("fromtestproperties");
	}

	@Test
	void postProcessorWithMultipleImports() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.environment.import.foo.locations=file:src/test/resources/additional-config/test.properties,file:src/test/resources/additional-config/other-test.properties",
				"spring.environment.import.bar.locations=file:src/test/resources/additional-config/other.properties");
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty("foo.something")).isEqualTo("bucket");
		assertThat(this.environment.getProperty("foo.value")).isEqualTo("1234");
		assertThat(this.environment.getProperty("foo.my.property")).isEqualTo("fromtestproperties");
		assertThat(this.environment.getProperty("foo.other.property")).isEqualTo("fromothertestproperties");
		assertThat(this.environment.getProperty("bar.hello")).isEqualTo("world");
	}

	@Test
	void postProcessorWhenMultipleContentLocationsHaveTheSameKeyShouldThrowException() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.environment.import.foo.locations=file:src/test/resources/additional-config/test.properties,file:src/test/resources/additional-config/test.yaml");
		assertThatIllegalStateException()
				.isThrownBy(() -> this.postProcessor.postProcessEnvironment(this.environment, this.application));
	}

	@Test
	void postProcessorWhenUsePrefixFalseShouldNotUsePrefix() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment,
				"spring.environment.import.foo.locations=file:src/test/resources/additional-config/test.yaml",
				"spring.environment.import.foo.usePrefix=false");
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		assertThat(this.environment.getProperty("something")).isEqualTo("bucket");
		assertThat(this.environment.getProperty("value")).isEqualTo("1234");
		assertThat(this.environment.getProperty("my.property")).isEqualTo("fromtestproperties");
	}

}