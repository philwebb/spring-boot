/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics.integration;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * Tests for {@link MetricsIntegrationAutoConfiguration}.
 *
 * @author Phillip Webb
 */
public class MetricsIntegrationAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(IntegrationAutoConfiguration.class,
					MetricsIntegrationAutoConfiguration.class))
			.withUserConfiguration(BaseConfiguration.class);

	@Test
	public void autoConfiguredIntegrationIsInstrumented() {
		this.contextRunner.run((context) -> {
			Message<?> message = MessageBuilder.withPayload("hello").build();
			context.getBean(TestGateway.class).exchange(message);
		});
	}

	@Configuration
	@IntegrationComponentScan
	static class BaseConfiguration {

		@Bean
		public SimpleMeterRegistry simpleMeterRegistry() {
			return new SimpleMeterRegistry();
		}

	}

	@MessagingGateway
	public interface TestGateway extends RequestReplyExchanger {

	}

}
