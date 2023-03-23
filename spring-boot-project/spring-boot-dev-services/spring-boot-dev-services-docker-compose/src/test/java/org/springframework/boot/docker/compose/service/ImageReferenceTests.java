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

package org.springframework.boot.docker.compose.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ImageReference}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ImageReferenceTests {

	@Test
	void getImageWhenImageOnly() {
		ImageReference imageReference = ImageReference.of("redis");
		assertThat(imageReference.getImage()).isEqualTo("redis");
	}

	@Test
	void getImageWhenImageAndTag() {
		ImageReference imageReference = ImageReference.of("redis:5");
		assertThat(imageReference.getImage()).isEqualTo("redis");
	}

	@Test
	void getImageWhenImageAndDigest() {
		ImageReference imageReference = ImageReference
			.of("redis@sha256:0ed5d5928d4737458944eb604cc8509e245c3e19d02ad83935398bc4b991aac7");
		assertThat(imageReference.getImage()).isEqualTo("redis");
	}

	@Test
	void getImageWhenProjectAndImage() {
		ImageReference imageReference = ImageReference.of("library/redis");
		assertThat(imageReference.getImage()).isEqualTo("redis");
	}

	@Test
	void getImageWhenRegistryLibraryAndImage() {
		ImageReference imageReference = ImageReference.of("docker.io/library/redis");
		assertThat(imageReference.getImage()).isEqualTo("redis");
	}

	@Test
	void getImageWhenRegistryLibraryImageAndTag() {
		ImageReference imageReference = ImageReference.of("docker.io/library/redis:5");
		assertThat(imageReference.getImage()).isEqualTo("redis");
	}

	@Test
	void getImageWhenRegistryLibraryImageAndDigest() {
		ImageReference imageReference = ImageReference
			.of("docker.io/library/redis@sha256:0ed5d5928d4737458944eb604cc8509e245c3e19d02ad83935398bc4b991aac7");
		assertThat(imageReference.getImage()).isEqualTo("redis");
	}

	@Test
	void getImageWhenRegistryWithPort() {
		ImageReference imageReference = ImageReference.of("my_private.registry:5000/redis");
		assertThat(imageReference.getImage()).isEqualTo("redis");
	}

	@Test
	void getImageWhenRegistryWithPortAndTag() {
		ImageReference imageReference = ImageReference.of("my_private.registry:5000/redis:5");
		assertThat(imageReference.getImage()).isEqualTo("redis");
	}

	@Test
	void toStringReturnsReferenceString() {
		ImageReference imageReference = ImageReference.of("docker.io/library/redis");
		assertThat(imageReference).hasToString("docker.io/library/redis");
	}

	@Test
	void equalsAndHashCode() {
		ImageReference imageReference1 = ImageReference.of("docker.io/library/redis");
		ImageReference imageReference2 = ImageReference.of("docker.io/library/redis");
		ImageReference imageReference3 = ImageReference.of("docker.io/library/other");
		assertThat(imageReference1.hashCode()).isEqualTo(imageReference2.hashCode());
		assertThat(imageReference1).isEqualTo(imageReference1).isEqualTo(imageReference2).isNotEqualTo(imageReference3);
	}

}
