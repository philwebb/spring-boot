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
import java.util.Optional;

/**
 * Locates a buildpack contained in the builder.
 *
 * The buildpack reference must contain a buildpack ID (for example,
 * {@code "example/buildpack"}) or a buildpack ID and version (for example,
 * {@code "example/buildpack@1.0.0"}). The reference can optionally contain a prefix
 * {@code urn:cnb:builder:} to unambiguously identify it as a builder buildpack reference.
 * If a version is not provided, the reference will match any version of a buildpack with
 * the same ID as the reference.
 *
 * @author Scott Frederick
 */
final class BuilderBuildpackLocator {

	private static final String BUILDER_BUILDPACK_PREFIX = "urn:cnb:builder:";

	private BuilderBuildpackLocator() {
	}

	/**
	 * Locate a buildpack that is present in the builder metadata.
	 * @param buildpackReference the buildpack reference
	 * @param builderBuildpacks the collection of buildpacks in the builder
	 * @return the buildpack if found in the builder, or {@code null} if the prefix is not
	 * present and the reference is not found in the builder buildpacks
	 * @throws IllegalArgumentException if the prefix is present and the buildpack is not
	 * found in the builder
	 */
	static Buildpack locate(String buildpackReference, List<BuildpackMetadata> builderBuildpacks) {
		if (buildpackReference.startsWith(BUILDER_BUILDPACK_PREFIX)) {
			BuildpackDescriptor descriptor = BuildpackDescriptor
					.from(buildpackReference.substring(BUILDER_BUILDPACK_PREFIX.length()));
			Optional<BuildpackMetadata> buildpack = findBuildpackInBuilder(descriptor, builderBuildpacks);
			return buildpack.map((bp) -> new BuilderBuildpack(bp.getId(), bp.getVersion())).orElseThrow(
					() -> new IllegalArgumentException("Buildpack '" + buildpackReference + "' not found in builder"));
		}
		BuildpackDescriptor descriptor = BuildpackDescriptor.from(buildpackReference);
		Optional<BuildpackMetadata> buildpack = findBuildpackInBuilder(descriptor, builderBuildpacks);
		return buildpack.map((bp) -> new BuilderBuildpack(bp.getId(), bp.getVersion())).orElse(null);
	}

	private static Optional<BuildpackMetadata> findBuildpackInBuilder(BuildpackDescriptor descriptor,
			List<BuildpackMetadata> builderBuildpacks) {
		return builderBuildpacks.stream()
				.filter((buildpack) -> descriptor.getId().equals(buildpack.getId())
						&& (descriptor.getVersion() == null || descriptor.getVersion().equals(buildpack.getVersion())))
				.findFirst();
	}

	static final class BuilderBuildpack extends Buildpack {

		private BuilderBuildpack(String id, String version) {
			this.descriptor = BuildpackDescriptor.of(id, version);
		}

	}

}
