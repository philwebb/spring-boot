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

package org.springframework.boot.autoconfigure.data.mongo;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.autoconfigure.mongo.MongoServiceConnection;
import org.springframework.boot.origin.Origin;

/**
 * A {@link MongoServiceConnection} for tests.
 *
 * @author Moritz Halbritter
 */
class TestMongoServiceConnection implements MongoServiceConnection {

	@Override
	public String getHost() {
		return "mongo.example.com";
	}

	@Override
	public int getPort() {
		return 12345;
	}

	@Override
	public List<Host> getAdditionalHosts() {
		return Collections.emptyList();
	}

	@Override
	public String getDatabase() {
		return "database-1";
	}

	@Override
	public String getAuthenticationDatabase() {
		return "authentication-database-1";
	}

	@Override
	public String getUsername() {
		return "user-1";
	}

	@Override
	public String getPassword() {
		return "password-1";
	}

	@Override
	public String getReplicaSetName() {
		return "replica-set-1";
	}

	@Override
	public GridFs getGridFs() {
		return new GridFs() {
			@Override
			public String getDatabase() {
				return "grid-database-1";
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

}
