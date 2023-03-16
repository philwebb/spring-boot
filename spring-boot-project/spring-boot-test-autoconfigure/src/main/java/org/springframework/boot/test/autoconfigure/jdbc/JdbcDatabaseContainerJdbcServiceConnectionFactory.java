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

import org.springframework.boot.autoconfigure.jdbc.JdbcServiceConnection;
import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnectionFactory;
import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnectionSource;
import org.springframework.boot.origin.Origin;

/**
 * A {@link ServiceConnectionFactory} for creating a {@link JdbcServiceConnection} from a
 * {@link JdbcDatabaseContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class JdbcDatabaseContainerJdbcServiceConnectionFactory
		implements ServiceConnectionFactory<JdbcDatabaseContainer<?>, JdbcServiceConnection> {

	@Override
	public JdbcServiceConnection createServiceConnection(
			ServiceConnectionSource<JdbcDatabaseContainer<?>, JdbcServiceConnection> source) {
		return new JdbcServiceConnection() {

			@Override
			public Origin getOrigin() {
				return source.origin();
			}

			@Override
			public String getName() {
				return source.name();
			}

			@Override
			public String getUsername() {
				return source.input().getUsername();
			}

			@Override
			public String getPassword() {
				return source.input().getPassword();
			}

			@Override
			public String getJdbcUrl() {
				return source.input().getJdbcUrl();
			}

		};
	}

}
