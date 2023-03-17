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

package org.springframework.boot.test.autoconfigure.jdbc;

import org.testcontainers.containers.JdbcDatabaseContainer;

import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactory;
import org.springframework.boot.test.autoconfigure.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.test.autoconfigure.service.connection.ServiceConnectedContainer;

/**
 * A {@link ConnectionDetailsFactory} for creating a {@link JdbcConnectionDetails} from a
 * {@link JdbcDatabaseContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class JdbcContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<JdbcConnection, JdbcConnectionDetails> {

	@Override
	protected JdbcConnectionDetails getContainerConnectionDetails(ServiceConnectedContainer<JdbcConnection> source) {
		return new JdbcContainerConnectionDetails(source);
	}

	/**
	 * {@link JdbcConnectionDetails} backed by a {@link ServiceConnectedContainer}.
	 */
	private static class JdbcContainerConnectionDetails extends ContainerConnectionDetails
			implements JdbcConnectionDetails {

		private final JdbcDatabaseContainer<?> container;

		protected JdbcContainerConnectionDetails(ServiceConnectedContainer<?> source) {
			super(source);
			this.container = (JdbcDatabaseContainer<?>) source.getContainer(); // FIXME
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
		public String getJdbcUrl() {
			return this.container.getJdbcUrl();
		}

	}

}
