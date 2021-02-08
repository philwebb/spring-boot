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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.boot.buildpack.platform.build.BuilderMetadata.Stack;
import org.springframework.boot.buildpack.platform.docker.DockerApi;
import org.springframework.boot.buildpack.platform.docker.TotalProgressEvent;
import org.springframework.boot.buildpack.platform.docker.TotalProgressPullListener;
import org.springframework.boot.buildpack.platform.docker.TotalProgressPushListener;
import org.springframework.boot.buildpack.platform.docker.UpdateListener;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfiguration;
import org.springframework.boot.buildpack.platform.docker.transport.DockerEngineException;
import org.springframework.boot.buildpack.platform.docker.type.Image;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Central API for running buildpack operations.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Andrey Shlykov
 * @since 2.3.0
 */
public class Builder {

	private final BuildLog log;

	private final DockerApi docker;

	private final DockerConfiguration dockerConfiguration;

	private BuildRequest request;

	/**
	 * Create a new builder instance with defaults.
	 */
	public Builder() {
		this(BuildLog.toSystemOut(), null, null);
	}

	private Builder(BuildLog log, DockerConfiguration dockerConfiguration, BuildRequest request) {
		this(log, dockerConfiguration, request, new DockerApi(dockerConfiguration));
	}

	private Builder(BuildLog log, DockerConfiguration dockerConfiguration, BuildRequest request, DockerApi docker) {
		this.log = log;
		this.dockerConfiguration = dockerConfiguration;
		this.request = request;
		this.docker = docker;
	}

	public void build() throws DockerEngineException, IOException {
		Assert.notNull(this.request, "Request must not be null");
		this.log.start(this.request);
		Image builderImage = getImage(this.request.getBuilder(), ImageType.BUILDER);
		BuilderMetadata builderMetadata = BuilderMetadata.fromImage(builderImage);
		BuildOwner buildOwner = BuildOwner.fromEnv(builderImage.getConfig().getEnv());
		determineRunImage(builderImage, builderMetadata.getStack());
		List<Buildpack> buildpacks = processBuildpacks(builderMetadata);
		EphemeralBuilder builder = new EphemeralBuilder(buildOwner, builderImage, builderMetadata,
				this.request.getCreator(), this.request.getEnv(), buildpacks);
		this.docker.image().load(builder.getArchive(), UpdateListener.none());
		try {
			executeLifecycle(this.request, builder);
			if (this.request.isPublish()) {
				pushImage(this.request.getName());
			}
		}
		finally {
			this.docker.image().remove(builder.getName(), true);
		}
	}

	private void determineRunImage(Image builderImage, Stack builderStack) throws IOException {
		if (this.request.getRunImage() == null) {
			ImageReference runImage = getRunImageReferenceForStack(builderStack);
			this.request = this.request.withRunImage(runImage);
		}
		Image runImage = getImage(this.request.getRunImage(), ImageType.RUNNER);
		assertStackIdsMatch(runImage, builderImage);
	}

	private ImageReference getRunImageReferenceForStack(Stack stack) {
		String name = stack.getRunImage().getImage();
		Assert.state(StringUtils.hasText(name), "Run image must be specified in the builder image stack");
		return ImageReference.of(name).inTaggedOrDigestForm();
	}

	private List<Buildpack> processBuildpacks(BuilderMetadata builderMetadata) {
		if (this.request.getBuildpacks() == null) {
			return null;
		}
		List<Buildpack> buildpacks = new ArrayList<>();
		for (String buildpack : this.request.getBuildpacks()) {
			buildpacks.add(BuildpackLocator.from(buildpack, builderMetadata.getBuildpacks(), this::getImage,
					this::exportImage));
		}
		return buildpacks;
	}

	private void executeLifecycle(BuildRequest request, EphemeralBuilder builder) throws IOException {
		try (Lifecycle lifecycle = new Lifecycle(this.log, this.docker, request, builder)) {
			lifecycle.execute();
		}
	}

	Image getImage(ImageReference imageReference, ImageType imageType) throws IOException {
		assertImageRegistriesMatch(imageReference, imageType);

		if (this.request.getPullPolicy() == PullPolicy.ALWAYS) {
			return pullImage(imageReference, imageType);
		}

		try {
			return this.docker.image().inspect(imageReference);
		}
		catch (DockerEngineException exception) {
			if (this.request.getPullPolicy() == PullPolicy.IF_NOT_PRESENT && exception.getStatusCode() == 404) {
				return pullImage(imageReference, imageType);
			}
			else {
				throw exception;
			}
		}
	}

	private Image pullImage(ImageReference reference, ImageType imageType) throws IOException {
		Consumer<TotalProgressEvent> progressConsumer = this.log.pullingImage(reference, imageType);
		TotalProgressPullListener listener = new TotalProgressPullListener(progressConsumer);
		Image image = this.docker.image().pull(reference, listener, getBuilderAuthHeader());
		this.log.pulledImage(image, imageType);
		return image;
	}

	void pushImage(ImageReference reference) throws IOException {
		Consumer<TotalProgressEvent> progressConsumer = this.log.pushingImage(reference);
		TotalProgressPushListener listener = new TotalProgressPushListener(progressConsumer);
		this.docker.image().push(reference, listener, getPublishAuthHeader());
		this.log.pushedImage(reference);
	}

	InputStream exportImage(ImageReference reference) throws IOException {
		return this.docker.image().export(reference);
	}

	private String getBuilderAuthHeader() {
		return (this.dockerConfiguration != null && this.dockerConfiguration.getBuilderRegistryAuthentication() != null)
				? this.dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader() : null;
	}

	private String getPublishAuthHeader() {
		return (this.dockerConfiguration != null && this.dockerConfiguration.getPublishRegistryAuthentication() != null)
				? this.dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader() : null;
	}

	private void assertStackIdsMatch(Image runImage, Image builderImage) {
		StackId runImageStackId = StackId.fromImage(runImage);
		StackId builderImageStackId = StackId.fromImage(builderImage);
		Assert.state(runImageStackId.equals(builderImageStackId), () -> "Run image stack '" + runImageStackId
				+ "' does not match builder stack '" + builderImageStackId + "'");
	}

	private void assertImageRegistriesMatch(ImageReference imageReference, ImageType imageType) {
		if (getBuilderAuthHeader() != null) {
			Assert.state(imageReference.getDomain().equals(this.request.getBuilder().getDomain()),
					"Builder image '" + this.request.getBuilder() + "' and " + imageType + " '" + imageReference
							+ "' must be pulled from the same authenticated registry");
		}
	}

	/**
	 * Create a new {@link Builder} with an updated {@link BuildLog}.
	 * @param log the build log
	 * @return an updated {@link Builder}
	 */
	public Builder withBuildLog(BuildLog log) {
		Assert.notNull(log, "Log must not be null");
		return new Builder(log, this.dockerConfiguration, this.request);
	}

	/**
	 * Create a new {@link Builder} with an updated {@link DockerConfiguration}.
	 * @param dockerConfiguration the Docker configuration
	 * @return an updated {@link Builder}
	 */
	public Builder withDockerConfiguration(DockerConfiguration dockerConfiguration) {
		return new Builder(this.log, dockerConfiguration, this.request);
	}

	/**
	 * Create a new {@link Builder} with an updated {@link BuildRequest}.
	 * @param request the build request
	 * @return an updated {@link Builder}
	 */
	public Builder withRequest(BuildRequest request) {
		Assert.notNull(request, "Request must not be null");
		return new Builder(this.log, this.dockerConfiguration, request);
	}

	Builder withDockerApi(DockerApi api) {
		return new Builder(this.log, this.dockerConfiguration, this.request, api);
	}

}
