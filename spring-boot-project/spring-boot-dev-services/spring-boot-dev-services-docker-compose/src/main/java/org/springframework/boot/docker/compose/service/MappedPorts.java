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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.boot.devservices.xdockercompose.interop.command.DockerInspectOutput;
import org.springframework.boot.docker.compose.service.DockerInspectResponse.HostPort;
import org.springframework.util.StringUtils;

/**
 * @author pwebb
 */
public class MappedPorts {

	// Fixme move into whatever RunningService Impl is

	private Map<ContainerPort, HostPort> map;

	Integer getMappedPort(int containerPort) {
		return null;
	}

	List<Integer> getMappedPorts(ContainerPort.Protocol protocol) {
		return null;
	}

	private static Map<Integer, ContainerPort> getPorts(DockerInspectResponse inspect) {
		if (isHostNetwork(inspect.hostConfig())) {
			return getPortsForHistNetwork(inspect.config());
		}
		Map<Integer, ContainerPort> result = new HashMap<>();
		for (Entry<String, List<HostPort>> entry : inspect.networkSettings().ports().entrySet()) {
			if (entry.getValue() == null) {
				continue;
			}
			ContainerPort containerPort = ContainerPort.parsePortWithProtocol(entry.getKey());
			for (HostPort host : entry.getValue()) {
				if (isIpV4(host)) {
					result.put(containerPort.number(),
							new ContainerPort(hostPortAsInt(host), containerPort.protocol()));
				}
			}
		}
		return result;
	}

	public static boolean isIpV4(HostPort m) {
		if (!StringUtils.hasLength(m.hostIp())) {
			return true;
		}
		return m.hostIp().contains(".");
	}

	public static int hostPortAsInt(HostPort m) {
		return Integer.parseInt(m.hostPort());
	}

	public static boolean isHostNetwork(DockerInspectResponse.HostConfig hostConfig) {
		return "host".equals(hostConfig.networkMode());
	}

	private static Map<String, DockerInspectOutput> groupInspects(List<DockerInspectOutput> inspects) {
		Map<String, DockerInspectOutput> result = new HashMap<>();
		for (DockerInspectOutput inspect : inspects) {
			result.put(inspect.id(), inspect);
		}
		return result;
	}

	private static Map<Integer, ContainerPort> getPortsForHistNetwork(DockerInspectResponse.Config config) {
		if (config.exposedPorts() == null || config.exposedPorts().isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Integer, ContainerPort> result = new HashMap<>();
		for (String entry : config.exposedPorts().keySet()) {
			ContainerPort containerPort = ContainerPort.parsePortWithProtocol(entry);
			result.put(containerPort.number(), containerPort);
		}
		return result;
	}

}
