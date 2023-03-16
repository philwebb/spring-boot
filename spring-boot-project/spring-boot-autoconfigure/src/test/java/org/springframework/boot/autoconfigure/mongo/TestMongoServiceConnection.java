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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.origin.Origin;

/**
 * A {@link MongoServiceConnection} for tests.
 *
 * @author Moritz Halbritter
 */
final class TestMongoServiceConnection implements MongoServiceConnection {

	private final String host;

	private final int port;

	private final List<Host> additionalHosts;

	private final String username;

	private final String password;

	private final String database;

	private final String authenticationDatabase;

	private final String replicaSetName;

	private final String gridDatabaseName;

	private TestMongoServiceConnection(String host, int port, List<Host> additionalHosts, String username,
			String password, String database, String authenticationDatabase, String replicaSetName,
			String gridDatabaseName) {
		this.host = host;
		this.port = port;
		this.additionalHosts = additionalHosts;
		this.username = username;
		this.password = password;
		this.database = database;
		this.authenticationDatabase = authenticationDatabase;
		this.replicaSetName = replicaSetName;
		this.gridDatabaseName = gridDatabaseName;
	}

	@Override
	public String getHost() {
		return this.host;
	}

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	public List<Host> getAdditionalHosts() {
		return this.additionalHosts;
	}

	@Override
	public String getDatabase() {
		return this.database;
	}

	@Override
	public String getAuthenticationDatabase() {
		return this.authenticationDatabase;
	}

	@Override
	public String getUsername() {
		return this.username;
	}

	@Override
	public String getPassword() {
		return this.password;
	}

	@Override
	public String getReplicaSetName() {
		return this.replicaSetName;
	}

	@Override
	public GridFs getGridFs() {
		if (this.gridDatabaseName == null) {
			return null;
		}
		return new GridFs() {
			@Override
			public String getDatabase() {
				return TestMongoServiceConnection.this.gridDatabaseName;
			}
		};
	}

	@Override
	public String getName() {
		return "mongoServiceConnection";
	}

	@Override
	public Origin getOrigin() {
		return null;
	}

	TestMongoServiceConnection withPort(int port) {
		return new TestMongoServiceConnection(this.host, port, this.additionalHosts, this.username, this.password,
				this.database, this.authenticationDatabase, this.replicaSetName, this.gridDatabaseName);
	}

	TestMongoServiceConnection withHost(String host) {
		return new TestMongoServiceConnection(host, this.port, this.additionalHosts, this.username, this.password,
				this.database, this.authenticationDatabase, this.replicaSetName, this.gridDatabaseName);
	}

	TestMongoServiceConnection withAdditionalHosts(Host... hosts) {
		return new TestMongoServiceConnection(this.host, this.port, Arrays.asList(hosts), this.username, this.password,
				this.database, this.authenticationDatabase, this.replicaSetName, this.gridDatabaseName);
	}

	TestMongoServiceConnection withUsername(String username) {
		return new TestMongoServiceConnection(this.host, this.port, this.additionalHosts, username, this.password,
				this.database, this.authenticationDatabase, this.replicaSetName, this.gridDatabaseName);
	}

	TestMongoServiceConnection withPassword(String password) {
		return new TestMongoServiceConnection(this.host, this.port, this.additionalHosts, this.username, password,
				this.database, this.authenticationDatabase, this.replicaSetName, this.gridDatabaseName);
	}

	TestMongoServiceConnection withAuthenticationDatabase(String authenticationDatabase) {
		return new TestMongoServiceConnection(this.host, this.port, this.additionalHosts, this.username, this.password,
				this.database, authenticationDatabase, this.replicaSetName, this.gridDatabaseName);
	}

	TestMongoServiceConnection withDatabase(String database) {
		return new TestMongoServiceConnection(this.host, this.port, this.additionalHosts, this.username, this.password,
				database, this.authenticationDatabase, this.replicaSetName, this.gridDatabaseName);
	}

	TestMongoServiceConnection withReplicaSetName(String replicaSetName) {
		return new TestMongoServiceConnection(this.host, this.port, this.additionalHosts, this.username, this.password,
				this.database, this.authenticationDatabase, replicaSetName, this.gridDatabaseName);
	}

	static TestMongoServiceConnection create() {
		return new TestMongoServiceConnection("mongo.example.com", 12345, Collections.emptyList(), "user-1",
				"password-1", "database-1", "authentication-database-1", "replica-set-1", null);
	}

}
