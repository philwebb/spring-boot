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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.buildpack.platform.docker.type.Layer;
import org.springframework.boot.buildpack.platform.io.Content;
import org.springframework.boot.buildpack.platform.io.FilePermissions;
import org.springframework.boot.buildpack.platform.io.Layout;
import org.springframework.boot.buildpack.platform.io.Owner;
import org.springframework.util.Assert;

/**
 * Locates buildpack in a directory on the local file system.
 *
 * The file system must contain a buildpack descriptor named {@code buildpack.toml} in the
 * root of the directory. The contents of the directory tree will be provided as a single
 * layer to be included in the builder image.
 *
 * @author Scott Frederick
 */
final class DirectoryBuildpackLocator {

	private DirectoryBuildpackLocator() {
	}

	/**
	 * Locate a buildpack as a directory on the local file system.
	 * @param buildpackReference the buildpack reference
	 * @return the buildpack or {@code null} if the reference is not a local directory
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
		if (Files.exists(path) && Files.isDirectory(path)) {
			return new DirectoryBuildpack(path);
		}
		return null;
	}

	static final class DirectoryBuildpack extends Buildpack {

		private final Path buildpackPath;

		private DirectoryBuildpack(Path buildpackPath) {
			this.descriptor = getBuildpackDescriptor(buildpackPath);
			this.buildpackPath = buildpackPath;
		}

		private BuildpackDescriptor getBuildpackDescriptor(Path buildpackPath) {
			Path descriptorPath = Paths.get(buildpackPath.toString(), "buildpack.toml");
			Assert.isTrue(Files.exists(descriptorPath),
					"Buildpack descriptor 'buildpack.toml' is required in buildpack '" + buildpackPath + "'");
			return BuildpackDescriptor.fromToml(descriptorPath, buildpackPath);
		}

		@Override
		List<Layer> getLayers() throws IOException {
			return Collections.singletonList(Layer.of((layout) -> {
				Path cnbPath = Paths.get("/cnb/buildpacks/", this.descriptor.getSanitizedId(),
						this.descriptor.getVersion());
				Files.walkFileTree(this.buildpackPath, new LayoutFileVisitor(this.buildpackPath, cnbPath, layout));
			}));
		}

	}

	static class LayoutFileVisitor extends SimpleFileVisitor<Path> {

		private final Path basePath;

		private final Path layerPath;

		private final Layout layout;

		LayoutFileVisitor(Path basePath, Path layerPath, Layout layout) {
			this.basePath = basePath;
			this.layerPath = layerPath;
			this.layout = layout;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			PosixFileAttributeView attributeView = Files.getFileAttributeView(file, PosixFileAttributeView.class);
			if (attributeView == null) {
				throw new RuntimeException(
						"Buildpack content in a directory is not supported on this operating system");
			}
			int mode = FilePermissions.posixPermissionsToUmask(attributeView.readAttributes().permissions());
			this.layout.file(relocate(file), Owner.ROOT, mode, Content.of(file.toFile()));
			return FileVisitResult.CONTINUE;
		}

		private String relocate(Path path) {
			Path node = path.subpath(this.basePath.getNameCount(), path.getNameCount());
			return Paths.get(this.layerPath.toString(), node.toString()).toString();
		}

	}

}
