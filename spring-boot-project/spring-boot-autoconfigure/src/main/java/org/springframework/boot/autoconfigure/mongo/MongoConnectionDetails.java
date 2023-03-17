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

package org.springframework.boot.autoconfigure.mongo;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * A connection to a MongoDB service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public interface MongoConnectionDetails extends ConnectionDetails {

	/**
	 * Mongo server host.
	 * @return the mongo server host
	 */
	String getHost(); // FIXME return Host type and drop getPort?

	/**
	 * Additional server hosts.
	 * @return the additional server hosts
	 */
	default List<Host> getAdditionalHosts() {
		return Collections.emptyList();
	}

	/**
	 * Mongo server port.
	 * @return the mongo server port
	 */
	int getPort();

	/**
	 * Database name.
	 * @return the database name
	 */
	String getDatabase();

	/**
	 * Authentication database name.
	 * @return the Authentication database name or {@code null}
	 */
	default String getAuthenticationDatabase() {
		return null;
	}

	/**
	 * Login user of the mongo server.
	 * @return the login user of the mongo server or {@code null}
	 */
	default String getUsername() {
		return null;
	}

	/**
	 * Login password of the mongo server.
	 * @return the login password of the mongo server or {@code null}
	 */
	default String getPassword() {
		return null;
	}

	/**
	 * Replica set name for the cluster.
	 * @return the required replica set name for the cluster or {@code null}
	 */
	default String getReplicaSetName() {
		return null;
	}

	/**
	 * GridFS configuration.
	 * @return the GridFS configuration or {@code null}
	 */
	default GridFs getGridFs() {
		return null;
	}

	/**
	 * GridFS configuration.
	 */
	interface GridFs {

		/**
		 * GridFS database name.
		 * @return the GridFS database name or {@code null}
		 */
		String getDatabase();

	}

	/**
	 * A mongo host.
	 *
	 * @param host the host
	 * @param port the port
	 */
	record Host(String host, int port) {
	}

}
