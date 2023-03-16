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

package org.springframework.boot.devservices.dockercompose.elasticsearch;

import org.springframework.boot.devservices.dockercompose.interop.DockerComposeOrigin;
import org.springframework.boot.devservices.dockercompose.interop.Port;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;

/**
 * A running Elasticsearch service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class ElasticsearchService {

	private static final int ELASTICSEARCH_PORT = 9200;

	private final RunningService service;

	ElasticsearchService(RunningService service) {
		this.service = service;
	}

	String getUsername() {
		return "elastic";
	}

	/**
	 * Returns the password.
	 * @return the password or {@code null}
	 */
	String getPassword() {
		if (this.service.env().containsKey("ELASTIC_PASSWORD")) {
			return this.service.env().get("ELASTIC_PASSWORD");
		}
		if (this.service.env().containsKey("ELASTIC_PASSWORD_FILE")) {
			throw new IllegalStateException("ELASTIC_PASSWORD_FILE is not supported");
		}
		return null;
	}

	String getHost() {
		return this.service.host();
	}

	int getPort() {
		Port mappedPort = this.service.ports().get(ELASTICSEARCH_PORT);
		if (mappedPort == null) {
			throw new IllegalStateException("No mapped port for port %d found".formatted(ELASTICSEARCH_PORT));
		}
		return mappedPort.number();
	}

	String getName() {
		return this.service.name();
	}

	DockerComposeOrigin getOrigin() {
		return this.service.origin();
	}

	static boolean matches(RunningService service) {
		return service.image().image().equals("elasticsearch");
	}

}
