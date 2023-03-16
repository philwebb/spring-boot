/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.devservices.dockercompose.interop;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.devservices.dockercompose.configuration.StopMode;
import org.springframework.boot.devservices.dockercompose.interop.ServiceMapper.DockerEnvironment;
import org.springframework.boot.devservices.dockercompose.interop.command.ComposeConfigOutput;
import org.springframework.boot.devservices.dockercompose.interop.command.ComposePsOutput;
import org.springframework.boot.devservices.dockercompose.interop.command.DockerContextOutput;
import org.springframework.boot.devservices.dockercompose.interop.command.DockerExec;
import org.springframework.boot.devservices.dockercompose.interop.command.DockerInspectOutput;
import org.springframework.core.log.LogMessage;

/**
 * Default implementation of {@link DockerCompose}. Uses {@link DockerExec} to invoke
 * docker.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class DefaultDockerCompose implements DockerCompose {

	private static final Log logger = LogFactory.getLog(DefaultDockerCompose.class);

	private static final Duration DEFAULT_STOP_TIMEOUT = Duration.ofSeconds(10);

	private final Path configFile;

	private final String dockerHostname;

	private final DockerExec dockerExec;

	/**
	 * Creates a new instance.
	 * @param configFile path to the docker compose config file
	 * @param dockerExec docker execution
	 * @param dockerHostname hostname of the docker daemon or {@code null}
	 */
	DefaultDockerCompose(Path configFile, DockerExec dockerExec, String dockerHostname) {
		this.dockerHostname = dockerHostname;
		this.configFile = configFile.toAbsolutePath();
		this.dockerExec = dockerExec;
		logDockerInfo();
	}

	private void logDockerInfo() {
		logger.info(LogMessage.format("Docker: %s", this.dockerExec.getDockerInfo()));
		logger.info(LogMessage.format("Docker compose: %s", this.dockerExec.getDockerComposeInfo()));
	}

	@Override
	public List<RunningService> listRunningServices() {
		DockerContextOutput context = getCurrentDockerContext();
		List<ComposePsOutput> runningServices = this.dockerExec.runComposePs()
			.stream()
			.filter(ComposePsOutput::running)
			.toList();
		List<DockerInspectOutput> inspects = this.dockerExec
			.runDockerInspect(runningServices.stream().map(ComposePsOutput::id).toList());
		return new ServiceMapper(DockerEnvironment.load(this.dockerHostname, context)).map(inspects, this.configFile);
	}

	@Override
	public List<DefinedService> listDefinedServices() {
		ComposeConfigOutput config = this.dockerExec.runComposeConfig();
		return config.services().keySet().stream().map(DefinedService::new).toList();
	}

	@Override
	public void startServices() {
		this.dockerExec.runComposeUp();
	}

	@Override
	public void stopServices(StopMode stopMode) {
		switch (stopMode) {
			case STOP -> this.dockerExec.runComposeStop(DEFAULT_STOP_TIMEOUT);
			case FORCE_STOP -> this.dockerExec.runComposeStop(Duration.ZERO);
			case DOWN -> this.dockerExec.runComposeDown(DEFAULT_STOP_TIMEOUT);
			case FORCE_DOWN -> this.dockerExec.runComposeDown(Duration.ZERO);
			default -> throw new IllegalStateException("Unexpected value: " + stopMode);
		}
	}

	@Override
	public boolean isRunning(List<DefinedService> definedServices, List<RunningService> runningServices) {
		if (definedServices.isEmpty()) {
			return true;
		}
		return !runningServices.isEmpty();
	}

	private DockerContextOutput getCurrentDockerContext() {
		List<DockerContextOutput> contexts = this.dockerExec.runDockerContext();
		for (DockerContextOutput context : contexts) {
			if (context.current()) {
				return context;
			}
		}
		throw new IllegalStateException("No current context found in %s".formatted(contexts));
	}

}
