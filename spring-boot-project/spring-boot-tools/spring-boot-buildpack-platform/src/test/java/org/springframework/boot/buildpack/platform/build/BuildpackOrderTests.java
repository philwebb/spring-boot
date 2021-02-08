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
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BuildpackOrder}.
 *
 * @author Scott Frederick
 */
class BuildpackOrderTests {

	@Test
	void toTomlStringWithEmptyOrder() {
		BuildpackOrder order = BuildpackOrder.of();
		assertThat(order.toTomlString()).isEmpty();
	}

	@Test
	void toTomlStringWithoutVersions() throws IOException {
		BuildpackOrder order = BuildpackOrder.of(new TestBuildpack("example/buildpack1"),
				new TestBuildpack("example/buildpack2"), new TestBuildpack("example/buildpack3"));
		assertTomlContent(order.toTomlString(), "order.toml");
	}

	@Test
	void toTomlStringWithVersions() throws IOException {
		BuildpackOrder order = BuildpackOrder.of(new TestBuildpack("example/buildpack1@0.0.1"),
				new TestBuildpack("example/buildpack2@0.0.2"), new TestBuildpack("example/buildpack3@0.0.3"));
		assertTomlContent(order.toTomlString(), "order-versions.toml");
	}

	private void assertTomlContent(String content, String fileName) {
		InputStream resource = getClass().getResourceAsStream(fileName);
		assertThat(resource).hasContent(content);
	}

	private static class TestBuildpack extends Buildpack {

		TestBuildpack(String reference) {
			this.descriptor = BuildpackDescriptor.from(reference);
		}

	}

}
