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

import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.devservices.dockercompose.interop.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.devservices.dockercompose.interop.Port;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;

/**
 * @author pwebb
 */
class RedisDockerComposeConnectionDetailsFactory extends DockerComposeConnectionDetailsFactory<RedisConnectionDetails> {

	private static final int REDIS_PORT = 6379;

	RedisDockerComposeConnectionDetailsFactory() {
		super("redis");
	}

	@Override
	protected RedisConnectionDetails getDockerComposeConnectionDetails(RunningService source) {
		return new RedisDockerComposeConnectionDetails(source);
	}

	private static class RedisDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements RedisConnectionDetails {

		private final Standalone standalone;

		protected RedisDockerComposeConnectionDetails(RunningService source) {
			super(source);
			Port mappedPort = source.getMappedPort(REDIS_PORT);
			this.standalone = new Standalone() {

				@Override
				public String getHost() {
					return source.host();
				}

				@Override
				public int getPort() {
					return mappedPort.number();
				}

			};
		}

		@Override
		public Standalone getStandalone() {
			return this.standalone;
		}

	}

}
