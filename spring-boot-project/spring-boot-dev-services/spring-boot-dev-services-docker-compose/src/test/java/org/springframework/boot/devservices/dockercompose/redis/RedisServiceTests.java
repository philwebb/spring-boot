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

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.devservices.dockercompose.interop.DockerImageName;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;
import org.springframework.boot.devservices.dockercompose.test.RunningServiceBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RedisService}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class RedisServiceTests {

	@Test
	void getPort() {
		RunningService service = createService(Collections.emptyMap());
		RedisService redisService = new RedisService(service);
		assertThat(redisService.getPort()).isEqualTo(16379);
	}

	@Test
	void matches() {
		assertThat(RedisService.matches(createService(Collections.emptyMap()))).isTrue();
		assertThat(RedisService.matches(createService(DockerImageName.parse("postgres:15.2"), Collections.emptyMap())))
			.isFalse();
	}

	private RunningService createService(Map<String, String> env) {
		return createService(DockerImageName.parse("redis:7.0"), env);
	}

	private RunningService createService(DockerImageName image, Map<String, String> env) {
		return RunningServiceBuilder.create("service-1", image).addTcpPort(6379, 16379).env(env).build();
	}

}
