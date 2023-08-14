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

import java.util.Set;

import org.apache.pulsar.client.api.ClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PulsarClientBuilderSslSettings}.
 *
 * @author Chris Bono
 */
class PulsarClientBuilderSslSettingsTests {

	private ClientBuilder clientBuilder;

	private PulsarClientBuilderSslSettings clientBuilderSslSettings;

	@BeforeEach
	void prepareForTest() {
		this.clientBuilder = mock(ClientBuilder.class);
		this.clientBuilderSslSettings = new PulsarClientBuilderSslSettings(this.clientBuilder);
	}

	@Test
	void adaptedClientBuilder() {
		assertThat(this.clientBuilderSslSettings.adaptedClientBuilder()).isSameAs(this.clientBuilder);
	}

	@SuppressWarnings("deprecation")
	@Test
	void enableTls() {
		this.clientBuilderSslSettings.enableTls(true);
		then(this.clientBuilder).should().enableTls(true);
	}

	@Test
	void tlsKeyFilePath() {
		this.clientBuilderSslSettings.tlsKeyFilePath("some/path");
		then(this.clientBuilder).should().tlsKeyFilePath("some/path");
	}

	@Test
	void tlsCertificateFilePath() {
		this.clientBuilderSslSettings.tlsCertificateFilePath("some/path");
		then(this.clientBuilder).should().tlsCertificateFilePath("some/path");

	}

	@Test
	void tlsTrustCertsFilePath() {
		this.clientBuilderSslSettings.tlsTrustCertsFilePath("some/path");
		then(this.clientBuilder).should().tlsTrustCertsFilePath("some/path");

	}

	@Test
	void allowTlsInsecureConnection() {
		this.clientBuilderSslSettings.allowTlsInsecureConnection(true);
		then(this.clientBuilder).should().allowTlsInsecureConnection(true);
	}

	@Test
	void enableTlsHostnameVerification() {
		this.clientBuilderSslSettings.enableTlsHostnameVerification(true);
		then(this.clientBuilder).should().enableTlsHostnameVerification(true);
	}

	@Test
	void useKeyStoreTls() {
		this.clientBuilderSslSettings.useKeyStoreTls(true);
		then(this.clientBuilder).should().useKeyStoreTls(true);
	}

	@Test
	void sslProvider() {
		this.clientBuilderSslSettings.sslProvider("p1");
		then(this.clientBuilder).should().sslProvider("p1");
	}

	@Test
	void tlsKeyStoreType() {
		this.clientBuilderSslSettings.tlsKeyStoreType("jks");
		then(this.clientBuilder).should().tlsKeyStoreType("jks");
	}

	@Test
	void tlsKeyStorePath() {
		this.clientBuilderSslSettings.tlsKeyStorePath("some/path");
		then(this.clientBuilder).should().tlsKeyStorePath("some/path");
	}

	@Test
	void tlsKeyStorePassword() {
		this.clientBuilderSslSettings.tlsKeyStorePassword("pwd");
		then(this.clientBuilder).should().tlsKeyStorePassword("pwd");
	}

	@Test
	void tlsTrustStoreType() {
		this.clientBuilderSslSettings.tlsKeyStorePath("pks");
		then(this.clientBuilder).should().tlsKeyStorePath("pks");
	}

	@Test
	void tlsTrustStorePath() {
		this.clientBuilderSslSettings.tlsTrustStorePath("some/path");
		then(this.clientBuilder).should().tlsTrustStorePath("some/path");
	}

	@Test
	void tlsTrustStorePassword() {
		this.clientBuilderSslSettings.tlsTrustStorePassword("pwd");
		then(this.clientBuilder).should().tlsTrustStorePassword("pwd");
	}

	@Test
	void tlsCiphers() {
		this.clientBuilderSslSettings.tlsCiphers(Set.of("c1", "c2"));
		then(this.clientBuilder).should().tlsCiphers(Set.of("c1", "c2"));
	}

	@Test
	void tlsProtocols() {
		this.clientBuilderSslSettings.tlsProtocols(Set.of("p1", "p2"));
		then(this.clientBuilder).should().tlsProtocols(Set.of("p1", "p2"));
	}

}
