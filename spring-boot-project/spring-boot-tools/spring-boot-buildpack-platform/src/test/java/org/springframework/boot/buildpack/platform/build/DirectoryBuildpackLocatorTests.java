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
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Tests for {@link DirectoryBuildpackLocator}.
 *
 * @author Scott Frederick
 */
class DirectoryBuildpackLocatorTests {

	@TempDir
	File temp;

	private File buildpackDirectory;

	@BeforeEach
	void setUp() {
		this.buildpackDirectory = new File(this.temp, "buildpack");
		this.buildpackDirectory.mkdirs();
	}

	@Test
	void locateDirectory() throws Exception {
		writeBuildpackDescriptor();
		writeScripts();
		Buildpack buildpack = DirectoryBuildpackLocator.locate(this.buildpackDirectory.toString());
		assertThat(buildpack).isNotNull();
		assertThat(buildpack.getDescriptor().toString()).isEqualTo("example/buildpack1@0.0.1");
		assertThat(buildpack.getLayers()).hasSize(1);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		buildpack.getLayers().get(0).writeTo(outputStream);
		try (TarArchiveInputStream tarStream = new TarArchiveInputStream(
				new ByteArrayInputStream(outputStream.toByteArray()))) {
			TarArchiveEntry entry;
			List<TarArchiveEntry> entries = new ArrayList<>();
			while ((entry = tarStream.getNextTarEntry()) != null) {
				entries.add(entry);
			}
			assertThat(entries).extracting("name", "mode").containsExactlyInAnyOrder(
					tuple("/cnb/buildpacks/example_buildpack1/0.0.1/buildpack.toml", 0644),
					tuple("/cnb/buildpacks/example_buildpack1/0.0.1/bin/detect", 0744),
					tuple("/cnb/buildpacks/example_buildpack1/0.0.1/bin/build", 0744));
		}
	}

	@Test
	void locateDirectoryAsUrl() throws Exception {
		writeBuildpackDescriptor();
		writeScripts();
		Buildpack buildpack = DirectoryBuildpackLocator.locate("file://" + this.buildpackDirectory.toString());
		assertThat(buildpack).isNotNull();
		assertThat(buildpack.getDescriptor().toString()).isEqualTo("example/buildpack1@0.0.1");
		assertThat(buildpack.getLayers()).hasSize(1);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		buildpack.getLayers().get(0).writeTo(outputStream);
		try (TarArchiveInputStream tarStream = new TarArchiveInputStream(
				new ByteArrayInputStream(outputStream.toByteArray()))) {
			TarArchiveEntry entry;
			List<TarArchiveEntry> entries = new ArrayList<>();
			while ((entry = tarStream.getNextTarEntry()) != null) {
				entries.add(entry);
			}
			assertThat(entries).extracting("name", "mode").containsExactlyInAnyOrder(
					tuple("/cnb/buildpacks/example_buildpack1/0.0.1/buildpack.toml", 0644),
					tuple("/cnb/buildpacks/example_buildpack1/0.0.1/bin/detect", 0744),
					tuple("/cnb/buildpacks/example_buildpack1/0.0.1/bin/build", 0744));
		}
	}

	@Test
	void locateDirectoryWithoutDescriptorThrowsException() throws Exception {
		Files.createDirectories(this.buildpackDirectory.toPath());
		assertThatIllegalArgumentException()
				.isThrownBy(() -> DirectoryBuildpackLocator.locate(this.buildpackDirectory.toString()))
				.withMessageContaining("Buildpack descriptor 'buildpack.toml' is required")
				.withMessageContaining(this.buildpackDirectory.getAbsolutePath());
	}

	@Test
	void locateDirectoryWithFileReturnsNull() throws Exception {
		Path file = Files.createFile(Paths.get(this.buildpackDirectory.toString(), "test"));
		Buildpack buildpack = DirectoryBuildpackLocator.locate(file.toString());
		assertThat(buildpack).isNull();
	}

	@Test
	void locateDirectoryThatDoesNotExistReturnsNull() {
		Buildpack buildpack = DirectoryBuildpackLocator.locate("/test/buildpack");
		assertThat(buildpack).isNull();
	}

	@Test
	void locateDirectoryAsUrlThatDoesNotExistThrowsException() {
		Buildpack buildpack = DirectoryBuildpackLocator.locate("file://test/buildpack");
		assertThat(buildpack).isNull();
	}

	private void writeBuildpackDescriptor() throws IOException {
		File descriptor = new File(this.buildpackDirectory, "buildpack.toml");
		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(descriptor.toPath()))) {
			writer.println("[buildpack]");
			writer.println("id = \"example/buildpack1\"");
			writer.println("version = \"0.0.1\"");
			writer.println("name = \"Example buildpack\"");
			writer.println("homepage = \"https://github.com/example/example-buildpack\"");
			writer.println("[[stacks]]");
			writer.println("id = \"io.buildpacks.stacks.bionic\"");
		}
	}

	private void writeScripts() throws IOException {
		File binDirectory = new File(this.buildpackDirectory, "bin");
		binDirectory.mkdirs();

		Path detect = Files.createFile(Paths.get(binDirectory.getAbsolutePath(), "detect"),
				PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr--r--")));
		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(detect))) {
			writer.println("#!/usr/bin/env bash");
			writer.println("echo \"---> detect\"");
		}
		Path build = Files.createFile(Paths.get(binDirectory.getAbsolutePath(), "build"),
				PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr--r--")));
		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(build))) {
			writer.println("#!/usr/bin/env bash");
			writer.println("echo \"---> build\"");
		}
	}

}
