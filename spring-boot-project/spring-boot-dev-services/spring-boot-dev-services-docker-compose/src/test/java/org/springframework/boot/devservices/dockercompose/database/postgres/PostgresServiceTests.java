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

package org.springframework.boot.devservices.dockercompose.database.postgres;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.devservices.dockercompose.interop.DockerImageName;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;
import org.springframework.boot.devservices.dockercompose.test.RunningServiceBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link PostgresService}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class PostgresServiceTests {

	@Test
	void usernameUsesEnvVariable() {
		RunningService service = createService(Map.of("POSTGRES_USER", "user-1"));
		PostgresService postgresService = new PostgresService(service);
		assertThat(postgresService.getUsername()).isEqualTo("user-1");
	}

	@Test
	void usernameHasFallback() {
		RunningService service = createService(Collections.emptyMap());
		PostgresService postgresService = new PostgresService(service);
		assertThat(postgresService.getUsername()).isEqualTo("postgres");
	}

	@Test
	void passwordUsesEnvVariable() {
		RunningService service = createService(Map.of("POSTGRES_PASSWORD", "password-1"));
		PostgresService postgresService = new PostgresService(service);
		assertThat(postgresService.getPassword()).isEqualTo("password-1");
	}

	@Test
	void passwordHasNoFallback() {
		RunningService service = createService(Collections.emptyMap());
		PostgresService postgresService = new PostgresService(service);
		assertThatThrownBy(postgresService::getPassword).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("POSTGRES_PASSWORD");
	}

	@Test
	void databaseUsesEnvVariable() {
		RunningService service = createService(Map.of("POSTGRES_DB", "database-1"));
		PostgresService postgresService = new PostgresService(service);
		assertThat(postgresService.getDatabase()).isEqualTo("database-1");
	}

	@Test
	void databaseFallsBackToUsername() {
		RunningService service = createService(Map.of("POSTGRES_USER", "user-1"));
		PostgresService postgresService = new PostgresService(service);
		assertThat(postgresService.getDatabase()).isEqualTo("user-1");
	}

	@Test
	void getPort() {
		RunningService service = createService(Collections.emptyMap());
		PostgresService postgresService = new PostgresService(service);
		assertThat(postgresService.getPort()).isEqualTo(54320);
	}

	@Test
	void matches() {
		assertThat(PostgresService.matches(createService(Collections.emptyMap()))).isTrue();
		assertThat(PostgresService.matches(createService(DockerImageName.parse("redis:7.1"), Collections.emptyMap())))
			.isFalse();
	}

	private RunningService createService(Map<String, String> env) {
		return createService(DockerImageName.parse("postgres:15.2"), env);
	}

	private RunningService createService(DockerImageName image, Map<String, String> env) {
		return RunningServiceBuilder.create("service-1", image).addTcpPort(5432, 54320).env(env).build();
	}

}
