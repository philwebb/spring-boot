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

package org.springframework.boot.devservices.dockercompose.redis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.autoconfigure.data.redis.RedisServiceConnection;
import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnection;
import org.springframework.boot.devservices.dockercompose.RunningServiceServiceConnectionProvider;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;
import org.springframework.boot.origin.Origin;
import org.springframework.util.ClassUtils;

/**
 * Handles connections to a redis service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class RedisConnectionProvider implements RunningServiceServiceConnectionProvider {

	private final boolean serviceConnectionPresent;

	RedisConnectionProvider(ClassLoader classLoader) {
		this.serviceConnectionPresent = ClassUtils
			.isPresent("org.springframework.boot.autoconfigure.data.redis.RedisServiceConnection", classLoader);
	}

	@Override
	public List<? extends ServiceConnection> provideServiceConnection(List<RunningService> services) {
		if (!this.serviceConnectionPresent) {
			return Collections.emptyList();
		}
		List<RedisServiceConnection> result = new ArrayList<>();
		for (RunningService service : services) {
			if (!RedisService.matches(service)) {
				continue;
			}
			RedisService redisService = new RedisService(service);
			result.add(new DockerComposeRedisServiceConnection(redisService));
		}
		return result;
	}

	private static class DockerComposeRedisServiceConnection implements RedisServiceConnection {

		private final RedisService service;

		DockerComposeRedisServiceConnection(RedisService service) {
			this.service = service;
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
					return DockerComposeRedisServiceConnection.this.service.getHost();
				}

				@Override
				public int getPort() {
					return DockerComposeRedisServiceConnection.this.service.getPort();
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

		@Override
		public String getName() {
			return "docker-compose-redis-%s".formatted(this.service.getName());
		}

		@Override
		public Origin getOrigin() {
			return this.service.getOrigin();
		}

		@Override
		public String toString() {
			return "DockerCompose[host='%s',port=%d,database='%s',username='%s']".formatted(getStandalone().getHost(),
					getStandalone().getPort(), getStandalone().getDatabase(), getUsername());
		}

	}

}
