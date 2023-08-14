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

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.admin.PulsarAdminBuilder;
import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.PulsarClientException.UnsupportedAuthenticationException;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Admin;
import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PulsarAdminBuilderConfigurer}.
 *
 * @author Chris Bono
 */
class PulsarAdminBuilderConfigurerTests {

	@Test
	void standardPropertiesAreApplied() {
		PulsarProperties pulsarProps = new PulsarProperties();
		Admin adminProps = pulsarProps.getAdministration();
		adminProps.setServiceUrl("my-service-url");
		adminProps.setConnectionTimeout(Duration.ofSeconds(1));
		adminProps.setReadTimeout(Duration.ofSeconds(2));
		adminProps.setRequestTimeout(Duration.ofSeconds(3));
		PulsarAdminBuilderConfigurer configurer = new PulsarAdminBuilderConfigurer(pulsarProps,
				mock(ClientBuilderSslConfigurer.class));
		PulsarAdminBuilder adminBuilder = mock(PulsarAdminBuilder.class);
		configurer.configure(adminBuilder);
		then(adminBuilder).should().serviceHttpUrl("my-service-url");
		then(adminBuilder).should().connectionTimeout(1000, TimeUnit.MILLISECONDS);
		then(adminBuilder).should().readTimeout(2000, TimeUnit.MILLISECONDS);
		then(adminBuilder).should().requestTimeout(3000, TimeUnit.MILLISECONDS);
	}

	@Test
	void authenticationIsApplied() throws UnsupportedAuthenticationException {
		String authPluginClassName = "org.apache.pulsar.client.impl.auth.AuthenticationToken";
		String authToken = "1234";
		String authParamsStr = "{\"token\":\"1234\"}";
		PulsarProperties pulsarProps = new PulsarProperties();
		Client clientProps = pulsarProps.getClient();
		clientProps.setAuthPluginClassName(authPluginClassName);
		clientProps.setAuthentication(Map.of("token", authToken));
		PulsarClientBuilderConfigurer configurer = new PulsarClientBuilderConfigurer(pulsarProps,
				Collections.emptyList(), mock(ClientBuilderSslConfigurer.class));
		ClientBuilder clientBuilder = mock(ClientBuilder.class);
		configurer.configure(clientBuilder);
		then(clientBuilder).should().authentication(authPluginClassName, authParamsStr);
	}

	@Test
	void sslConfigurerIsApplied() {
		PulsarProperties pulsarProps = new PulsarProperties();
		ClientBuilderSslConfigurer sslConfigurer = mock(ClientBuilderSslConfigurer.class);
		PulsarAdminBuilderConfigurer configurer = new PulsarAdminBuilderConfigurer(pulsarProps, sslConfigurer);
		PulsarAdminBuilder adminBuilder = mock(PulsarAdminBuilder.class);
		configurer.configure(adminBuilder);
		then(sslConfigurer).should().applySsl(assertArg((sslSettings) -> {
			assertThat(sslSettings).isInstanceOf(PulsarAdminBuilderSslSettings.class);
			assertThat(sslSettings.adaptedClientBuilder()).isSameAs(adminBuilder);
		}), same(pulsarProps.getAdministration().getSsl()));
	}

}
