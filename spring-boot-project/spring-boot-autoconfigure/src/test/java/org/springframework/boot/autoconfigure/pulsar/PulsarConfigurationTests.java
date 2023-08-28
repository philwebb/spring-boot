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

package org.springframework.boot.autoconfigure.pulsar;

import org.apache.pulsar.client.api.PulsarClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.pulsar.core.PulsarAdministration;
import org.springframework.pulsar.core.PulsarClientBuilderCustomizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PulsarConfiguration}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
class PulsarConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PulsarConfiguration.class))
		.withUserConfiguration(PulsarClientBuilderCustomizerConfiguration.class);

	@Nested
	class ClientTests {

		@Test
		void whenHasUserDefinedBeanDoesNotAutoConfigureBean() {
			PulsarClient customClient = mock(PulsarClient.class);
			new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(PulsarConfiguration.class))
				.withBean("customPulsarClient", PulsarClient.class, () -> customClient)
				.run((context) -> assertThat(context).getBean(PulsarClient.class).isSameAs(customClient));
		}

		@Test
		void whenHasUseDefinedCustomizersAppliesInCorrectOrder() {
			new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(PulsarConfiguration.class))
				.withUserConfiguration(ClientCustomizersTestConfiguration.class)
				.run((context) -> {
					PulsarClientBuilderCustomizer customizer1 = context.getBean("clientCustomizerBar",
							PulsarClientBuilderCustomizer.class);
					PulsarClientBuilderCustomizer customizer2 = context.getBean("clientCustomizerFoo",
							PulsarClientBuilderCustomizer.class);
					InOrder ordered = inOrder(customizer1, customizer2);
					ordered.verify(customizer1).customize(any());
					ordered.verify(customizer2).customize(any());
				});
		}

		@Configuration(proxyBeanMethods = false)
		static class ClientCustomizersTestConfiguration {

			@Bean
			@Order(200)
			PulsarClientBuilderCustomizer clientCustomizerFoo() {
				return mock(PulsarClientBuilderCustomizer.class);
			}

			@Bean
			@Order(100)
			PulsarClientBuilderCustomizer clientCustomizerBar() {
				return mock(PulsarClientBuilderCustomizer.class);
			}

		}

	}

	@Nested
	class AdministrationTests {

		private final ApplicationContextRunner contextRunner = PulsarConfigurationTests.this.contextRunner;

		@Test
		void whenHasUserDefinedBeanDoesNotAutoConfigureBean() {
			PulsarAdministration pulsarAdministration = mock(PulsarAdministration.class);
			this.contextRunner
				.withBean("customPulsarAdministration", PulsarAdministration.class, () -> pulsarAdministration)
				.run((context) -> assertThat(context).hasNotFailed()
					.getBean(PulsarAdministration.class)
					.isSameAs(pulsarAdministration));
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	static class PulsarClientBuilderCustomizerConfiguration {

		@Bean
		PulsarClient pulsarClient() {
			// Use a mock because the real PulsarClient is very slow to close
			return mock(PulsarClient.class);
		}

	}

}
