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

package org.springframework.boot.test.autoconfigure.r2dbc;

import org.testcontainers.containers.JdbcDatabaseContainer;

import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.boot.test.autoconfigure.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.test.autoconfigure.service.connection.ServiceConnectedContainer;

/**
 * {@link ContainerConnectionDetailsFactory} for {@link R2dbcConnection @R2dbcConnection}
 * annotated {@link JdbcDatabaseContainer} fields.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class R2dbcContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<R2dbcConnection, R2dbcConnectionDetails, JdbcDatabaseContainer<?>> {

	@Override
	protected R2dbcConnectionDetails getContainerConnectionDetails(
			ServiceConnectedContainer<R2dbcConnection, R2dbcConnectionDetails, JdbcDatabaseContainer<?>> source) {
		return new R2dbcContainerConnectionDetails(source);
	}

	/**
	 * {@link RedisConnectionDetails} backed by a {@link ServiceConnectedContainer}.
	 */
	private static class R2dbcContainerConnectionDetails extends ContainerConnectionDetails
			implements R2dbcConnectionDetails {

		private final JdbcDatabaseContainer<?> container;

		protected R2dbcContainerConnectionDetails(
				ServiceConnectedContainer<R2dbcConnection, R2dbcConnectionDetails, JdbcDatabaseContainer<?>> source) {
			super(source);
			this.container = source.getContainer();
		}

		@Override
		public String getUsername() {
			return this.container.getUsername();
		}

		@Override
		public String getPassword() {
			return this.container.getPassword();
		}

		@Override
		public String getR2dbcUrl() {
			// FIXME A better way of mapping JDBC to R2DBC
			return "r2dbc" + this.container.getJdbcUrl().substring(4);
		}

	}

}
