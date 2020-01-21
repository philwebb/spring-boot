/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.cloudnativebuildpack.build;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import org.springframework.boot.cloudnativebuildpack.build.BuilderMetadata.Stack.RunImage;
import org.springframework.boot.cloudnativebuildpack.docker.type.Image;
import org.springframework.boot.cloudnativebuildpack.docker.type.ImageArchive;
import org.springframework.boot.cloudnativebuildpack.docker.type.ImageReference;
import org.springframework.boot.cloudnativebuildpack.docker.type.Layer;
import org.springframework.boot.cloudnativebuildpack.io.Content;
import org.springframework.boot.cloudnativebuildpack.io.Owner;
import org.springframework.boot.cloudnativebuildpack.toml.Toml;

/**
 * An short lived builder that is created for each {@link Lifecycle} run.
 *
 * @author Phillip Webb
 */
class EphemeralBuilder {

	private final BuildOwner buildOwner;

	private final BuilderMetadata builderMetadata;

	private final ImageArchive archive;

	/**
	 * Create a new {@link EphemeralBuilder} instance.
	 * @param buildOwner the build owner
	 * @param builderImage the image
	 * @param builderMetadata the builder metadata
	 * @param env the builder env
	 * @throws IOException on IO error
	 */
	EphemeralBuilder(BuildOwner buildOwner, Image builderImage, BuilderMetadata builderMetadata,
			Map<String, String> env) throws IOException {
		this(Clock.systemUTC(), buildOwner, builderImage, builderMetadata, env);
	}

	/**
	 * Create a new {@link EphemeralBuilder} instance with a specific clock.
	 * @param clock the clock used for the current time
	 * @param buildOwner the build owner
	 * @param builderImage the image
	 * @param builderMetadata the builder metadata
	 * @param env the builder env
	 * @throws IOException on IO error
	 */
	EphemeralBuilder(Clock clock, BuildOwner buildOwner, Image builderImage, BuilderMetadata builderMetadata,
			Map<String, String> env) throws IOException {
		ImageReference name = ImageReference.random("pack.local/builder/").inTaggedForm();
		this.buildOwner = buildOwner;
		this.builderMetadata = builderMetadata.copy(this::updateMetadata);
		this.archive = ImageArchive.from(builderImage, (update) -> {
			update.withUpdatedConfig(this.builderMetadata::attachTo);
			update.withTag(name);
			update.withCreateDate(Instant.now(clock));
			update.withNewLayer(getDefaultDirsLayer(buildOwner));
			update.withNewLayer(getStackLayer(builderMetadata));
			if (env != null && !env.isEmpty()) {
				update.withNewLayer(getEnvLayer(env));
			}
		});
	}

	private void updateMetadata(BuilderMetadata.Update update) {
		update.withCreatedBy("Spring Boot", "dev");
	}

	private Layer getDefaultDirsLayer(Owner buildOwner) throws IOException {
		return Layer.of((layout) -> {
			layout.folder("/workspace", buildOwner);
			layout.folder("/layers", buildOwner);
			layout.folder("/cnb", Owner.ROOT);
			layout.folder("/cnb/buildpacks", Owner.ROOT);
			layout.folder("/platform", Owner.ROOT);
			layout.folder("/platform/env", Owner.ROOT);
		});
	}

	private Layer getStackLayer(BuilderMetadata builderMetadata) throws IOException {
		Toml toml = getRunImageToml(builderMetadata.getStack().getRunImage());
		return Layer.of((layout) -> layout.file("/cnb/stack.toml", Owner.ROOT, Content.of(toml.toString())));
	}

	private Toml getRunImageToml(RunImage runImage) {
		Toml toml = new Toml();
		toml.table("run-image");
		toml.string("image", runImage.getImage());
		toml.array("mirrors", runImage.getMirrors());
		return toml;
	}

	private Layer getEnvLayer(Map<String, String> env) throws IOException {
		return Layer.of((layout) -> {
			for (Map.Entry<String, String> entry : env.entrySet()) {
				String name = "/platform/env/" + entry.getKey();
				Content content = Content.of(entry.getValue());
				layout.file(name, Owner.ROOT, content);
			}
		});
	}

	/**
	 * Return the name of this archive as tagged in Docker.
	 * @return the ephemeral builder name
	 */
	ImageReference getName() {
		return this.archive.getTag();
	}

	/**
	 * Return the build owner that should be used for written content.
	 * @return the builder owner
	 */
	Owner getBuildOwner() {
		return this.buildOwner;
	}

	/**
	 * Return the builder meta-data that was used to create this ephemeral builder.
	 * @return the builder meta-data
	 */
	BuilderMetadata getBuilderMetadata() {
		return this.builderMetadata;
	}

	/**
	 * Return the contents of ephemeral builder for passing to Docker.
	 * @return the ephemeral builder archive
	 */
	ImageArchive getArchive() {
		return this.archive;
	}

	@Override
	public String toString() {
		return this.archive.getTag().toString();
	}

}
