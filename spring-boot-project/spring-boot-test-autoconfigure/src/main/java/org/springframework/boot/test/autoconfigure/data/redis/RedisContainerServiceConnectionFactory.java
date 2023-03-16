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

package org.springframework.boot.test.autoconfigure.data.redis;

import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.autoconfigure.data.redis.RedisServiceConnection;
import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnectionFactory;
import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnectionSource;
import org.springframework.boot.origin.Origin;

/**
 * A {@link ServiceConnectionFactory} for creating a {@link RedisServiceConnection} from a
 * {@link GenericContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class RedisContainerServiceConnectionFactory
		implements ServiceConnectionFactory<GenericContainer<?>, RedisServiceConnection> {

	@Override
	public RedisServiceConnection createServiceConnection(
			ServiceConnectionSource<GenericContainer<?>, RedisServiceConnection> source) {
		return new RedisServiceConnection() {

			@Override
			public String getName() {
				return source.name();
			}

			@Override
			public Origin getOrigin() {
				return source.origin();
			}

			@Override
			public String getUsername() {
				return null;
			}

			@Override
			public String getPassword() {
				return null;
			}

			@Override
			public Standalone getStandalone() {
				return new Standalone() {

					@Override
					public int getDatabase() {
						return 0;
					}

					@Override
					public String getHost() {
						return source.input().getHost();
					}

					@Override
					public int getPort() {
						return source.input().getFirstMappedPort();
					}

				};
			}

			@Override
			public Sentinel getSentinel() {
				return null;
			}

			@Override
			public Cluster getCluster() {
				return null;
			}

		};
	}

}
