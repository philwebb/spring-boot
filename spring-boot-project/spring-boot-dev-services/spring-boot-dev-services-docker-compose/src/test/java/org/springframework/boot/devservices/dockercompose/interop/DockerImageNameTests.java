/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.devservices.dockercompose.interop;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ImageReference}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class DockerImageNameTests {

	@Test
	void imageOnly() {
		assertThat(ImageReference.parse("redis")).isEqualTo(new ImageReference(null, "redis", null));
	}

	@Test
	void imageAndTag() {
		assertThat(ImageReference.parse("redis:5")).isEqualTo(new ImageReference(null, "redis", "5"));
	}

	@Test
	void imageAndDigest() {
		assertThat(
				ImageReference.parse("redis@sha256:0ed5d5928d4737458944eb604cc8509e245c3e19d02ad83935398bc4b991aac7"))
			.isEqualTo(new ImageReference(null, "redis", null));
	}

	@Test
	void projectAndImage() {
		assertThat(ImageReference.parse("library/redis")).isEqualTo(new ImageReference("library", "redis", null));
	}

	@Test
	void registryLibraryAndImage() {
		assertThat(ImageReference.parse("docker.io/library/redis"))
			.isEqualTo(new ImageReference("library", "redis", null));
	}

	@Test
	void registryLibraryImageAndTag() {
		assertThat(ImageReference.parse("docker.io/library/redis:5"))
			.isEqualTo(new ImageReference("library", "redis", "5"));
	}

	@Test
	void registryLibraryImageAndDigest() {
		assertThat(ImageReference
			.parse("docker.io/library/redis@sha256:0ed5d5928d4737458944eb604cc8509e245c3e19d02ad83935398bc4b991aac7"))
			.isEqualTo(new ImageReference("library", "redis", null));
	}

	@Test
	void registryWithPort() {
		assertThat(ImageReference.parse("my_private.registry:5000/redis"))
			.isEqualTo(new ImageReference(null, "redis", null));
	}

	@Test
	void registryWithPortAndTag() {
		assertThat(ImageReference.parse("my_private.registry:5000/redis:5"))
			.isEqualTo(new ImageReference(null, "redis", "5"));
	}

}
