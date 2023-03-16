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

package org.springframework.boot.devservices.dockercompose.rabbit;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.amqp.RabbitServiceConnection;
import org.springframework.boot.autoconfigure.amqp.RabbitServiceConnection.Address;
import org.springframework.boot.devservices.dockercompose.AbstractIntegrationTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for RabbitMQ.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class RabbitIntegrationTests extends AbstractIntegrationTests {

	@Test
	void test() {
		RabbitServiceConnection serviceConnection = runProvider(RabbitServiceConnection.class);
		assertThat(serviceConnection.getName()).isEqualTo("docker-compose-rabbit-rabbitmq");
		assertThat(serviceConnection.getUsername()).isEqualTo("myuser");
		assertThat(serviceConnection.getPassword()).isEqualTo("secret");
		assertThat(serviceConnection.getVirtualHost()).isEqualTo("/");
		assertThat(serviceConnection.getAddresses()).hasSize(1);
		Address address = serviceConnection.getFirstAddress();
		assertThat(address.host()).isNotNull();
		assertThat(address.port()).isGreaterThan(0);
	}

	@Override
	protected InputStream getComposeContent() {
		return RabbitIntegrationTests.class.getResourceAsStream("rabbitmq-compose.yaml");
	}

}
