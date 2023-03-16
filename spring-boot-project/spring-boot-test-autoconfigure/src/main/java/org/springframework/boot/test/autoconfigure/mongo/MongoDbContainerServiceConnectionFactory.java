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

package org.springframework.boot.test.autoconfigure.mongo;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.testcontainers.containers.MongoDBContainer;

import org.springframework.boot.autoconfigure.mongo.MongoServiceConnection;
import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnection;
import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnectionFactory;
import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnectionSource;
import org.springframework.boot.origin.Origin;

/**
 * A {@link ServiceConnectionFactory} for creating a {@link MongoServiceConnection} from a
 * {@link MongoDBContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class MongoDbContainerServiceConnectionFactory
		implements ServiceConnectionFactory<MongoDBContainer, MongoServiceConnection> {

	@Override
	public ServiceConnection createServiceConnection(
			ServiceConnectionSource<MongoDBContainer, MongoServiceConnection> source) {
		return new MongoServiceConnection() {

			@Override
			public String getName() {
				return source.name();
			}

			@Override
			public Origin getOrigin() {
				return source.origin();
			}

			@Override
			public String getHost() {
				return URI.create(source.input().getReplicaSetUrl()).getHost();
			}

			@Override
			public int getPort() {
				return URI.create(source.input().getReplicaSetUrl()).getPort();
			}

			@Override
			public List<Host> getAdditionalHosts() {
				return Collections.emptyList();
			}

			@Override
			public String getDatabase() {
				return URI.create(source.input().getReplicaSetUrl()).getPath();
			}

			@Override
			public String getAuthenticationDatabase() {
				return null;
			}

			@Override
			public String getUsername() {
				return null;
			}

			@Override
			public String getPassword() {
				return null;
			}

			@Override
			public String getReplicaSetName() {
				return null;
			}

			@Override
			public GridFs getGridFs() {
				return null;
			}

		};
	}

}
