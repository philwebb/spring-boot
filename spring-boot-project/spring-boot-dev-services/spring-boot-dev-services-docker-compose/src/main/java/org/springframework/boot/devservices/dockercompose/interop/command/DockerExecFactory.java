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

package org.springframework.boot.devservices.dockercompose.interop.command;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Factory for {@link DockerExec} instances.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public final class DockerExecFactory {

	private DockerExecFactory() {
	}

	/**
	 * Creates a {@link DockerExec} instance.
	 * @param configFile docker compose config file
	 * @param dockerComposeProfiles the active docker compose profiles
	 * @return {@link DockerExec} instance
	 * @throws DockerNotRunningException if docker is not running
	 * @throws DockerNotInstalledException if docker is not installed
	 */
	public static DockerExec create(Path configFile, Set<String> dockerComposeProfiles) {
		ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		Exec exec = new Exec(configFile.toAbsolutePath().getParent());
		DockerInfo dockerInfo = fetchDockerInfo(exec);
		DockerComposeInfo dockerComposeInfo = fetchDockerComposeInfo(exec, objectMapper, dockerComposeProfiles);
		return new DefaultDockerExec(configFile.toAbsolutePath(), dockerInfo, dockerComposeInfo, exec, objectMapper);
	}

	private static DockerInfo fetchDockerInfo(Exec exec) {
		try {
			String version = exec.run("docker", "version", "--format", "{{.Client.Version}}");
			return new DockerInfo(List.of("docker"), version.trim());
		}
		catch (ExecException ex) {
			if (ex.getStrErr().contains("docker daemon is not running")
					|| ex.getStrErr().contains("Cannot connect to the Docker daemon")) {
				throw new DockerNotRunningException(ex.getStrErr(), ex);
			}
			if (ex.getCause() instanceof IOException cause && cause.getMessage().startsWith("Cannot run program")) {
				throw new DockerNotInstalledException("No 'docker' command found", ex);
			}
			throw ex;
		}
	}

	private static DockerComposeInfo fetchDockerComposeInfo(Exec exec, ObjectMapper objectMapper,
			Set<String> dockerComposeProfiles) {
		try {
			String json = exec.run("docker", "compose", "version", "--format", "json");
			try {
				ComposeVersionOutput versionDto = ComposeVersionOutput.parse(objectMapper, json);
				return new DockerComposeInfo(List.of("docker", "compose"), versionDto.version(), dockerComposeProfiles);
			}
			catch (JsonProcessingException ex) {
				throw new DockerOutputParseException(
						"Failed to parse docker compose version output: '%s'".formatted(json), ex);
			}
		}
		catch (ExecException ex) {
			// Do nothing
		}
		try {
			String json = exec.run("docker-compose", "version", "--format", "json");
			try {
				ComposeVersionOutput versionDto = ComposeVersionOutput.parse(objectMapper, json);
				return new DockerComposeInfo(List.of("docker-compose"), versionDto.version(), dockerComposeProfiles);
			}
			catch (JsonProcessingException ex) {
				throw new DockerOutputParseException(
						"Failed to parse docker-compose version output: '%s'".formatted(json), ex);
			}
		}
		catch (ExecException ex) {
			// Do nothing
		}
		throw new DockerNotInstalledException("No 'docker compose' or 'docker-compose' command found!");
	}

}
