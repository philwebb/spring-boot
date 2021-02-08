/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.buildpack.platform.build;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link BuilderBuildpackLocator}.
 *
 * @author Scott Frederick
 */
class BuilderBuildpackLocatorTests extends AbstractJsonTests {

	private BuilderMetadata metadata;

	@BeforeEach
	void setUp() throws Exception {
		this.metadata = BuilderMetadata.fromJson(getContentAsString("builder-metadata.json"));
	}

	@Test
	void locateFullyQualifiedBuildpackWithVersion() throws Exception {
		Buildpack buildpack = BuilderBuildpackLocator.locate("urn:cnb:builder:paketo-buildpacks/spring-boot@3.5.0",
				this.metadata.getBuildpacks());
		assertThat(buildpack.getDescriptor().toString()).isEqualTo("paketo-buildpacks/spring-boot@3.5.0");
		assertThat(buildpack.getLayers()).isEmpty();
	}

	@Test
	void locateFullyQualifiedBuildpackWithoutVersion() throws Exception {
		Buildpack buildpack = BuilderBuildpackLocator.locate("urn:cnb:builder:paketo-buildpacks/spring-boot",
				this.metadata.getBuildpacks());
		assertThat(buildpack.getDescriptor().toString()).isEqualTo("paketo-buildpacks/spring-boot@3.5.0");
		assertThat(buildpack.getLayers()).isEmpty();
	}

	@Test
	void locateUnqualifiedBuildpackWithVersion() throws Exception {
		Buildpack buildpack = BuilderBuildpackLocator.locate("paketo-buildpacks/spring-boot@3.5.0",
				this.metadata.getBuildpacks());
		assertThat(buildpack.getDescriptor().toString()).isEqualTo("paketo-buildpacks/spring-boot@3.5.0");
		assertThat(buildpack.getLayers()).isEmpty();
	}

	@Test
	void locateUnqualifiedBuildpackWithoutVersion() throws Exception {
		Buildpack buildpack = BuilderBuildpackLocator.locate("paketo-buildpacks/spring-boot",
				this.metadata.getBuildpacks());
		assertThat(buildpack.getDescriptor().toString()).isEqualTo("paketo-buildpacks/spring-boot@3.5.0");
		assertThat(buildpack.getLayers()).isEmpty();
	}

	@Test
	void locateFullyQualifiedBuildpackWithVersionNotInBuilderThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> BuilderBuildpackLocator.locate("urn:cnb:builder:example/buildpack1@1.2.3",
						this.metadata.getBuildpacks()))
				.withMessageContaining("'urn:cnb:builder:example/buildpack1@1.2.3'")
				.withMessageContaining("not found in builder");
	}

	@Test
	void locateFullyQualifiedBuildpackWithoutVersionNotInBuilderThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> BuilderBuildpackLocator.locate("urn:cnb:builder:example/buildpack1",
						this.metadata.getBuildpacks()))
				.withMessageContaining("'urn:cnb:builder:example/buildpack1'")
				.withMessageContaining("not found in builder");
	}

	@Test
	void locateUnqualifiedBuildpackNotInBuilderReturnsNull() {
		Buildpack buildpack = BuilderBuildpackLocator.locate("example/buildpack1@1.2.3", this.metadata.getBuildpacks());
		assertThat(buildpack).isNull();
	}

}
