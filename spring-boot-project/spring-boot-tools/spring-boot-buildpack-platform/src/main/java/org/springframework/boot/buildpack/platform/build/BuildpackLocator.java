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

import java.util.List;

import org.springframework.util.Assert;

/**
 * A factory for creating a {@code Buildpack}.
 *
 * @author Scott Frederick
 */
final class BuildpackLocator {

	private BuildpackLocator() {
	}

	/**
	 * Factory method for creating a {@code Buildpack} from the provided buildpack
	 * reference.
	 *
	 * The acceptable values for a buildpack reference are determined by
	 * {@code BuildpackLocator} implementations.
	 * @param buildpackReference the buildpack reference
	 * @param builderBuildpacks the collection of buildpack exposed by the metadata in the
	 * builder image
	 * @param imageFetcher a fetcher of Docker images
	 * @param imageExporter an exporter of Docker images
	 * @return a {@code BuildpackLocator} implementation that recognizes the buildpack
	 * reference
	 * @throws IllegalArgumentException if no {@code BuildpackLocator} implementations
	 * recognize the buildpack reference
	 */
	static Buildpack from(String buildpackReference, List<BuildpackMetadata> builderBuildpacks,
			ImageFetcher imageFetcher, ImageExporter imageExporter) {
		Assert.hasText(buildpackReference, "BuildpackReference must not be empty");
		Buildpack buildpack = BuilderBuildpackLocator.locate(buildpackReference, builderBuildpacks);
		if (buildpack != null) {
			return buildpack;
		}
		buildpack = DirectoryBuildpackLocator.locate(buildpackReference);
		if (buildpack != null) {
			return buildpack;
		}
		buildpack = TarGzipBuildpackLocator.locate(buildpackReference);
		if (buildpack != null) {
			return buildpack;
		}
		buildpack = ImageBuildpackLocator.locate(buildpackReference, imageFetcher, imageExporter);
		if (buildpack != null) {
			return buildpack;
		}
		throw new IllegalArgumentException("Invalid buildpack reference '" + buildpackReference + "'");
	}

}
