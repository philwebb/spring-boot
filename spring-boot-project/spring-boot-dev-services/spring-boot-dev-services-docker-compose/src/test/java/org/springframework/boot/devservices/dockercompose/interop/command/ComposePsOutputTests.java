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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ComposePsOutput}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class ComposePsOutputTests {

	private final ObjectMapper objectMapper = new ObjectMapper()
		.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

	@Test
	void deserialize() throws JsonProcessingException {
		String json = """
				{"Command":"\\"docker-entrypoint.s…\\"","CreatedAt":"2023-02-21 13:35:10 +0100 CET","ID":"f5af31dae7f6","Image":"redis:7.0","Labels":"com.docker.compose.project.config_files=/compose.yaml,com.docker.compose.project.working_dir=/,com.docker.compose.container-number=1,com.docker.compose.image=sha256:e79ba23ed43baa22054741136bf45bdb041824f41c5e16c0033ea044ca164b82,com.docker.compose.oneoff=False,com.docker.compose.project=redis-docker,com.docker.compose.config-hash=cfdc8e119d85a53c7d47edb37a3b160a8c83ba48b0428ebc07713befec991dd0,com.docker.compose.depends_on=,com.docker.compose.service=redis,com.docker.compose.version=2.16.0","LocalVolumes":"1","Mounts":"9edc7fa2fe6c9e…","Names":"redis-docker-redis-1","Networks":"redis-docker_default","Ports":"0.0.0.0:32770-\\u003e6379/tcp, :::32770-\\u003e6379/tcp","RunningFor":"2 days ago","Size":"0B","State":"running","Status":"Up 3 seconds"}
				""";
		List<ComposePsOutput> deserialized = ComposePsOutput.parse(this.objectMapper, json);
		assertThat(deserialized).containsExactly(new ComposePsOutput("f5af31dae7f6", "running"));
	}

	@Test
	void running() {
		assertThat(new ComposePsOutput("f5af31dae7f6", "running").running()).isTrue();
		assertThat(new ComposePsOutput("f5af31dae7f6", "exited").running()).isFalse();
	}

}
