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

package org.springframework.boot.devservices.dockercompose.elasticsearch;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.devservices.dockercompose.interop.DockerImageName;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;
import org.springframework.boot.devservices.dockercompose.test.RunningServiceBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ElasticsearchService}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class ElasticsearchServiceTests {

	@Test
	void usernameIsElastic() {
		RunningService service = createService(Collections.emptyMap());
		ElasticsearchService elasticsearchService = new ElasticsearchService(service);
		assertThat(elasticsearchService.getUsername()).isEqualTo("elastic");
	}

	@Test
	void passwordUsesEnvVariable() {
		RunningService service = createService(Map.of("ELASTIC_PASSWORD", "some-secret-password"));
		ElasticsearchService elasticsearchService = new ElasticsearchService(service);
		assertThat(elasticsearchService.getPassword()).isEqualTo("some-secret-password");
	}

	@Test
	void passwordHasFallback() {
		RunningService service = createService(Collections.emptyMap());
		ElasticsearchService elasticsearchService = new ElasticsearchService(service);
		assertThat(elasticsearchService.getPassword()).isNull();
	}

	@Test
	void passwordDoesNotSupportFile() {
		RunningService service = createService(Map.of("ELASTIC_PASSWORD_FILE", "/password.txt"));
		ElasticsearchService elasticsearchService = new ElasticsearchService(service);
		assertThatThrownBy(elasticsearchService::getPassword).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("ELASTIC_PASSWORD_FILE");
	}

	@Test
	void getPort() {
		RunningService service = createService(Collections.emptyMap());
		ElasticsearchService elasticsearchService = new ElasticsearchService(service);
		assertThat(elasticsearchService.getPort()).isEqualTo(19200);
	}

	@Test
	void matches() {
		assertThat(ElasticsearchService.matches(createService(Collections.emptyMap()))).isTrue();
		assertThat(
				ElasticsearchService.matches(createService(DockerImageName.parse("redis:7.1"), Collections.emptyMap())))
			.isFalse();
	}

	private RunningService createService(Map<String, String> env) {
		return createService(DockerImageName.parse("elasticsearch:8.6.2"), env);
	}

	private RunningService createService(DockerImageName image, Map<String, String> env) {
		return RunningServiceBuilder.create("service-1", image).addTcpPort(9200, 19200).env(env).build();
	}

}
