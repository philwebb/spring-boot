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

package org.springframework.boot.devservices.dockercompose.test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.devservices.dockercompose.interop.DockerImageName;
import org.springframework.boot.devservices.dockercompose.interop.Port;
import org.springframework.boot.devservices.dockercompose.interop.Port.Protocol;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;

/**
 * Builder for {@link RunningService}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
public final class RunningServiceBuilder {

	private Path composeConfigFile = null;

	private final String name;

	private final DockerImageName image;

	private String host = "127.0.0.1";

	private Map<Integer, Port> ports = new HashMap<>();

	private Map<String, String> env = new HashMap<>();

	private Map<String, String> labels = new HashMap<>();

	private RunningServiceBuilder(String name, DockerImageName image) {
		this.name = name;
		this.image = image;
	}

	public RunningServiceBuilder composeConfigFile(Path composeConfigFile) {
		this.composeConfigFile = composeConfigFile;
		return this;
	}

	public RunningServiceBuilder host(String host) {
		this.host = host;
		return this;
	}

	public RunningServiceBuilder ports(Map<Integer, Port> ports) {
		this.ports = ports;
		return this;
	}

	public RunningServiceBuilder addPort(int containerPort, Port port) {
		this.ports.put(containerPort, port);
		return this;
	}

	public RunningServiceBuilder addTcpPort(int containerPort, int hostPort) {
		this.ports.put(containerPort, new Port(hostPort, Protocol.TCP));
		return this;
	}

	public RunningServiceBuilder env(Map<String, String> env) {
		this.env = env;
		return this;
	}

	public RunningServiceBuilder addEnv(String name, String value) {
		this.env.put(name, value);
		return this;
	}

	public RunningServiceBuilder labels(Map<String, String> labels) {
		this.labels = labels;
		return this;
	}

	public RunningServiceBuilder addLabel(String key, String value) {
		this.labels.put(key, value);
		return this;
	}

	public RunningService build() {
		return new RunningService(this.composeConfigFile, this.name, this.image, this.host, this.ports, this.env,
				this.labels);
	}

	public static RunningServiceBuilder create(String name, String image) {
		return new RunningServiceBuilder(name, DockerImageName.parse(image));
	}

	public static RunningServiceBuilder create(String name, DockerImageName image) {
		return new RunningServiceBuilder(name, image);
	}

	public static RunningServiceBuilder create(RunningService service) {
		return new RunningServiceBuilder(service.name(), service.image()).composeConfigFile(service.composeConfigFile())
			.host(service.host())
			.env(service.env())
			.labels(service.labels())
			.ports(service.ports());
	}

}
