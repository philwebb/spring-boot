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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.devservices.dockercompose.interop.Port.Protocol;
import org.springframework.boot.devservices.dockercompose.interop.ServiceMapper.DockerEnvironment;
import org.springframework.boot.devservices.dockercompose.interop.command.DockerContextOutput;
import org.springframework.boot.devservices.dockercompose.interop.command.DockerInspectOutput;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServiceMapper}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class ServiceMapperTests {

	private final ObjectMapper objectMapper = new ObjectMapper()
		.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

	@Test
	void map() throws Exception {
		ServiceMapper serviceMapper = new ServiceMapper(linuxEnvironment());
		RunningService service1 = mapToSingleService(serviceMapper, "/docker/inspect.json");
		assertThat(service1.name()).isEqualTo("redis");
		assertThat(service1.image()).isEqualTo(DockerImageName.parse("redis:7.0"));
		assertThat(service1.ignore()).isFalse();
		assertThat(service1.readinessCheck()).isTrue();
		assertThat(service1.env()).isEqualTo(Map.of("GOSU_VERSION", "1.16", "REDIS_VERSION", "7.0.8"));
		assertThat(service1.composeConfigFile()).isEqualTo(Path.of("compose.yaml"));
		assertThat(service1.origin()).isNotNull();
		assertThat(service1.labels()).isEqualTo(Map.of("com.docker.compose.config-hash",
				"cfdc8e119d85a53c7d47edb37a3b160a8c83ba48b0428ebc07713befec991dd0",
				"com.docker.compose.container-number", "1", "com.docker.compose.depends_on", "",
				"com.docker.compose.image", "sha256:e79ba23ed43baa22054741136bf45bdb041824f41c5e16c0033ea044ca164b82",
				"com.docker.compose.oneoff", "False", "com.docker.compose.project", "redis-docker",
				"com.docker.compose.project.config_files", "/compose.yaml", "com.docker.compose.project.working_dir",
				"/", "com.docker.compose.service", "redis", "com.docker.compose.version", "2.16.0"));
	}

	@Test
	void bridgeNetwork() throws Exception {
		ServiceMapper serviceMapper = new ServiceMapper(linuxEnvironment());
		RunningService service1 = mapToSingleService(serviceMapper, "/docker/inspect-bridge-network.json");
		assertThat(service1.host()).isEqualTo("127.0.0.1");
		assertThat(service1.ports()).isEqualTo(Map.of(6379, new Port(32770, Protocol.TCP)));
	}

	@Test
	void bridgeNetworkWindows() throws Exception {
		ServiceMapper serviceMapper = new ServiceMapper(windowsEnvironment());
		RunningService service1 = mapToSingleService(serviceMapper, "/docker/inspect-bridge-network.json");
		assertThat(service1.host()).isEqualTo("127.0.0.1");
		assertThat(service1.ports()).isEqualTo(Map.of(6379, new Port(32770, Protocol.TCP)));
	}

	@Test
	void bridgeNetworkMac() throws Exception {
		ServiceMapper serviceMapper = new ServiceMapper(macEnvironment());
		RunningService service1 = mapToSingleService(serviceMapper, "/docker/inspect-bridge-network.json");
		assertThat(service1.host()).isEqualTo("127.0.0.1");
		assertThat(service1.ports()).isEqualTo(Map.of(6379, new Port(32770, Protocol.TCP)));
	}

	@Test
	void bridgeNetworkWsl() throws Exception {
		ServiceMapper serviceMapper = new ServiceMapper(wslEnvironment());
		RunningService service1 = mapToSingleService(serviceMapper, "/docker/inspect-bridge-network.json");
		assertThat(service1.host()).isEqualTo("127.0.0.1");
		assertThat(service1.ports()).isEqualTo(Map.of(6379, new Port(32770, Protocol.TCP)));
	}

	@Test
	void hostNetwork() throws Exception {
		ServiceMapper serviceMapper = new ServiceMapper(linuxEnvironment());
		RunningService service1 = mapToSingleService(serviceMapper, "/docker/inspect-host-network.json");
		assertThat(service1.host()).isEqualTo("127.0.0.1");
		assertThat(service1.ports()).isEqualTo(Map.of(6379, new Port(6379, Protocol.TCP)));
	}

	@Test
	void dockerHostNameOverride() throws Exception {
		DockerEnvironment environment = new DockerEnvironment(linuxDockerContext(), "192.168.1.1", null, null);
		ServiceMapper serviceMapper = new ServiceMapper(environment);
		RunningService service1 = mapToSingleService(serviceMapper, "/docker/inspect-bridge-network.json");
		assertThat(service1.host()).isEqualTo("192.168.1.1");
	}

	@Test
	void servicesHostOverride() throws Exception {
		DockerEnvironment environment = new DockerEnvironment(linuxDockerContext(), null, "192.168.1.1", null);
		ServiceMapper serviceMapper = new ServiceMapper(environment);
		RunningService service1 = mapToSingleService(serviceMapper, "/docker/inspect-bridge-network.json");
		assertThat(service1.host()).isEqualTo("192.168.1.1");
	}

	@Test
	void dockerHostOverride() throws Exception {
		DockerEnvironment environment = new DockerEnvironment(linuxDockerContext(), null, null,
				"tcp://192.168.1.1:3267");
		ServiceMapper serviceMapper = new ServiceMapper(environment);
		RunningService service1 = mapToSingleService(serviceMapper, "/docker/inspect-bridge-network.json");
		assertThat(service1.host()).isEqualTo("192.168.1.1");
	}

	@Test
	void dockerContextWithHostname() throws Exception {
		DockerContextOutput currentContext = new DockerContextOutput("default", true, "https://192.168.1.1:3267");
		DockerEnvironment environment = new DockerEnvironment(currentContext, null, null, null);
		ServiceMapper serviceMapper = new ServiceMapper(environment);
		RunningService service1 = mapToSingleService(serviceMapper, "/docker/inspect-bridge-network.json");
		assertThat(service1.host()).isEqualTo("192.168.1.1");
	}

	@Test
	void loadDockerEnvironment() {
		DockerEnvironment dockerEnvironment = DockerEnvironment.load("192.168.1.1", linuxDockerContext());
		assertThat(dockerEnvironment.dockerHostname()).isEqualTo("192.168.1.1");
	}

	private DockerEnvironment linuxEnvironment() {
		DockerContextOutput currentContext = new DockerContextOutput("default", true, "unix:///var/run/docker.sock");
		return new DockerEnvironment(currentContext, null, null, null);
	}

	private RunningService mapToSingleService(ServiceMapper serviceMapper, String jsonResource) throws IOException {
		DockerInspectOutput dockerInspectOutput;
		try (InputStream stream = ServiceMapperTests.class.getResourceAsStream(jsonResource)) {
			dockerInspectOutput = this.objectMapper.readValue(stream, DockerInspectOutput.class);
		}
		List<RunningService> services = serviceMapper.map(List.of(dockerInspectOutput), Path.of("compose.yaml"));
		Assertions.assertThat(services).hasSize(1);
		return services.get(0);
	}

	private DockerContextOutput linuxDockerContext() {
		return new DockerContextOutput("default", true, "unix:///var/run/docker.sock");
	}

	private DockerEnvironment windowsEnvironment() {
		DockerContextOutput currentContext = new DockerContextOutput("default", true, "npipe:////./pipe/docker_engine");
		return new DockerEnvironment(currentContext, null, null, null);
	}

	private DockerEnvironment macEnvironment() {
		DockerContextOutput currentContext = new DockerContextOutput("default", true, "unix:///var/run/docker.sock");
		return new DockerEnvironment(currentContext, null, null, null);
	}

	private DockerEnvironment wslEnvironment() {
		DockerContextOutput currentContext = new DockerContextOutput("default", true, "unix:///var/run/docker.sock");
		return new DockerEnvironment(currentContext, null, null, null);
	}

}
