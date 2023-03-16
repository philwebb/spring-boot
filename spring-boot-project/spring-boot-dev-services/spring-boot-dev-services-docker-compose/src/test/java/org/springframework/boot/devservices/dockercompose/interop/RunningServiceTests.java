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

import org.springframework.boot.devservices.dockercompose.test.RunningServiceBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RunningService}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class RunningServiceTests {

	@Test
	void image() {
		RunningService service = RunningServiceBuilder.create("service1", "service1").build();
		assertThat(service.image()).isEqualTo(DockerImageName.parse("service1"));
		assertThat(service.originalImage()).isEqualTo(DockerImageName.parse("service1"));
	}

	@Test
	void imageOverridden() {
		RunningService service = RunningServiceBuilder.create("service1", "service1")
			.addLabel("org.springframework.boot.image-override", "redis:7.0")
			.build();
		assertThat(service.image()).isEqualTo(DockerImageName.parse("redis:7.0"));
		assertThat(service.originalImage()).isEqualTo(DockerImageName.parse("service1"));
	}

	@Test
	void ignore() {
		RunningService service = RunningServiceBuilder.create("service1", "service1").build();
		assertThat(service.ignore()).isFalse();

		RunningService service2 = RunningServiceBuilder.create("service1", "service1")
			.addLabel("org.springframework.boot.ignore", "")
			.build();
		assertThat(service2.ignore()).isTrue();
	}

	@Test
	void readinessCheck() {
		RunningService service = RunningServiceBuilder.create("service1", "service1").build();
		assertThat(service.readinessCheck()).isTrue();

		RunningService service2 = RunningServiceBuilder.create("service1", "service1")
			.addLabel("org.springframework.boot.ignore", "")
			.build();
		assertThat(service2.readinessCheck()).isFalse();

		RunningService service3 = RunningServiceBuilder.create("service1", "service1")
			.addLabel("org.springframework.boot.readiness-check.disable", "")
			.build();
		assertThat(service3.readinessCheck()).isFalse();
	}

	@Test
	void origin() {
		RunningService service = RunningServiceBuilder.create("service1", "service1").build();
		assertThat(service.origin()).isNull();

		RunningService service2 = RunningServiceBuilder.create("service1", "service1")
			.composeConfigFile(Path.of("compose.yaml"))
			.build();
		DockerComposeOrigin origin = service2.origin();
		assertThat(origin).isNotNull();
		assertThat(origin.configFile()).isEqualTo(Path.of("compose.yaml"));
		assertThat(origin.serviceName()).isEqualTo("service1");
	}

}
