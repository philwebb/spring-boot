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
import java.nio.file.Path;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import org.springframework.util.Assert;

/**
 * A descriptor that uniquely identifies a buildpack.
 *
 * @author Scott Frederick
 */
final class BuildpackDescriptor {

	private final String id;

	private final String version;

	private BuildpackDescriptor(String id, String version) {
		this.id = id;
		this.version = version;
	}

	private BuildpackDescriptor(TomlParseResult descriptor, Path buildpackPath) {
		validateBuildpackDescriptor(descriptor, buildpackPath.toString());
		this.id = descriptor.getString("buildpack.id");
		this.version = descriptor.getString("buildpack.version");
	}

	/**
	 * Return the buildpack ID.
	 * @return the ID
	 */
	String getId() {
		return this.id;
	}

	/**
	 * Return the buildpack ID with all "/" replaced by "_".
	 * @return the ID
	 */
	String getSanitizedId() {
		return this.id.replace("/", "_");
	}

	/**
	 * Return the buildpack version, if provided.
	 * @return the version
	 */
	String getVersion() {
		return this.version;
	}

	@Override
	public String toString() {
		if (this.version == null) {
			return this.id;
		}
		return this.id + "@" + this.version;
	}

	private void validateBuildpackDescriptor(TomlParseResult descriptor, String buildpackPath) {
		Assert.isTrue(!descriptor.isEmpty(),
				"Buildpack descriptor 'buildpack.toml' is required in buildpack '" + buildpackPath + "'");
		Assert.hasText(descriptor.getString("buildpack.id"),
				"Buildpack descriptor must contain ID in buildpack '" + buildpackPath + "'");
		Assert.hasText(descriptor.getString("buildpack.version"),
				"Buildpack descriptor must contain version in buildpack '" + buildpackPath + "'");
		Assert.isTrue(descriptor.contains("stacks") || descriptor.contains("order"),
				"Buildpack descriptor must contain either 'stacks' or 'order' in buildpack '" + buildpackPath + "'");
		Assert.isTrue(!(descriptor.contains("stacks") && descriptor.contains("order")),
				"Buildpack descriptor must not contain both 'stacks' and 'order' in buildpack '" + buildpackPath + "'");
	}

	/**
	 * Create a descriptor from an ID and version.
	 * @param id the buildpack ID
	 * @param version the buildpack version
	 * @return the descriptor
	 */
	static BuildpackDescriptor of(String id, String version) {
		return new BuildpackDescriptor(id, version);
	}

	/**
	 * Create a descriptor from a reference.
	 * @param reference the buildpack reference
	 * @return the descriptor
	 */
	static BuildpackDescriptor from(String reference) {
		if (reference.contains("@")) {
			String[] parts = reference.split("@");
			return new BuildpackDescriptor(parts[0], parts[1]);
		}
		else {
			return new BuildpackDescriptor(reference, null);
		}
	}

	/**
	 * Create a descriptor from a <a href=
	 * "https://github.com/buildpacks/spec/blob/main/buildpack.md#buildpacktoml-toml">{@code buildpack.toml}</a>
	 * file.
	 * @param toml an input stream containing {@code buildpack.toml} content
	 * @param buildpackPath the path to the buildpack containing the
	 * {@code buildpack.toml} file
	 * @return the descriptor
	 */
	static BuildpackDescriptor fromToml(InputStream toml, Path buildpackPath) {
		try {
			return new BuildpackDescriptor(Toml.parse(toml), buildpackPath);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Error parsing descriptor for buildpack '" + buildpackPath + "'", ex);
		}
	}

	/**
	 * Create a descriptor from a <a href=
	 * "https://github.com/buildpacks/spec/blob/main/buildpack.md#buildpacktoml-toml">{@code buildpack.toml}</a>
	 * file.
	 * @param tomlFilePath the path to a file containing {@code buildpack.toml} content
	 * @param buildpackPath the path to the buildpack containing the
	 * {@code buildpack.toml} file
	 * @return the descriptor
	 */
	static BuildpackDescriptor fromToml(Path tomlFilePath, Path buildpackPath) {
		try {
			return new BuildpackDescriptor(Toml.parse(tomlFilePath), buildpackPath);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Error parsing descriptor for buildpack '" + buildpackPath + "'", ex);
		}
	}

}
