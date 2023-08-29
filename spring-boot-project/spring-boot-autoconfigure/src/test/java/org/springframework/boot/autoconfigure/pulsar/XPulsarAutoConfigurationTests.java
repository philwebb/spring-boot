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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.pulsar.config.ConcurrentPulsarListenerContainerFactory;
import org.springframework.pulsar.core.PulsarAdministration;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.pulsar.function.PulsarFunctionAdministration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Autoconfiguration tests for {@link XPulsarAutoConfiguration}.
 *
 * @author Chris Bono
 * @author Alexander Preuß
 * @author Soby Chacko
 */
@SuppressWarnings("unchecked")
class XPulsarAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(XPulsarAutoConfiguration.class));

	@Nested
	class FunctionTests {

		@Test
		void functionSupportEnabledByDefault() {
			// NOTE: hasNoNullFieldsOrProperties() ensures object providers set
			XPulsarAutoConfigurationTests.this.contextRunner.run((context) -> assertThat(context).hasNotFailed()
				.getBean(PulsarFunctionAdministration.class)
				.hasFieldOrPropertyWithValue("failFast", Boolean.TRUE)
				.hasFieldOrPropertyWithValue("propagateFailures", Boolean.TRUE)
				.hasFieldOrPropertyWithValue("propagateStopFailures", Boolean.FALSE)
				.hasNoNullFieldsOrProperties()
				.extracting("pulsarAdministration")
				.isSameAs(context.getBean(PulsarAdministration.class)));
		}

		@Test
		void functionSupportCanBeConfigured() {
			XPulsarAutoConfigurationTests.this.contextRunner
				.withPropertyValues("spring.pulsar.function.fail-fast=false",
						"spring.pulsar.function.propagate-failures=false",
						"spring.pulsar.function.propagate-stop-failures=true")
				.run((context) -> assertThat(context).hasNotFailed()
					.getBean(PulsarFunctionAdministration.class)
					.hasFieldOrPropertyWithValue("failFast", Boolean.FALSE)
					.hasFieldOrPropertyWithValue("propagateFailures", Boolean.FALSE)
					.hasFieldOrPropertyWithValue("propagateStopFailures", Boolean.TRUE));
		}

		@Test
		void functionSupportCanBeDisabled() {
			XPulsarAutoConfigurationTests.this.contextRunner.withPropertyValues("spring.pulsar.function.enabled=false")
				.run((context) -> assertThat(context).hasNotFailed()
					.doesNotHaveBean(PulsarFunctionAdministration.class));
		}

		@Test
		void customFunctionAdminIsRespected() {
			PulsarFunctionAdministration customFunctionAdmin = mock(PulsarFunctionAdministration.class);
			XPulsarAutoConfigurationTests.this.contextRunner
				.withBean(PulsarFunctionAdministration.class, () -> customFunctionAdmin)
				.run((context) -> assertThat(context).hasNotFailed()
					.getBean(PulsarFunctionAdministration.class)
					.isSameAs(customFunctionAdmin));
		}

	}

	@Nested
	class ObservationTests {

		@Test
		void templateObservationsEnabledByDefault() {
			XPulsarAutoConfigurationTests.this.contextRunner
				.run((context) -> assertThat(context).getBean(PulsarTemplate.class)
					.hasFieldOrPropertyWithValue("observationEnabled", true));
		}

		@Test
		void templateObservationsEnabledExplicitly() {
			XPulsarAutoConfigurationTests.this.contextRunner
				.withPropertyValues("spring.pulsar.template.observations-enabled=true")
				.run((context) -> assertThat(context).getBean(PulsarTemplate.class)
					.hasFieldOrPropertyWithValue("observationEnabled", true));
		}

		@Test
		void templateObservationsCanBeDisabled() {
			XPulsarAutoConfigurationTests.this.contextRunner
				.withPropertyValues("spring.pulsar.template.observations-enabled=false")
				.run((context) -> assertThat(context).getBean(PulsarTemplate.class)
					.hasFieldOrPropertyWithValue("observationEnabled", false));
		}

		@Test
		void listenerObservationsEnabledByDefault() {
			XPulsarAutoConfigurationTests.this.contextRunner
				.run((context) -> assertThat(context).getBean(ConcurrentPulsarListenerContainerFactory.class)
					.hasFieldOrPropertyWithValue("containerProperties.observationEnabled", true));
		}

		@Test
		void listenerObservationsEnabledExplicitly() {
			XPulsarAutoConfigurationTests.this.contextRunner
				.withPropertyValues("spring.pulsar.listener.observations-enabled=true")
				.run((context) -> assertThat(context).getBean(ConcurrentPulsarListenerContainerFactory.class)
					.hasFieldOrPropertyWithValue("containerProperties.observationEnabled", true));
		}

		@Test
		void listenerObservationsCanBeDisabled() {
			XPulsarAutoConfigurationTests.this.contextRunner
				.withPropertyValues("spring.pulsar.listener.observations-enabled=false")
				.run((context) -> assertThat(context).getBean(ConcurrentPulsarListenerContainerFactory.class)
					.hasFieldOrPropertyWithValue("containerProperties.observationEnabled", false));
		}

	}

}
