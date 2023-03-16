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

package org.springframework.boot.devservices.dockercompose.mongo;

import org.springframework.boot.devservices.dockercompose.interop.DockerComposeOrigin;
import org.springframework.boot.devservices.dockercompose.interop.Port;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;

/**
 * A running MongoDB service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class MongoService {

	private static final int MONGODB_PORT = 27017;

	private final RunningService service;

	MongoService(RunningService service) {
		this.service = service;
	}

	/**
	 * Returns the username.
	 * @return the username or {@code null}
	 */
	String getUsername() {
		if (this.service.env().containsKey("MONGO_INITDB_ROOT_USERNAME")) {
			return this.service.env().get("MONGO_INITDB_ROOT_USERNAME");
		}
		if (this.service.env().containsKey("MONGO_INITDB_ROOT_USERNAME_FILE")) {
			throw new IllegalStateException("MONGO_INITDB_ROOT_USERNAME_FILE is not supported");
		}
		return null;
	}

	/**
	 * Returns the password.
	 * @return the password or {@code null}
	 */
	String getPassword() {
		if (this.service.env().containsKey("MONGO_INITDB_ROOT_PASSWORD")) {
			return this.service.env().get("MONGO_INITDB_ROOT_PASSWORD");
		}
		if (this.service.env().containsKey("MONGO_INITDB_ROOT_PASSWORD_FILE")) {
			throw new IllegalStateException("MONGO_INITDB_ROOT_PASSWORD_FILE is not supported");
		}
		return null;
	}

	String getHost() {
		return this.service.host();
	}

	/**
	 * Returns the database.
	 * @return the database or {@code null}
	 */
	String getDatabase() {
		if (this.service.env().containsKey("MONGO_INITDB_DATABASE")) {
			return this.service.env().get("MONGO_INITDB_DATABASE");
		}
		return null;
	}

	int getPort() {
		Port mappedPort = this.service.ports().get(MONGODB_PORT);
		if (mappedPort == null) {
			throw new IllegalStateException("No mapped port for port %d found".formatted(MONGODB_PORT));
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
		return service.image().image().equals("mongo");
	}

}
