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

package org.springframework.boot.actuate.autoconfigure.serviceconnection;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.serviceconnection.ServiceConnectionEndpoint.ServiceConnectionsDto;
import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinServiceConnection;
import org.springframework.boot.autoconfigure.amqp.RabbitServiceConnection;
import org.springframework.boot.autoconfigure.data.redis.RedisServiceConnection;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchServiceConnection;
import org.springframework.boot.autoconfigure.jdbc.JdbcServiceConnection;
import org.springframework.boot.autoconfigure.mongo.MongoServiceConnection;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcServiceConnection;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServiceConnectionEndpoint}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class ServiceConnectionEndpointTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean("endpoint", ServiceConnectionEndpoint.class)
		.withBean("elasticsearchServiceConnection", ElasticsearchServiceConnection.class,
				() -> mock(ElasticsearchServiceConnection.class))
		.withBean("mongoServiceConnection", MongoServiceConnection.class, () -> mock(MongoServiceConnection.class))
		.withBean("redisServiceConnection", RedisServiceConnection.class, () -> mock(RedisServiceConnection.class))
		.withBean("jdbcServiceConnection", JdbcServiceConnection.class, () -> mock(JdbcServiceConnection.class))
		.withBean("r2dbcServiceConnection", R2dbcServiceConnection.class, () -> mock(R2dbcServiceConnection.class))
		.withBean("rabbitServiceConnection", RabbitServiceConnection.class, () -> mock(RabbitServiceConnection.class))
		.withBean("zipkinServiceConnection", ZipkinServiceConnection.class, () -> mock(ZipkinServiceConnection.class));

	@Test
	void serviceConnectionsAreProduced() {
		this.contextRunner.run((context) -> {
			ServiceConnectionsDto serviceConnections = context.getBean(ServiceConnectionEndpoint.class)
				.serviceConnections();
			assertThat(serviceConnections.elasticsearch()).isNotEmpty();
			assertThat(serviceConnections.mongo()).isNotEmpty();
			assertThat(serviceConnections.redis()).isNotEmpty();
			assertThat(serviceConnections.jdbc()).isNotEmpty();
			assertThat(serviceConnections.r2dbc()).isNotEmpty();
			assertThat(serviceConnections.rabbit()).isNotEmpty();
			assertThat(serviceConnections.zipkin()).isNotEmpty();
		});
	}

}
