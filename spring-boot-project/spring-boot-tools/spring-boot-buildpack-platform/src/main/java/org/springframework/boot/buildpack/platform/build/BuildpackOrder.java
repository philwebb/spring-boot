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

import java.util.Arrays;
import java.util.List;

/**
 * A collection of buildpacks and the order in which a builder should process them.
 *
 * @author Scott Frederick
 */
final class BuildpackOrder {

	private final List<Buildpack> buildpacks;

	private BuildpackOrder(List<Buildpack> buildpacks) {
		this.buildpacks = buildpacks;
	}

	/**
	 * Return a {@code String} containing a TOML representation of the provided
	 * buildpacks, conforming to the CNB {@code order.toml} specification.
	 * @return the TOML representation
	 */
	String toTomlString() {
		StringBuilder builder = new StringBuilder();
		for (Buildpack buildpack : this.buildpacks) {
			builder.append("[[order]]\n");
			builder.append("group = [\n");
			builder.append("  { ").append(toBuildpacksString(buildpack)).append(" }\n");
			builder.append("]\n\n");
		}
		return builder.toString();
	}

	private String toBuildpacksString(Buildpack buildpack) {
		StringBuilder builder = new StringBuilder();
		builder.append("id = \"").append(buildpack.getDescriptor().getId()).append("\"");
		if (buildpack.getDescriptor().getVersion() != null) {
			builder.append(", version = \"").append(buildpack.getDescriptor().getVersion()).append("\"");
		}
		return builder.toString();
	}

	/**
	 * Create a {@code BuildpackOrder} from an array of buildpacks.
	 * @param buildpacks the buildpacks
	 * @return the created {@code BuildpackOrder}
	 */
	static BuildpackOrder of(Buildpack... buildpacks) {
		return new BuildpackOrder(Arrays.asList(buildpacks));
	}

	/**
	 * Create a {@code BuildpackOrder} from a collection of buildpacks.
	 * @param buildpacks the buildpacks
	 * @return the created {@code BuildpackOrder}
	 */
	static BuildpackOrder of(List<Buildpack> buildpacks) {
		return new BuildpackOrder(buildpacks);
	}

}
