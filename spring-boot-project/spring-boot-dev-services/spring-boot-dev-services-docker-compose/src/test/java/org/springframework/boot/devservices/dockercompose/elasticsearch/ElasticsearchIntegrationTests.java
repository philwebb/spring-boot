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

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchServiceConnection;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchServiceConnection.Node;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchServiceConnection.Node.Protocol;
import org.springframework.boot.devservices.dockercompose.AbstractIntegrationTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ElasticSearch.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class ElasticsearchIntegrationTests extends AbstractIntegrationTests {

	@Test
	void test() {
		ElasticsearchServiceConnection serviceConnection = runProvider(ElasticsearchServiceConnection.class);
		assertThat(serviceConnection.getName()).isEqualTo("docker-compose-elasticsearch-elasticsearch");
		assertThat(serviceConnection.getUsername()).isEqualTo("elastic");
		assertThat(serviceConnection.getPassword()).isEqualTo("secret");
		assertThat(serviceConnection.getPathPrefix()).isNull();
		assertThat(serviceConnection.getNodes()).hasSize(1);
		Node node = serviceConnection.getNodes().get(0);
		assertThat(node.hostname()).isNotNull();
		assertThat(node.port()).isGreaterThan(0);
		assertThat(node.protocol()).isEqualTo(Protocol.HTTP);
		assertThat(node.username()).isEqualTo("elastic");
		assertThat(node.password()).isEqualTo("secret");
	}

	@Override
	protected InputStream getComposeContent() {
		return ElasticsearchIntegrationTests.class.getResourceAsStream("elasticsearch-compose.yaml");
	}

}
