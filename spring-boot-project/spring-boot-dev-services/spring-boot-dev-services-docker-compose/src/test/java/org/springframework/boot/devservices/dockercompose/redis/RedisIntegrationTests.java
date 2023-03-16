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

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.data.redis.RedisServiceConnection;
import org.springframework.boot.autoconfigure.data.redis.RedisServiceConnection.Standalone;
import org.springframework.boot.devservices.dockercompose.AbstractIntegrationTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Redis.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class RedisIntegrationTests extends AbstractIntegrationTests {

	@Test
	void test() {
		RedisServiceConnection serviceConnection = runProvider(RedisServiceConnection.class);
		assertThat(serviceConnection.getName()).isEqualTo("docker-compose-redis-redis");
		assertThat(serviceConnection.getUsername()).isNull();
		assertThat(serviceConnection.getPassword()).isNull();
		assertThat(serviceConnection.getCluster()).isNull();
		assertThat(serviceConnection.getSentinel()).isNull();
		Standalone standalone = serviceConnection.getStandalone();
		assertThat(standalone).isNotNull();
		assertThat(standalone.getDatabase()).isZero();
		assertThat(standalone.getPort()).isGreaterThan(0);
		assertThat(standalone.getHost()).isNotNull();
	}

	@Override
	protected InputStream getComposeContent() {
		return RedisIntegrationTests.class.getResourceAsStream("redis-compose.yaml");
	}

}
