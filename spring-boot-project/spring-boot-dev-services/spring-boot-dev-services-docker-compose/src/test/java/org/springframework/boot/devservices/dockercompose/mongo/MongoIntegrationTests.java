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

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.mongo.MongoServiceConnection;
import org.springframework.boot.devservices.dockercompose.AbstractIntegrationTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MongoDB.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class MongoIntegrationTests extends AbstractIntegrationTests {

	@Test
	void test() {
		MongoServiceConnection serviceConnection = runProvider(MongoServiceConnection.class);
		assertThat(serviceConnection.getName()).isEqualTo("docker-compose-mongo-mongo");
		assertThat(serviceConnection.getHost()).isNotNull();
		assertThat(serviceConnection.getPort()).isGreaterThan(0);
		assertThat(serviceConnection.getUsername()).isEqualTo("root");
		assertThat(serviceConnection.getPassword()).isEqualTo("secret");
		assertThat(serviceConnection.getDatabase()).isEqualTo("mydatabase");
		assertThat(serviceConnection.getAuthenticationDatabase()).isEqualTo("admin");
		assertThat(serviceConnection.getReplicaSetName()).isNull();
		assertThat(serviceConnection.getAdditionalHosts()).isEmpty();
		assertThat(serviceConnection.getGridFs()).isNull();
	}

	@Override
	protected InputStream getComposeContent() {
		return MongoIntegrationTests.class.getResourceAsStream("mongodb-compose.yaml");
	}

}
