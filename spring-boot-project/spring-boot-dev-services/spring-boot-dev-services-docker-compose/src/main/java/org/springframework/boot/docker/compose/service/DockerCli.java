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

package org.springframework.boot.docker.compose.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.devservices.xdockercompose.interop.command.DockerNotRunningException;
import org.springframework.boot.docker.compose.service.DockerCliCommand.Type;
import org.springframework.core.log.LogMessage;

/**
 * Wrapper around {@code docker} and {@code docker-compose} command line tools.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerCli {

	private final Log logger = LogFactory.getLog(DockerCli.class);

	private final ProcessRunner processRunner;

	private final List<String> dockerCommand;

	private final List<String> dockerComposeCommand;

	private final DockerComposeFile dockerComposeFile;

	private final Set<String> activeProfiles;

	DockerCli(File workingDirectory, DockerComposeFile dockerComposeFile, Set<String> activeProfiles) {
		this.processRunner = new ProcessRunner(workingDirectory);
		this.dockerCommand = getDockerCommand(this.processRunner);
		this.dockerComposeCommand = getDockerComposeCommand(this.processRunner);
		this.dockerComposeFile = dockerComposeFile;
		this.activeProfiles = (activeProfiles != null) ? activeProfiles : Collections.emptySet();

	}

	private List<String> getDockerCommand(ProcessRunner processRunner) {
		try {
			String version = processRunner.run("docker", "version", "--format", "{{.Client.Version}}");
			this.logger.trace(LogMessage.format("Using docker %s", version));
			return List.of("docker");
		}
		catch (ProcessStartException ex) {
			throw new DockerProcessStartException("Unable to start docker process", ex);
		}
		catch (ProcessExitException ex) {
			if (ex.getStdErr().contains("docker daemon is not running")
					|| ex.getStdErr().contains("Cannot connect to the Docker daemon")) {
				throw new DockerNotRunningException(ex.getStdErr(), ex);
			}
			throw ex;

		}
	}

	private List<String> getDockerComposeCommand(ProcessRunner processRunner) {
		try {
			DockerCliComposeVersionResponse response = DockerJson.deserialize(
					processRunner.run("docker", "compose", "version", "--format", "json"),
					DockerCliComposeVersionResponse.class);
			this.logger.trace(LogMessage.format("Using docker compose $s", response.version()));
			return List.of("docker", "compose");
		}
		catch (ProcessExitException ex) {
			// Ignore and try docker-compose
		}
		try {
			DockerCliComposeVersionResponse response = DockerJson.deserialize(
					processRunner.run("docker-compose", "version", "--format", "json"),
					DockerCliComposeVersionResponse.class);
			this.logger.trace(LogMessage.format("Using docker-compose $s", response.version()));
			return List.of("docker-compose");
		}
		catch (ProcessStartException ex) {
			throw new DockerProcessStartException("Unable to start 'docker-compose' process or use 'docker compose'",
					ex);
		}
	}

	<R> R run(DockerCliCommand<R> dockerCommand) {
		List<String> command = createCommand(dockerCommand.getType());
		command.addAll(dockerCommand.getCommand());
		String json = this.processRunner.run(command.toArray(new String[0]));
		return dockerCommand.deserialize(json);
	}

	private <R> List<String> createCommand(Type type) {
		return switch (type) {
			case DOCKER -> new ArrayList<>(this.dockerCommand);
			case DOCKER_COMPOSE -> {
				List<String> result = new ArrayList<>(this.dockerComposeCommand);
				result.add("--file");
				result.add(this.dockerComposeFile.toString());
				result.add("--ansi");
				result.add("never");
				for (String profile : this.activeProfiles) {
					result.add("--profile");
					result.add(profile);
				}
				yield result;
			}
		};
	}

}
