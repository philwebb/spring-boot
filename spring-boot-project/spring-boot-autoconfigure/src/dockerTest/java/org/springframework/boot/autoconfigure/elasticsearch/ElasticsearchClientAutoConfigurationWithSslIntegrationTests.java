/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.elasticsearch;

import java.util.Map;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ElasticsearchClientAutoConfiguration} with enabled SSL.
 *
 * @author Moritz Halbritter
 */
@Testcontainers(disabledWithoutDocker = true)
class ElasticsearchClientAutoConfigurationWithSslIntegrationTests {

	@Container
	static final ElasticsearchContainer elasticsearch = TestImage.container(ElasticsearchContainer.class)
		// See
		// https://www.elastic.co/guide/en/elastic-stack-get-started/7.5/get-started-docker.html#get-started-docker-tls
		.withEnv(Map.of("ELASTIC_PASSWORD", "secret", "xpack.security.enabled", "true",
				"xpack.security.http.ssl.enabled", "true", "xpack.security.http.ssl.key",
				"/usr/share/elasticsearch/config/ssl.key", "xpack.security.http.ssl.certificate",
				"/usr/share/elasticsearch/config/ssl.crt", "xpack.security.transport.ssl.enabled", "true",
				"xpack.security.transport.ssl.verification_mode", "certificate", "xpack.security.transport.ssl.key",
				"/usr/share/elasticsearch/config/ssl.key", "xpack.security.transport.ssl.certificate",
				"/usr/share/elasticsearch/config/ssl.crt"))
		.withCopyFileToContainer(
				MountableFile.forClasspathResource("/org/springframework/boot/autoconfigure/elasticsearch/ssl.crt"),
				"/usr/share/elasticsearch/config/ssl.crt")
		.withCopyFileToContainer(
				MountableFile.forClasspathResource("/org/springframework/boot/autoconfigure/elasticsearch/ssl.key"),
				"/usr/share/elasticsearch/config/ssl.key");

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SslAutoConfiguration.class, JacksonAutoConfiguration.class,
				ElasticsearchRestClientAutoConfiguration.class, ElasticsearchClientAutoConfiguration.class));

	@Test
	void clientCanQueryElasticsearchNode() {
		this.contextRunner.withPropertyValues(
				"spring.ssl.bundle.pem.mybundle.truststore.certificate=classpath:org/springframework/boot/autoconfigure/elasticsearch/ssl.crt",
				"spring.elasticsearch.uris=https://" + elasticsearch.getHttpHostAddress(),
				"spring.elasticsearch.username=elastic", "spring.elasticsearch.password=secret",
				"spring.elasticsearch.connection-timeout=120s", "spring.elasticsearch.socket-timeout=120s",
				"spring.elasticsearch.restclient.ssl.bundle=mybundle")
			.run((context) -> {
				ElasticsearchClient client = context.getBean(ElasticsearchClient.class);
				client.index((b) -> b.index("foo").id("1").document(Map.of("a", "alpha", "b", "bravo")));
				GetResponse<Object> response = client.get((b) -> b.index("foo").id("1"), Object.class);
				assertThat(response).isNotNull();
				assertThat(response.found()).isTrue();
			});
	}

}
