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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link BuildpackDescriptor}.
 *
 * @author Scott Frederick
 */
class BuildpackDescriptorTests {

	private final Path archive = Paths.get("/buildpack/path");

	@Test
	void createFromToml() {
		BuildpackDescriptor descriptor = BuildpackDescriptor
				.fromToml(createDescriptor("example/buildpack1", "0.0.1", true, false), this.archive);
		assertThat(descriptor.getId()).isEqualTo("example/buildpack1");
		assertThat(descriptor.getSanitizedId()).isEqualTo("example_buildpack1");
		assertThat(descriptor.getVersion()).isEqualTo("0.0.1");
		assertThat(descriptor.toString()).isEqualTo("example/buildpack1@0.0.1");
	}

	@Test
	void createFromTomlWithoutDescriptorThrowsException() throws Exception {
		ByteArrayInputStream descriptor = new ByteArrayInputStream("".getBytes());
		assertThatIllegalArgumentException().isThrownBy(() -> BuildpackDescriptor.fromToml(descriptor, this.archive))
				.withMessageContaining("Buildpack descriptor 'buildpack.toml' is required")
				.withMessageContaining(this.archive.toString());
	}

	@Test
	void createFromTomlWithoutIDThrowsException() throws Exception {
		InputStream descriptor = createDescriptor(null, null, true, false);
		assertThatIllegalArgumentException().isThrownBy(() -> BuildpackDescriptor.fromToml(descriptor, this.archive))
				.withMessageContaining("Buildpack descriptor must contain ID")
				.withMessageContaining(this.archive.toString());
	}

	@Test
	void createFromTomlWithoutVersionThrowsException() throws Exception {
		InputStream descriptor = createDescriptor("example/buildpack1", null, true, false);
		assertThatIllegalArgumentException().isThrownBy(() -> BuildpackDescriptor.fromToml(descriptor, this.archive))
				.withMessageContaining("Buildpack descriptor must contain version")
				.withMessageContaining(this.archive.toString());
	}

	@Test
	void createFromTomlWithoutStacksOrOrderThrowsException() throws Exception {
		InputStream descriptor = createDescriptor("example/buildpack1", "0.0.1", false, false);
		assertThatIllegalArgumentException().isThrownBy(() -> BuildpackDescriptor.fromToml(descriptor, this.archive))
				.withMessageContaining("Buildpack descriptor must contain either 'stacks' or 'order'")
				.withMessageContaining(this.archive.toString());
	}

	@Test
	void createFromTomlWithStacksAndOrderThrowsException() throws Exception {
		InputStream descriptor = createDescriptor("example/buildpack1", "0.0.1", true, true);
		assertThatIllegalArgumentException().isThrownBy(() -> BuildpackDescriptor.fromToml(descriptor, this.archive))
				.withMessageContaining("Buildpack descriptor must not contain both 'stacks' and 'order'")
				.withMessageContaining(this.archive.toString());
	}

	private InputStream createDescriptor(String id, String version, boolean includeStacks, boolean includeOrder) {
		StringBuilder builder = new StringBuilder();
		builder.append("[buildpack]\n");
		if (id != null) {
			builder.append("id = \"").append(id).append("\"\n");
		}
		if (version != null) {
			builder.append("version = \"").append(version).append("\"\n");
		}
		builder.append("name = \"Example buildpack\"\n");
		builder.append("homepage = \"https://github.com/example/example-buildpack\"\n");
		if (includeStacks) {
			builder.append("[[stacks]]\n");
			builder.append("id = \"io.buildpacks.stacks.bionic\"\n");
		}
		if (includeOrder) {
			builder.append("[[order]]\n");
			builder.append("group = [ { id = \"example/buildpack2\", version=\"0.0.2\" } ]\n");
		}
		return new ByteArrayInputStream(builder.toString().getBytes(StandardCharsets.UTF_8));
	}

}
