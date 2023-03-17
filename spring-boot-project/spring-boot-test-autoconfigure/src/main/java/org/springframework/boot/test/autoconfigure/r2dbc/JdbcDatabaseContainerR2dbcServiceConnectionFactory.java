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
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactory;
import org.springframework.boot.autoconfigure.service.connection.ServiceConnectionSource;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.test.autoconfigure.data.redis.RedisConnection;
import org.springframework.boot.test.autoconfigure.service.connection.ContainerConnectionDetailsFactory;

/**
 * A {@link ConnectionDetailsFactory} for creating an {@link R2dbcConnectionDetails} from
 * a {@link JdbcDatabaseContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class JdbcDatabaseContainerR2dbcServiceConnectionFactory
		extends ContainerConnectionDetailsFactory<RedisConnection, RedisConnectionDetails> {

	@Override
	public R2dbcConnectionDetails createServiceConnection(
			ServiceConnectionSource<JdbcDatabaseContainer<?>, R2dbcConnectionDetails> source) {
		return new R2dbcConnectionDetails() {

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
			public String getR2dbcUrl() {
				// TODO A better way of mapping JDBC to R2DBC
				return "r2dbc" + source.input().getJdbcUrl().substring(4);
			}

		};
	}

}
