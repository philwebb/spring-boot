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

import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.boot.devservices.dockercompose.interop.command.DockerContextOutput;
import org.springframework.boot.devservices.dockercompose.interop.command.DockerInspectOutput;
import org.springframework.boot.devservices.dockercompose.interop.command.DockerInspectOutput.Config;
import org.springframework.boot.devservices.dockercompose.interop.command.DockerInspectOutput.NetworkSettings.PortDto;
import org.springframework.util.StringUtils;

/**
 * Maps {@link DockerInspectOutput} to {@link RunningService}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class ServiceMapper {

	private static final String LOCALHOST = "127.0.0.1";

	private final DockerEnvironment environment;

	ServiceMapper(DockerEnvironment environment) {
		this.environment = environment;
	}

	List<RunningService> map(List<DockerInspectOutput> inspects, Path composeConfigFile) {
		Map<String, DockerInspectOutput> groupedInspects = groupInspects(inspects);
		return groupedInspects.values()
			.stream()
			.map((inspect) -> new RunningService(composeConfigFile, getServiceName(inspect),
					DockerImageName.parse(inspect.config().image()), getHost(), getPorts(inspect),
					inspect.config().envAsMap(), inspect.config().labels()))
			.toList();
	}

	private String getHost() {
		if (StringUtils.hasLength(this.environment.dockerHostname())) {
			return this.environment.dockerHostname();
		}
		if (StringUtils.hasLength(this.environment.servicesHost())) {
			return this.environment.servicesHost();
		}
		String host = getHostFromDockerEndpoint(this.environment.dockerHost());
		if (StringUtils.hasLength(host)) {
			return host;
		}
		host = getHostFromDockerEndpoint(this.environment.currentContext().dockerEndpoint());
		if (StringUtils.hasLength(host)) {
			return host;
		}
		return LOCALHOST;
	}

	private static String getServiceName(DockerInspectOutput inspect) {
		String service = inspect.config().labels().get("com.docker.compose.service");
		return (service != null) ? service : "unknown";
	}

	private static String getHostFromDockerEndpoint(String endpoint) {
		if (!StringUtils.hasLength(endpoint)) {
			return null;
		}
		URI uri = URI.create(endpoint);
		return switch (uri.getScheme()) {
			case "http", "https", "tcp" -> uri.getHost();
			default -> null;
		};
	}

	private static Map<Integer, Port> getPorts(DockerInspectOutput inspect) {
		if (inspect.hostConfig().isHostNetwork()) {
			return createPortMap(inspect.config());
		}

		Map<Integer, Port> result = new HashMap<>();
		for (Entry<String, List<PortDto>> entry : inspect.networkSettings().ports().entrySet()) {
			if (entry.getValue() == null) {
				continue;
			}
			for (PortDto portDto : entry.getValue()) {
				if (portDto.isIpV4()) {
					Port port = Port.parsePortWithProtocol(entry.getKey());
					result.put(port.number(), new Port(portDto.hostPortAsInt(), port.protocol()));
				}
			}
		}
		return result;
	}

	private static Map<String, DockerInspectOutput> groupInspects(List<DockerInspectOutput> inspects) {
		Map<String, DockerInspectOutput> result = new HashMap<>();
		for (DockerInspectOutput inspect : inspects) {
			result.put(inspect.id(), inspect);
		}
		return result;
	}

	private static Map<Integer, Port> createPortMap(Config config) {
		if (config.exposedPorts() == null || config.exposedPorts().isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Integer, Port> result = new HashMap<>();
		for (String entry : config.exposedPorts().keySet()) {
			Port port = Port.parsePortWithProtocol(entry);
			result.put(port.number(), port);
		}
		return result;
	}

	record DockerEnvironment(DockerContextOutput currentContext, String dockerHostname, String servicesHost,
			String dockerHost) {
		static DockerEnvironment load(String dockerHostname, DockerContextOutput currentContext) {
			return new DockerEnvironment(currentContext, dockerHostname, System.getenv("SERVICES_HOST"),
					System.getenv("DOCKER_HOST"));
		}
	}

}
