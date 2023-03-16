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

import org.springframework.boot.devservices.dockercompose.configuration.DockerComposeDevServiceConfigurationProperties;
import org.springframework.boot.testsupport.testcontainers.DisabledIfDockerUnavailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultDockerComposeFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
@DisabledIfDockerUnavailable
class DefaultDockerComposeFactoryTests {

	@Test
	void create() {
		DockerComposeDevServiceConfigurationProperties configuration = new DockerComposeDevServiceConfigurationProperties();
		configuration.setDockerHostname("my-machine-name");
		DockerCompose dockerCompose = new DefaultDockerComposeFactory().create(configuration, Path.of("compose.yaml"));
		assertThat(dockerCompose).isInstanceOf(DefaultDockerCompose.class);
		assertThat(dockerCompose).extracting("dockerHostname").isEqualTo("my-machine-name");
	}

}
