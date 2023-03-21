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

package org.springframework.boot.docker.compose.autoconfigure.redis;

import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.devservices.dockercompose.interop.Port;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;
import org.springframework.boot.docker.compose.autoconfigure.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.autoconfigure.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link RedisConnectionDetails}
 * for a {@code redis} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class RedisDockerComposeConnectionDetailsFactory extends DockerComposeConnectionDetailsFactory<RedisConnectionDetails> {

	private static final int REDIS_PORT = 6379;

	RedisDockerComposeConnectionDetailsFactory() {
		super("redis");
	}

	@Override
	protected RedisConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new RedisDockerComposeConnectionDetails(source.getService());
	}

	/**
	 * {@link RedisConnectionDetails} backed by a {@code redis} {@link RunningService}.
	 */
	static class RedisDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements RedisConnectionDetails {

		private final Standalone standalone;

		RedisDockerComposeConnectionDetails(RunningService source) {
			super(source);
			Port mappedPort = source.getMappedPort(REDIS_PORT);
			this.standalone = Standalone.of(source.host(), mappedPort.number());
		}

		@Override
		public Standalone getStandalone() {
			return this.standalone;
		}

	}

}
