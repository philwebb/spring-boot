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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerComposeOrigin}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class DockerComposeOriginTests {

	@Test
	void hasToString() {
		DockerComposeOrigin origin = new DockerComposeOrigin(Path.of("compose.yaml"), "service-1");
		assertThat(origin).hasToString("docker compose service 'service-1' defined in 'compose.yaml'");
	}

	@Test
	void equalsAndHashcode() {
		DockerComposeOrigin origin1 = new DockerComposeOrigin(Path.of("compose.yaml"), "service-1");
		DockerComposeOrigin origin2 = new DockerComposeOrigin(Path.of("compose.yaml"), "service-1");
		DockerComposeOrigin origin3 = new DockerComposeOrigin(Path.of("compose.yaml"), "service-3");
		assertThat(origin1).isEqualTo(origin1);
		assertThat(origin1).isEqualTo(origin2);
		assertThat(origin1).hasSameHashCodeAs(origin2);
		assertThat(origin2).isEqualTo(origin1);
		assertThat(origin1).isNotEqualTo(origin3);
		assertThat(origin2).isNotEqualTo(origin3);
		assertThat(origin3).isNotEqualTo(origin1);
		assertThat(origin3).isNotEqualTo(origin2);
	}

}
