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

package org.springframework.boot.devservices.dockercompose.zipkin;

import org.springframework.boot.devservices.dockercompose.interop.DockerComposeOrigin;
import org.springframework.boot.devservices.dockercompose.interop.Port;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;

/**
 * A running Zipkin service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class ZipkinService {

	private static final int ZIPKIN_PORT = 9411;

	private final RunningService service;

	ZipkinService(RunningService service) {
		this.service = service;
	}

	String getHost() {
		return this.service.host();
	}

	String getName() {
		return this.service.name();
	}

	int getPort() {
		Port mappedPort = this.service.ports().get(ZIPKIN_PORT);
		if (mappedPort == null) {
			throw new IllegalStateException("No mapped port for port %d found".formatted(ZIPKIN_PORT));
		}
		return mappedPort.number();
	}

	DockerComposeOrigin getOrigin() {
		return this.service.origin();
	}

	static boolean matches(RunningService service) {
		return service.image().image().equals("zipkin");
	}

}
