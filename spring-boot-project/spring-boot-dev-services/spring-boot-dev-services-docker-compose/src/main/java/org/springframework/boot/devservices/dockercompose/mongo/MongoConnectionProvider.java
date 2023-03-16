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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.autoconfigure.mongo.MongoServiceConnection;
import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnection;
import org.springframework.boot.devservices.dockercompose.RunningServiceServiceConnectionProvider;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;
import org.springframework.boot.origin.Origin;
import org.springframework.util.ClassUtils;

/**
 * Handles connections to a MongoDB service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class MongoConnectionProvider implements RunningServiceServiceConnectionProvider {

	private final boolean serviceConnectionPresent;

	MongoConnectionProvider(ClassLoader classLoader) {
		this.serviceConnectionPresent = ClassUtils
			.isPresent("org.springframework.boot.autoconfigure.mongo.MongoServiceConnection", classLoader);
	}

	@Override
	public List<? extends ServiceConnection> provideServiceConnection(List<RunningService> services) {
		if (!this.serviceConnectionPresent) {
			return Collections.emptyList();
		}
		List<MongoServiceConnection> result = new ArrayList<>();
		for (RunningService service : services) {
			if (!MongoService.matches(service)) {
				continue;
			}
			MongoService mongoService = new MongoService(service);
			result.add(new DockerComposeMongoServiceConnection(mongoService));
		}
		return result;
	}

	private static class DockerComposeMongoServiceConnection implements MongoServiceConnection {

		private final MongoService service;

		DockerComposeMongoServiceConnection(MongoService service) {
			this.service = service;
		}

		@Override
		public String getHost() {
			return this.service.getHost();
		}

		@Override
		public int getPort() {
			return this.service.getPort();
		}

		@Override
		public List<Host> getAdditionalHosts() {
			return Collections.emptyList();
		}

		@Override
		public String getDatabase() {
			String database = this.service.getDatabase();
			return (database != null) ? database : "test";
		}

		@Override
		public String getAuthenticationDatabase() {
			if (getUsername() != null && getPassword() != null) {
				return "admin";
			}
			return null;
		}

		@Override
		public String getUsername() {
			return this.service.getUsername();
		}

		@Override
		public String getPassword() {
			return this.service.getPassword();
		}

		@Override
		public String getReplicaSetName() {
			return null;
		}

		@Override
		public GridFs getGridFs() {
			return null;
		}

		@Override
		public String getName() {
			return "docker-compose-mongo-%s".formatted(this.service.getName());
		}

		@Override
		public Origin getOrigin() {
			return this.service.getOrigin();
		}

		@Override
		public String toString() {
			return "DockerCompose[host='%s',port=%d,username='%s',database='%s']".formatted(getHost(), getPort(),
					getUsername(), getDatabase());
		}

	}

}
