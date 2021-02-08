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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageArchive;
import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ImageBuildpackLocator}.
 *
 * @author Scott Frederick
 */
class ImageBuildpackLocatorTests extends AbstractJsonTests {

	@Test
	void locateFullyQualifiedReference() throws Exception {
		Image image = Image.of(getContent("buildpack-image.json"));
		ImageArchive archive = ImageArchive.from(image);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		archive.writeTo(outputStream);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		Buildpack buildpack = ImageBuildpackLocator.locate("docker://example/buildpack1:latest",
				(reference, imageType) -> image, (reference) -> inputStream);
		assertThat(buildpack.getDescriptor().toString()).isEqualTo("example/hello-universe@0.0.1");
	}

	@Test
	void locateUnqualifiedReference() throws Exception {
		Image image = Image.of(getContent("buildpack-image.json"));
		ImageArchive archive = ImageArchive.from(image);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		archive.writeTo(outputStream);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		Buildpack buildpack = ImageBuildpackLocator.locate("example/buildpack1:latest", (reference, imageType) -> image,
				(reference) -> inputStream);
		assertThat(buildpack.getDescriptor().toString()).isEqualTo("example/hello-universe@0.0.1");
	}

	@Test
	void locateWhenImageNotPulledThrowsException() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(
				() -> ImageBuildpackLocator.locate("docker://example/buildpack1:latest", (reference, imageType) -> {
					throw new IOException("test");
				}, null)).withMessageContaining("Error pulling buildpack image")
				.withMessageContaining("example/buildpack1:latest");
	}

	@Test
	void locateWithMissingMetadataLabelThrowsException() throws Exception {
		Image image = Image.of(getContent("image.json"));
		assertThatIllegalArgumentException()
				.isThrownBy(() -> ImageBuildpackLocator.locate("docker://example/buildpack1:latest",
						(reference, imageType) -> image, null))
				.withMessageContaining("No 'io.buildpacks.buildpackage.metadata' label found");
	}

	@Test
	void locateFullyQualifiedReferenceWithInvalidImageReferenceThrowsException() throws Exception {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> ImageBuildpackLocator.locate("docker://buildpack@0.0.1", null, null))
				.withMessageContaining("Unable to parse image reference \"buildpack@0.0.1\"");
	}

	@Test
	void locateUnqualifiedReferenceWithInvalidImageReferenceReturnsNull() throws Exception {
		Buildpack buildpack = ImageBuildpackLocator.locate("buildpack@0.0.1", null, null);
		assertThat(buildpack).isNull();
	}

}
