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

package org.springframework.boot.devservices.dockercompose.database;

import java.util.Map;

import org.springframework.boot.devservices.dockercompose.interop.Port;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;
import org.springframework.boot.origin.Origin;

/**
 * Abstract base class for database services.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public abstract class DatabaseService {

	protected final RunningService service;

	protected DatabaseService(RunningService service) {
		this.service = service;
	}

	/**
	 * Returns the username for the JDBC connection.
	 * @return the username for the JDBC connection.
	 */
	public abstract String getUsername();

	/**
	 * Returns the password for the JDBC connection.
	 * @return the password for the JDBC connection.
	 */
	public abstract String getPassword();

	/**
	 * Returns the database for the JDBC connection.
	 * @return the database for the JDBC connection.
	 */
	public abstract String getDatabase();

	/**
	 * Returns the port for the JDBC connection.
	 * @return the port for the JDBC connection.
	 */
	public abstract int getPort();

	/**
	 * Returns the host for the JDBC connection.
	 * @return the host for the JDBC connection.
	 */
	public String getHost() {
		return this.service.host();
	}

	protected int getMappedPortOrThrow(int port) {
		Port mappedPort = this.service.ports().get(port);
		if (mappedPort == null) {
			throw new IllegalStateException("No mapped port for port %d found".formatted(port));
		}
		return mappedPort.number();
	}

	/**
	 * Returns the origin for this service.
	 * @return the origin for this service
	 */
	public Origin getOrigin() {
		return this.service.origin();
	}

	/**
	 * Returns the name for this service.
	 * @return the name for this service
	 */
	public String getName() {
		return this.service.name();
	}

	/**
	 * Returns the labels for this service.
	 * @return the labels for this service
	 */
	public Map<String, String> getLabels() {
		return this.service.labels();
	}

}
