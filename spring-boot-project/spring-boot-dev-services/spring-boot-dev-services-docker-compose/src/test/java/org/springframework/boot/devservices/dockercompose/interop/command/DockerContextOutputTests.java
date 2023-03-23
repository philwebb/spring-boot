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

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.boot.devservices.xdockercompose.interop.command.DockerContextOutput;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerContextOutput}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class DockerContextOutputTests {

	private final ObjectMapper objectMapper = new ObjectMapper()
		.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

	@Test
	void deserialize() throws JsonProcessingException {
		String json = """
				{"Current":true,"Description":"Current DOCKER_HOST based configuration","DockerEndpoint":"unix:///var/run/docker.sock","Error":"","KubernetesEndpoint":"","Name":"default"}
				""";
		List<DockerContextOutput> deserialized = DockerContextOutput.parse(this.objectMapper, json);
		assertThat(deserialized)
			.containsExactly(new DockerContextOutput("default", true, "unix:///var/run/docker.sock"));
	}

}
