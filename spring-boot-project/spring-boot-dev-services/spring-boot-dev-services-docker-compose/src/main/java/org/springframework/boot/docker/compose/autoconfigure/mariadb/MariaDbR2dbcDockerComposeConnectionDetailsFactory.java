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

import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;
import org.springframework.boot.docker.compose.autoconfigure.r2dbc.R2dbcUrl;

/**
 * @author pwebb
 */
class MariaDbR2dbcDockerComposeConnectionDetailsFactory
		extends MariaDbDockerComposeConnectionDetailsFactory<R2dbcConnectionDetails> {

	@Override
	protected R2dbcConnectionDetails getDockerComposeConnectionDetails(RunningService source) {
		return new MariaDbJdbcDockerComposeConnectionDetails(source);
	}

	static class MariaDbJdbcDockerComposeConnectionDetails extends MariaDbDockerComposeConnectionDetails
			implements R2dbcConnectionDetails {

		private final R2dbcUrl r2dbcUrl;

		MariaDbJdbcDockerComposeConnectionDetails(RunningService source) {
			super(source);
			this.r2dbcUrl = new R2dbcUrl(source, "mariadb", MARIADB_PORT, getDatabase());
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
		public String getR2dbcUrl() {
			return this.r2dbcUrl.toString();
		}

	}

}
