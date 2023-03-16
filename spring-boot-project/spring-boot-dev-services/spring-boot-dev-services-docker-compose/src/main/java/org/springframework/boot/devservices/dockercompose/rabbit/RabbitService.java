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

package org.springframework.boot.devservices.dockercompose.rabbit;

import org.springframework.boot.devservices.dockercompose.interop.DockerComposeOrigin;
import org.springframework.boot.devservices.dockercompose.interop.Port;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;

/**
 * A running RabbitMQ service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class RabbitService {

	private static final int RABBITMQ_PORT = 5672;

	private final RunningService service;

	RabbitService(RunningService service) {
		this.service = service;
	}

	String getUsername() {
		if (this.service.env().containsKey("RABBITMQ_DEFAULT_USER")) {
			return this.service.env().get("RABBITMQ_DEFAULT_USER");
		}
		return "guest";
	}

	String getPassword() {
		if (this.service.env().containsKey("RABBITMQ_DEFAULT_PASS")) {
			return this.service.env().get("RABBITMQ_DEFAULT_PASS");
		}
		return "guest";
	}

	int getPort() {
		Port mappedPort = this.service.ports().get(RABBITMQ_PORT);
		if (mappedPort == null) {
			throw new IllegalStateException("No mapped port for port %d found".formatted(RABBITMQ_PORT));
		}
		return mappedPort.number();
	}

	String getHost() {
		return this.service.host();
	}

	String getName() {
		return this.service.name();
	}

	DockerComposeOrigin getOrigin() {
		return this.service.origin();
	}

	static boolean matches(RunningService service) {
		return service.image().image().equals("rabbitmq");
	}

}
