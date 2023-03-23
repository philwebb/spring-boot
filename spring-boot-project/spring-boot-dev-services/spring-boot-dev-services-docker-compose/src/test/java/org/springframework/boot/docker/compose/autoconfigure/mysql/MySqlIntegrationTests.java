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

package org.springframework.boot.docker.compose.autoconfigure.mysql;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.devservices.dockercompose.AbstractIntegrationTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MySQL.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class MySqlIntegrationTests extends AbstractIntegrationTests {

	@Test
	void shouldHaveJdbcServiceConnection() {
		JdbcConnectionDetails serviceConnection = runProvider(JdbcConnectionDetails.class);
		assertThat(serviceConnection.getName()).isEqualTo("docker-compose-mysql-jdbc-database");
		assertThat(serviceConnection.getUsername()).isEqualTo("myuser");
		assertThat(serviceConnection.getPassword()).isEqualTo("secret");
		assertThat(serviceConnection.getJdbcUrl()).startsWith("jdbc:mysql://").endsWith("/mydatabase");
	}

	@Override
	protected InputStream getComposeContent() {
		return MySqlIntegrationTests.class.getResourceAsStream("mysql-compose.yaml");
	}

}
