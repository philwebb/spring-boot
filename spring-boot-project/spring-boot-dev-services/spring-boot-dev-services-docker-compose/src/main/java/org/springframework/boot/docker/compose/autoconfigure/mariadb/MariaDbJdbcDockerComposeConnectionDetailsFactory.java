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

package org.springframework.boot.docker.compose.autoconfigure.mariadb;

import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;
import org.springframework.boot.docker.compose.autoconfigure.jdbc.JdbcUrl;

/**
 * @author pwebb
 */
class MariaDbJdbcDockerComposeConnectionDetailsFactory
		extends MariaDbDockerComposeConnectionDetailsFactory<JdbcConnectionDetails> {

	@Override
	protected JdbcConnectionDetails getDockerComposeConnectionDetails(RunningService source) {
		return new MariaDbJdbcDockerComposeConnectionDetails(source);
	}

	static class MariaDbJdbcDockerComposeConnectionDetails extends MariaDbDockerComposeConnectionDetails
			implements JdbcConnectionDetails {

		private final JdbcUrl jdbcUrl;

		MariaDbJdbcDockerComposeConnectionDetails(RunningService source) {
			super(source);
			this.jdbcUrl = new JdbcUrl(source, "mysql", MARIADB_PORT, getDatabase());
		}

		@Override
		public String getUsername() {
			return super.getUsername();
		}

		@Override
		public String getPassword() {
			return super.getPassword();
		}

		@Override
		public String getJdbcUrl() {
			return this.jdbcUrl.toString();
		}

	}

}
