/*
 * Copyright 2023-2023 the original author or authors.
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.PulsarClientException.UnsupportedAuthenticationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Client;
import org.springframework.pulsar.core.PulsarClientBuilderCustomizer;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PulsarClientBuilderCustomizers}.
 *
 * @author Chris Bono
 */
class PulsarClientBuilderConfigurerTests {

	@Test
	void singleCustomizerIsApplied() {
		PulsarClientBuilderCustomizer customizer = mock(PulsarClientBuilderCustomizer.class);
		PulsarClientBuilderCustomizers configurer = new PulsarClientBuilderCustomizers(new PulsarProperties(),
				List.of(customizer));
		ClientBuilder clientBuilder = mock(ClientBuilder.class);
		configurer.configure(clientBuilder);
		then(customizer).should().customize(clientBuilder);
	}

	@Test
	void multipleCustomizersAreAppliedInOrder() {
		PulsarClientBuilderCustomizer customizer1 = mock(PulsarClientBuilderCustomizer.class);
		PulsarClientBuilderCustomizer customizer2 = mock(PulsarClientBuilderCustomizer.class);
		PulsarClientBuilderCustomizers configurer = new PulsarClientBuilderCustomizers(new PulsarProperties(),
				List.of(customizer2, customizer1));
		ClientBuilder clientBuilder = mock(ClientBuilder.class);
		configurer.configure(clientBuilder);
		InOrder inOrder = inOrder(customizer1, customizer2);
		inOrder.verify(customizer2).customize(clientBuilder);
		inOrder.verify(customizer1).customize(clientBuilder);
	}

	@SuppressWarnings("deprecation")
	@Test
	void standardPropertiesAreApplied() {
		PulsarProperties pulsarProps = new PulsarProperties();
		Client clientProps = pulsarProps.getClient();
		clientProps.setServiceUrl("my-service-url");
		clientProps.setOperationTimeout(Duration.ofSeconds(1));
		clientProps.setLookupTimeout(Duration.ofSeconds(2));
		clientProps.setConnectionTimeout(Duration.ofSeconds(12));

		PulsarClientBuilderCustomizers configurer = new PulsarClientBuilderCustomizers(pulsarProps,
				Collections.emptyList());
		ClientBuilder clientBuilder = mock(ClientBuilder.class);
		configurer.configure(clientBuilder);

		then(clientBuilder).should().serviceUrl(clientProps.getServiceUrl());
		then(clientBuilder).should().operationTimeout(1000, TimeUnit.MILLISECONDS);
		then(clientBuilder).should().lookupTimeout(2000, TimeUnit.MILLISECONDS);
		then(clientBuilder).should().enableTls(true);
		then(clientBuilder).should().enableTlsHostnameVerification(true);
		then(clientBuilder).should().tlsTrustCertsFilePath("my-trust-certs-file-path");
		then(clientBuilder).should().tlsCertificateFilePath("my-certificate-file-path");
		then(clientBuilder).should().tlsKeyFilePath("my-key-file-path");
		then(clientBuilder).should().allowTlsInsecureConnection(true);
		then(clientBuilder).should().useKeyStoreTls(true);
		then(clientBuilder).should().sslProvider("my-ssl-provider");
		then(clientBuilder).should().tlsTrustStoreType("my-trust-store-type");
		then(clientBuilder).should().tlsTrustStorePath("my-trust-store-path");
		then(clientBuilder).should().tlsTrustStorePassword("my-trust-store-password");
		then(clientBuilder).should().tlsCiphers(Set.of("my-tls-cipher"));
		then(clientBuilder).should().tlsProtocols(Set.of("my-tls-protocol"));
		then(clientBuilder).should().connectionTimeout(12000, TimeUnit.MILLISECONDS);
	}

	@Test
	void customizerAppliedAfterProperties() {
		PulsarProperties pulsarProps = new PulsarProperties();
		Client clientProps = pulsarProps.getClient();
		clientProps.setServiceUrl("foo");

		PulsarClientBuilderCustomizer customizer = (clientBuilder) -> clientBuilder.serviceUrl("bar");
		PulsarClientBuilderCustomizers configurer = new PulsarClientBuilderCustomizers(pulsarProps, List.of(customizer));
		ClientBuilder clientBuilder = mock(ClientBuilder.class);
		configurer.configure(clientBuilder);

		InOrder inOrder = inOrder(clientBuilder);
		inOrder.verify(clientBuilder).serviceUrl("foo");
		inOrder.verify(clientBuilder).serviceUrl("bar");
	}

	@Nested
	class AuthenticationProperties {

		private final String authPluginClassName = "org.apache.pulsar.client.impl.auth.AuthenticationToken";

		private final String authParamsStr = "{\"token\":\"1234\"}";

		private final String authToken = "1234";

		@Test
		void usingAuthenticationMap() throws UnsupportedAuthenticationException {
			PulsarProperties pulsarProps = new PulsarProperties();
			Client clientProps = pulsarProps.getClient();
			clientProps.setAuthPluginClassName(this.authPluginClassName);
			clientProps.setAuthentication(Map.of("token", this.authToken));
			PulsarClientBuilderCustomizers configurer = new PulsarClientBuilderCustomizers(pulsarProps,
					Collections.emptyList());
			ClientBuilder clientBuilder = mock(ClientBuilder.class);
			configurer.configure(clientBuilder);
			then(clientBuilder).should().authentication(this.authPluginClassName, this.authParamsStr);
		}

	}

}
