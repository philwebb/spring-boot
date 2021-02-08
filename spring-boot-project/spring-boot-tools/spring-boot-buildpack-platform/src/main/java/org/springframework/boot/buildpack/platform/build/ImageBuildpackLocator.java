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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import org.springframework.boot.buildpack.platform.docker.transport.DockerEngineException;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.docker.type.Layer;

/**
 * Locates a buildpack contained in an OCI image.
 *
 * The reference must be an OCI image reference. The reference can optionally contain a
 * prefix {@code docker://} to unambiguously identify it as an image buildpack reference.
 *
 * @author Scott Frederick
 */
final class ImageBuildpackLocator {

	private static final String DOCKER_PREFIX = "docker://";

	private ImageBuildpackLocator() {
	}

	/**
	 * Locate a buildpack as an OCI image.
	 * @param buildpackReference the buildpack reference
	 * @param imageFetcher a fetcher of Docker images
	 * @param imageExporter an exporter of Docker images
	 * @return the buildpack or {@code null} if the reference is not an OCI image
	 * reference
	 */
	static Buildpack locate(String buildpackReference, ImageFetcher imageFetcher, ImageExporter imageExporter) {
		ImageReference imageReference;
		if (buildpackReference.startsWith(DOCKER_PREFIX)) {
			imageReference = ImageReference.of(buildpackReference.substring(DOCKER_PREFIX.length()));
		}
		else {
			try {
				imageReference = ImageReference.of(buildpackReference);
			}
			catch (IllegalArgumentException ex) {
				return null;
			}
		}
		return new ImageBuildpack(imageReference, imageFetcher, imageExporter);
	}

	static final class ImageBuildpack extends Buildpack {

		private final ImageReference imageReference;

		private final ImageExporter imageExporter;

		private ImageBuildpack(ImageReference imageReference, ImageFetcher imageFetcher, ImageExporter imageExporter) {
			this.imageReference = imageReference;
			this.imageExporter = imageExporter;
			try {
				Image image = imageFetcher.fetchImage(imageReference, ImageType.BUILDPACK);
				BuildpackMetadata buildpackMetadata = BuildpackMetadata.fromImage(image);
				this.descriptor = BuildpackDescriptor.of(buildpackMetadata.getId(), buildpackMetadata.getVersion());
			}
			catch (IOException | DockerEngineException ex) {
				throw new IllegalArgumentException("Error pulling buildpack image '" + imageReference + "'", ex);
			}
		}

		@Override
		List<Layer> getLayers() throws IOException {
			InputStream imageInputStream = this.imageExporter.exportImage(this.imageReference);
			TarArchiveInputStream tar = new TarArchiveInputStream(imageInputStream);
			List<Layer> layers = new ArrayList<>();
			TarArchiveEntry entry = tar.getNextTarEntry();
			while (entry != null) {
				if (entry.getName().endsWith("/layer.tar")) {
					Path layerFile = writeLayerContentToTempFile(entry, tar);
					layers.add(Layer.fromTarArchive((outputStream) -> copyLayerTar(outputStream, layerFile)));
				}
				entry = tar.getNextTarEntry();
			}
			return layers;
		}

		private Path writeLayerContentToTempFile(TarArchiveEntry entry, TarArchiveInputStream tar) throws IOException {
			String[] parts = entry.getName().split("/");
			Path tempFile = Files.createTempFile("create-builder-scratch-", parts[0]);
			IOUtils.copy(tar, Files.newOutputStream(tempFile));
			return tempFile;
		}

		private void copyLayerTar(OutputStream outputStream, Path layerTarFile) throws IOException {
			try (TarArchiveInputStream layerTar = new TarArchiveInputStream(Files.newInputStream(layerTarFile));
					TarArchiveOutputStream output = new TarArchiveOutputStream(outputStream)) {
				TarArchiveEntry entry = layerTar.getNextTarEntry();
				while (entry != null) {
					if (entry.isFile()) {
						output.putArchiveEntry(entry);
						IOUtils.copy(layerTar, output);
						output.closeArchiveEntry();
					}
					entry = layerTar.getNextTarEntry();
				}
				output.finish();
			}
			Files.delete(layerTarFile);
		}

	}

}
