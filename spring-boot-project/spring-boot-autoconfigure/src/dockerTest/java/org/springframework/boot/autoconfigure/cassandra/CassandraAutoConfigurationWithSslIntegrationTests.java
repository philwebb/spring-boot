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

package org.springframework.boot.autoconfigure.cassandra;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CassandraAutoConfiguration} that uses SSL for client-server
 * communication.
 *
 * @author Moritz Halbritter
 */
@Testcontainers(disabledWithoutDocker = true)
class CassandraAutoConfigurationWithSslIntegrationTests {

	@Container
	static final SslCassandraContainer cassandra = TestImage.container(SslCassandraContainer.class)
		.withStartupTimeout(Duration.ofMinutes(10));

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(CassandraAutoConfiguration.class, SslAutoConfiguration.class))
		.withPropertyValues(
				"spring.ssl.bundle.pem.mybundle.truststore.certificate=classpath:org/springframework/boot/autoconfigure/cassandra/certificate.pem",
				"spring.cassandra.contact-points:" + cassandra.getHost() + ":" + cassandra.getFirstMappedPort(),
				"spring.cassandra.local-datacenter=datacenter1", "spring.cassandra.connection.connect-timeout=60s",
				"spring.cassandra.connection.init-query-timeout=60s", "spring.cassandra.request.timeout=60s",
				"spring.cassandra.ssl.enabled=true", "spring.cassandra.ssl.bundle=mybundle");

	@Test
	void sslConnectionWorks() {
		this.contextRunner.run((context) -> {
			SimpleStatement select = SimpleStatement.newInstance("SELECT release_version FROM system.local")
				.setConsistencyLevel(ConsistencyLevel.LOCAL_ONE);
			assertThat(context.getBean(CqlSession.class).execute(select).one()).isNotNull();
		});
	}

	static final class SslCassandraContainer extends CassandraContainer<SslCassandraContainer> {

		SslCassandraContainer(DockerImageName dockerImageName) {
			super(dockerImageName);
		}

		@Override
		protected void containerIsCreated(String containerId) {
			String config = copyFileFromContainer("/etc/cassandra/cassandra.yaml",
					(stream) -> StreamUtils.copyToString(stream, StandardCharsets.UTF_8));
			config = config + """
					client_encryption_options:
					  enabled: true
					  keystore: /etc/cassandra/ssl.jks
					  keystore_password: cassandra
					""";
			copyFileToContainer(Transferable.of(config), "/etc/cassandra/cassandra.yaml");
			try {
				byte[] keystore = new ClassPathResource("org/springframework/boot/autoconfigure/cassandra/keystore.jks")
					.getContentAsByteArray();
				copyFileToContainer(Transferable.of(keystore), "/etc/cassandra/ssl.jks");
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

	}

}
