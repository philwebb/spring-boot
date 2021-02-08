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
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import org.springframework.boot.buildpack.platform.docker.type.Layer;

/**
 * Locates buildpack contained in a local gzipped tar archive file.
 *
 * The archive must contain a buildpack descriptor named {@code buildpack.toml} at the
 * root of the archive. The contents of the archive will be provided as a single layer to
 * be included in the builder image.
 *
 * @author Scott Frederick
 */
final class TarGzipBuildpackLocator {

	private TarGzipBuildpackLocator() {
	}

	/**
	 * Locate a buildpack as a gzipped tar archive file.
	 * @param buildpackReference the buildpack reference
	 * @return the buildpack or {@code null} if the reference is not a file
	 */
	static Buildpack locate(String buildpackReference) {
		Path path = Paths.get(buildpackReference);
		try {
			URL url = new URL(buildpackReference);
			if (url.getProtocol().equals("file")) {
				path = Paths.get(url.getPath());
			}
		}
		catch (MalformedURLException ex) {
			// not a URL, fall through to attempting to find a plain file path
		}
		if (Files.exists(path) && Files.isRegularFile(path)) {
			return new TarGzipBuildpack(path);
		}
		return null;
	}

	static final class TarGzipBuildpack extends Buildpack {

		private final Path buildpackPath;

		private TarGzipBuildpack(Path buildpackPath) {
			this.descriptor = getBuildpackDescriptor(buildpackPath);
			this.buildpackPath = buildpackPath;
		}

		@Override
		List<Layer> getLayers() throws IOException {
			return Collections.singletonList(Layer.fromTarArchive(this::copyAndRebaseEntries));
		}

		private BuildpackDescriptor getBuildpackDescriptor(Path buildpackPath) {
			try {
				try (TarArchiveInputStream tar = new TarArchiveInputStream(
						new GzipCompressorInputStream(Files.newInputStream(buildpackPath)))) {
					ArchiveEntry entry = tar.getNextEntry();
					while (entry != null) {
						if ("buildpack.toml".equals(entry.getName())) {
							return BuildpackDescriptor.fromToml(tar, buildpackPath);
						}
						entry = tar.getNextEntry();
					}
					throw new IllegalArgumentException(
							"Buildpack descriptor 'buildpack.toml' is required in buildpack '" + buildpackPath + "'");
				}
			}
			catch (IOException ex) {
				throw new RuntimeException("Error parsing descriptor for buildpack '" + buildpackPath + "'", ex);
			}
		}

		private void copyAndRebaseEntries(OutputStream outputStream) throws IOException {
			Path layerBasePath = Paths.get("/cnb/buildpacks/", this.descriptor.getSanitizedId(),
					this.descriptor.getVersion());
			try (TarArchiveInputStream input = new TarArchiveInputStream(
					new GzipCompressorInputStream(Files.newInputStream(this.buildpackPath)));
					TarArchiveOutputStream output = new TarArchiveOutputStream(outputStream)) {
				TarArchiveEntry entry = input.getNextTarEntry();
				while (entry != null) {
					entry.setName(layerBasePath + "/" + entry.getName());
					output.putArchiveEntry(entry);
					IOUtils.copy(input, output);
					output.closeArchiveEntry();
					entry = input.getNextTarEntry();
				}
				output.finish();
			}
		}

	}

}
