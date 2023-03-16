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

import java.util.List;

import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnection;

/**
 * A connection to a MongoDB service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public interface MongoServiceConnection extends ServiceConnection {

	/**
	 * Mongo server host.
	 * @return the mongo server host
	 */
	String getHost();

	/**
	 * Mongo server port.
	 * @return the mongo server port
	 */
	int getPort();

	/**
	 * Additional server hosts.
	 * @return the additional server hosts
	 */
	List<Host> getAdditionalHosts();

	/**
	 * Database name.
	 * @return the database name
	 */
	String getDatabase();

	/**
	 * Authentication database name.
	 * @return the Authentication database name or {@code null}
	 */
	String getAuthenticationDatabase();

	/**
	 * Login user of the mongo server.
	 * @return the login user of the mongo server or {@code null}
	 */
	String getUsername();

	/**
	 * Login password of the mongo server.
	 * @return the login password of the mongo server or {@code null}
	 */
	String getPassword();

	/**
	 * Replica set name for the cluster.
	 * @return the required replica set name for the cluster or {@code null}
	 */
	String getReplicaSetName();

	/**
	 * GridFS configuration.
	 * @return the GridFS configuration or {@code null}
	 */
	GridFs getGridFs();

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
