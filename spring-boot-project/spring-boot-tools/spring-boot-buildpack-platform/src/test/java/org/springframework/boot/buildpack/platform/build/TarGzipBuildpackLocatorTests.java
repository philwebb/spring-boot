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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link TarGzipBuildpackLocator}.
 *
 * @author Scott Frederick
 */
class TarGzipBuildpackLocatorTests {

	@TempDir
	File temp;

	private File buildpackDirectory;

	@BeforeEach
	void setUp() {
		this.buildpackDirectory = new File(this.temp, "buildpack");
		this.buildpackDirectory.mkdirs();
	}

	@Test
	void locateArchive() throws Exception {
		Path buildpackArchive = createBuildpackArchive();
		writeBuildpackContentToArchive(buildpackArchive);
		Path compressedArchive = compressBuildpackArchive(buildpackArchive);
		Buildpack buildpack = TarGzipBuildpackLocator.locate(compressedArchive.toString());
		assertThat(buildpack).isNotNull();
		assertThat(buildpack.getDescriptor().toString()).isEqualTo("example/buildpack1@0.0.1");
		assertThat(buildpack.getLayers()).hasSize(1);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		buildpack.getLayers().get(0).writeTo(outputStream);
		TarArchiveInputStream tarStream = new TarArchiveInputStream(
				new ByteArrayInputStream(outputStream.toByteArray()));
		assertThat(tarStream.getNextEntry().getName())
				.isEqualTo("cnb/buildpacks/example_buildpack1/0.0.1/buildpack.toml");
		assertThat(tarStream.getNextEntry().getName()).isEqualTo("cnb/buildpacks/example_buildpack1/0.0.1/bin/detect");
		assertThat(tarStream.getNextEntry().getName()).isEqualTo("cnb/buildpacks/example_buildpack1/0.0.1/bin/build");
		assertThat(tarStream.getNextEntry()).isNull();
	}

	@Test
	void locateArchiveAsUrl() throws Exception {
		Path buildpackArchive = createBuildpackArchive();
		writeBuildpackContentToArchive(buildpackArchive);
		Path compressedArchive = compressBuildpackArchive(buildpackArchive);
		Buildpack buildpack = TarGzipBuildpackLocator.locate("file://" + compressedArchive.toString());
		assertThat(buildpack).isNotNull();
		assertThat(buildpack.getDescriptor().toString()).isEqualTo("example/buildpack1@0.0.1");
		assertThat(buildpack.getLayers()).hasSize(1);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		buildpack.getLayers().get(0).writeTo(outputStream);
		TarArchiveInputStream tarStream = new TarArchiveInputStream(
				new ByteArrayInputStream(outputStream.toByteArray()));
		assertThat(tarStream.getNextEntry().getName())
				.isEqualTo("cnb/buildpacks/example_buildpack1/0.0.1/buildpack.toml");
		assertThat(tarStream.getNextEntry().getName()).isEqualTo("cnb/buildpacks/example_buildpack1/0.0.1/bin/detect");
		assertThat(tarStream.getNextEntry().getName()).isEqualTo("cnb/buildpacks/example_buildpack1/0.0.1/bin/build");
		assertThat(tarStream.getNextEntry()).isNull();
	}

	@Test
	void locateArchiveWithoutDescriptorThrowsException() throws Exception {
		Path compressedArchive = compressBuildpackArchive(createBuildpackArchive());
		assertThatIllegalArgumentException()
				.isThrownBy(() -> TarGzipBuildpackLocator.locate(compressedArchive.toString()))
				.withMessageContaining("Buildpack descriptor 'buildpack.toml' is required")
				.withMessageContaining(compressedArchive.toString());
	}

	@Test
	void locateArchiveWithDirectoryReturnsNull() {
		Buildpack buildpack = TarGzipBuildpackLocator.locate(this.buildpackDirectory.getAbsolutePath());
		assertThat(buildpack).isNull();
	}

	@Test
	void locateArchiveThatDoesNotExistReturnsNull() {
		Buildpack buildpack = TarGzipBuildpackLocator.locate("/test/buildpack.tar");
		assertThat(buildpack).isNull();
	}

	private Path createBuildpackArchive() throws Exception {
		Path tarPath = Paths.get(this.buildpackDirectory.getAbsolutePath(), "buildpack.tar");
		return Files.createFile(tarPath);
	}

	private Path compressBuildpackArchive(Path archive) throws Exception {
		Path tgzPath = Paths.get(this.buildpackDirectory.getAbsolutePath(), "buildpack.tgz");
		FileCopyUtils.copy(Files.newInputStream(archive),
				new GzipCompressorOutputStream(Files.newOutputStream(tgzPath)));
		return tgzPath;
	}

	private void writeBuildpackContentToArchive(Path archive) throws Exception {
		String descriptor = "[buildpack]\n" + "id = \"example/buildpack1\"\n" + "version = \"0.0.1\"\n"
				+ "name = \"Example buildpack\"\n" + "homepage = \"https://github.com/example/example-buildpack\"\n"
				+ "[[stacks]]\n" + "id = \"io.buildpacks.stacks.bionic\"\n";
		String detectScript = "#!/usr/bin/env bash\n" + "echo \"---> detect\"\n";
		String buildScript = "#!/usr/bin/env bash\n" + "echo \"---> build\"\n";
		try (TarArchiveOutputStream tar = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
			writeStringEntryToArchive(tar, "buildpack.toml", descriptor);
			writeStringEntryToArchive(tar, "bin/detect", detectScript);
			writeStringEntryToArchive(tar, "bin/build", buildScript);
			tar.finish();
		}
	}

	private void writeStringEntryToArchive(TarArchiveOutputStream tar, String entryName, String content)
			throws IOException {
		TarArchiveEntry entry = new TarArchiveEntry(entryName);
		entry.setSize(content.length());
		tar.putArchiveEntry(entry);
		IOUtils.copy(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), tar);
		tar.closeArchiveEntry();
	}

}
