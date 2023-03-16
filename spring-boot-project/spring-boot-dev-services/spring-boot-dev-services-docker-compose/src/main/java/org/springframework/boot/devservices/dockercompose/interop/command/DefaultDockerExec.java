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

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class DefaultDockerExec implements DockerExec {

	private final DockerInfo dockerInfo;

	private final DockerComposeInfo dockerComposeInfo;

	private final Exec exec;

	private final ObjectMapper objectMapper;

	private final Path configFile;

	DefaultDockerExec(Path configFile, DockerInfo dockerInfo, DockerComposeInfo dockerComposeInfo, Exec exec,
			ObjectMapper objectMapper) {
		this.configFile = configFile;
		this.dockerInfo = dockerInfo;
		this.dockerComposeInfo = dockerComposeInfo;
		this.exec = exec;
		this.objectMapper = objectMapper;
	}

	@Override
	public DockerInfo getDockerInfo() {
		return this.dockerInfo;
	}

	@Override
	public DockerComposeInfo getDockerComposeInfo() {
		return this.dockerComposeInfo;
	}

	@Override
	public List<DockerContextOutput> runDockerContext() {
		String json = this.exec.run(createDockerCommand("context", "ls", "--format={{ json . }}"));
		try {
			return DockerContextOutput.parse(this.objectMapper, json);
		}
		catch (JsonProcessingException ex) {
			throw new DockerOutputParseException("Failed to parse context json: '%s'".formatted(json), ex);
		}
	}

	@Override
	public List<ComposePsOutput> runComposePs() {
		String json = this.exec.run(createDockerComposeCommand("ps", "--format=json"));
		try {
			return ComposePsOutput.parse(this.objectMapper, json);
		}
		catch (JsonProcessingException ex) {
			throw new DockerOutputParseException("Failed to parse inspect json: '%s'".formatted(json), ex);
		}
	}

	@Override
	public List<DockerInspectOutput> runDockerInspect(Collection<String> ids) {
		if (ids.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> command = new ArrayList<>(createDockerCommand("inspect", "--format={{ json . }}"));
		command.addAll(ids);
		String json = this.exec.run(command);
		try {
			return DockerInspectOutput.parse(this.objectMapper, json);
		}
		catch (JsonProcessingException ex) {
			throw new DockerOutputParseException("Failed to parse inspect json: '%s'".formatted(json), ex);
		}
	}

	@Override
	public void runComposeUp() {
		this.exec.run(createDockerComposeCommand("up", "--no-color", "--quiet-pull", "--detach", "--wait"));
	}

	@Override
	public void runComposeStop(Duration timeout) {
		this.exec.run(createDockerComposeCommand("stop", "--timeout", Long.toString(timeout.toSeconds())));
	}

	@Override
	public void runComposeDown(Duration timeout) {
		this.exec.run(createDockerComposeCommand("down", "--timeout", Long.toString(timeout.toSeconds())));
	}

	@Override
	public ComposeConfigOutput runComposeConfig() {
		String json = this.exec.run(createDockerComposeCommand("config", "--format=json"));
		try {
			return ComposeConfigOutput.parse(this.objectMapper, json);
		}
		catch (JsonProcessingException ex) {
			throw new DockerOutputParseException("Failed to parse compose config json: '%s'".formatted(json), ex);
		}
	}

	private List<String> createDockerComposeCommand(String... arguments) {
		List<String> result = new ArrayList<>(this.dockerComposeInfo.executable());
		result.add("--file=" + this.configFile);
		result.add("--ansi=never");
		for (String profile : this.dockerComposeInfo.activeProfiles()) {
			result.add("--profile");
			result.add(profile);
		}
		Collections.addAll(result, arguments);
		return result;
	}

	private List<String> createDockerCommand(String... arguments) {
		List<String> result = new ArrayList<>(this.dockerInfo.executable());
		Collections.addAll(result, arguments);
		return result;
	}

}
