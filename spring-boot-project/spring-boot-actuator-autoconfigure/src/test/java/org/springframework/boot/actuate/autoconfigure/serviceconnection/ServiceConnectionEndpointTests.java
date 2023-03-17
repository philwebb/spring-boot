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
import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinConnectionDetails;
import org.springframework.boot.autoconfigure.amqp.RabbitConnectionDetails;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.autoconfigure.mongo.MongoConnectionDetails;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
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
		.withBean("elasticsearchServiceConnection", ElasticsearchConnectionDetails.class,
				() -> mock(ElasticsearchConnectionDetails.class))
		.withBean("mongoServiceConnection", MongoConnectionDetails.class, () -> mock(MongoConnectionDetails.class))
		.withBean("redisServiceConnection", RedisConnectionDetails.class, () -> mock(RedisConnectionDetails.class))
		.withBean("jdbcServiceConnection", JdbcConnectionDetails.class, () -> mock(JdbcConnectionDetails.class))
		.withBean("r2dbcServiceConnection", R2dbcConnectionDetails.class, () -> mock(R2dbcConnectionDetails.class))
		.withBean("rabbitServiceConnection", RabbitConnectionDetails.class, () -> mock(RabbitConnectionDetails.class))
		.withBean("zipkinServiceConnection", ZipkinConnectionDetails.class, () -> mock(ZipkinConnectionDetails.class));

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
