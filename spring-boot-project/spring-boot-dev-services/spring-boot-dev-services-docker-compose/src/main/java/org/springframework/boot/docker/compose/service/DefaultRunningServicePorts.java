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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.docker.compose.service.DockerCliInspectResponse.Config;
import org.springframework.boot.docker.compose.service.DockerCliInspectResponse.HostConfig;
import org.springframework.boot.docker.compose.service.DockerCliInspectResponse.HostPort;
import org.springframework.boot.docker.compose.service.DockerCliInspectResponse.NetworkSettings;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Default {@link RunningService.Ports} implementation backed by {@link DockerCli}
 * responses.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DefaultRunningServicePorts implements RunningService.Ports {

	private Map<ContainerPort, Integer> mappings = new LinkedHashMap<>();

	private Map<Integer, Integer> portMappings = new LinkedHashMap<>();

	DefaultRunningServicePorts(DockerCliInspectResponse inspectResponse) {
		this.mappings = !isHostNetworkMode(inspectResponse)
				? buildMappingsForNetworkSettings(inspectResponse.networkSettings())
				: buildMappingsForHostNetworking(inspectResponse.config());
		Map<Integer, Integer> portMappings = new HashMap<>();
		this.mappings.forEach((containerPort, hostPort) -> portMappings.put(containerPort.number(), hostPort));
		this.portMappings = Collections.unmodifiableMap(portMappings);
	}

	private static boolean isHostNetworkMode(DockerCliInspectResponse inspectResponse) {
		HostConfig config = inspectResponse.hostConfig();
		return (config != null) && "host".equals(config.networkMode());
	}

	private Map<ContainerPort, Integer> buildMappingsForNetworkSettings(NetworkSettings networkSettings) {
		if (networkSettings == null || CollectionUtils.isEmpty(networkSettings.ports())) {
			return Collections.emptyMap();
		}
		Map<ContainerPort, Integer> mappings = new HashMap<>();
		networkSettings.ports().forEach((containerPortString, hostPorts) -> {
			if (!CollectionUtils.isEmpty(hostPorts)) {
				ContainerPort containerPort = ContainerPort.parse(containerPortString);
				hostPorts.stream()
					.filter(this::isIpV4)
					.forEach((hostPort) -> mappings.put(containerPort, getPortNumber(hostPort)));
			}
		});
		return Collections.unmodifiableMap(mappings);
	}

	public boolean isIpV4(HostPort hostPort) {
		String ip = (hostPort != null) ? hostPort.hostIp() : null;
		return !StringUtils.hasLength(ip) || ip.contains(".");
	}

	public static int getPortNumber(HostPort hostPort) {
		return Integer.parseInt(hostPort.hostPort());
	}

	private Map<ContainerPort, Integer> buildMappingsForHostNetworking(Config config) {
		if (CollectionUtils.isEmpty(config.exposedPorts())) {
			return Collections.emptyMap();
		}
		Map<ContainerPort, Integer> mappings = new HashMap<>();
		for (String entry : config.exposedPorts().keySet()) {
			ContainerPort containerPort = ContainerPort.parse(entry);
			mappings.put(containerPort, containerPort.number());
		}
		return Collections.unmodifiableMap(mappings);
	}

	@Override
	public int get(int containerPort) {
		Integer hostPort = this.portMappings.get(containerPort);
		Assert.state(hostPort != null, "No host port mapping found for container port %s".formatted(containerPort));
		return hostPort;
	}

	@Override
	public List<Integer> getAll() {
		return getAll(null);
	}

	@Override
	public List<Integer> getAll(String protocol) {
		List<Integer> hostPorts = new ArrayList<>();
		this.mappings.forEach((containerPort, hostPort) -> {
			if (protocol == null || protocol.equalsIgnoreCase(containerPort.protocol())) {
				hostPorts.add(hostPort);
			}
		});
		return Collections.unmodifiableList(hostPorts);
	}

	Map<ContainerPort, Integer> getMappings() {
		return this.mappings;
	}

	static record ContainerPort(int number, String protocol) {

		@Override
		public String toString() {
			return "%d/%s".formatted(this.number, this.protocol);
		}

		public static ContainerPort parse(String value) {
			try {
				String[] parts = value.split("/");
				Assert.state(parts.length == 2, "Unable to split string");
				return new ContainerPort(Integer.parseInt(parts[0]), parts[1]);
			}
			catch (RuntimeException ex) {
				throw new IllegalStateException("Unable to parse container port '%s'".formatted(value), ex);
			}
		}

	}

}
