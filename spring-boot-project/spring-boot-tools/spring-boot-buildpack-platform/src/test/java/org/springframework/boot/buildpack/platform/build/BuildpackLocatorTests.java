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

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link BuildpackLocator}.
 *
 * @author Scott Frederick
 */
class BuildpackLocatorTests extends AbstractJsonTests {

	@Test
	void fromWithBuilderBuildpack() throws IOException {
		BuilderMetadata metadata = BuilderMetadata.fromJson(getContentAsString("builder-metadata.json"));
		Buildpack locator = BuildpackLocator.from("urn:cnb:builder:paketo-buildpacks/spring-boot@3.5.0",
				metadata.getBuildpacks(), null, null);
		assertThat(locator).isNotNull();
	}

	@Test
	void fromWithInvalidLocatorThrowsException() throws IOException {
		BuilderMetadata metadata = BuilderMetadata.fromJson(getContentAsString("builder-metadata.json"));
		assertThatIllegalArgumentException()
				.isThrownBy(
						() -> BuildpackLocator.from("unknown-buildpack@0.0.1", metadata.getBuildpacks(), null, null))
				.withMessageContaining("Invalid buildpack reference")
				.withMessageContaining("'unknown-buildpack@0.0.1'");
	}

}
