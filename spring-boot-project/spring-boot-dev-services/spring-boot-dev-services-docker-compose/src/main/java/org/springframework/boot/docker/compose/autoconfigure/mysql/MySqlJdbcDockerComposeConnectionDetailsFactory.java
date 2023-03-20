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

import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;
import org.springframework.boot.docker.compose.autoconfigure.jdbc.JdbcUrlBuilder;
import org.springframework.boot.docker.compose.autoconfigure.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.autoconfigure.service.connection.DockerComposeConnectionSource;

/**
 * @author pwebb
 */
class MySqlJdbcDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<JdbcConnectionDetails> {

	protected MySqlJdbcDockerComposeConnectionDetailsFactory() {
		super("mysql");
	}

	@Override
	protected JdbcConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new MySqlJdbcDockerComposeConnectionDetails(source.getService());
	}

	static class MySqlJdbcDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements JdbcConnectionDetails {

		private static final JdbcUrlBuilder jdbcUrlBuilder = new JdbcUrlBuilder("mariadb", 3306);

		private final MySqlEnv env;

		private final String jdbcUrl;

		MySqlJdbcDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.env = new MySqlEnv(service.env());
			this.jdbcUrl = jdbcUrlBuilder.build(service, this.env.getDatabase());
		}

		@Override
		public String getUsername() {
			return this.env.getUser();
		}

		@Override
		public String getPassword() {
			return this.env.getPassword();
		}

		@Override
		public String getJdbcUrl() {
			return this.jdbcUrl;
		}

	}

}
