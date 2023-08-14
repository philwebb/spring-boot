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

import org.apache.pulsar.client.admin.PulsarAdminBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PulsarAdminBuilderSslSettings}.
 *
 * @author Chris Bono
 */
public class PulsarAdminBuilderSslSettingsTests {

	private PulsarAdminBuilder adminBuilder;

	private PulsarAdminBuilderSslSettings adminBuilderSslSettings;

	@BeforeEach
	void prepareForTest() {
		this.adminBuilder = mock(PulsarAdminBuilder.class);
		this.adminBuilderSslSettings = new PulsarAdminBuilderSslSettings(this.adminBuilder);
	}

	@Test
	void adaptedClientBuilder() {
		assertThat(this.adminBuilderSslSettings.adaptedClientBuilder()).isSameAs(this.adminBuilder);
	}

	@SuppressWarnings("deprecation")
	@Test
	void enableTls() {
		this.adminBuilderSslSettings.enableTls(true);
		then(this.adminBuilder).shouldHaveNoInteractions();
	}

	@Test
	void tlsKeyFilePath() {
		this.adminBuilderSslSettings.tlsKeyFilePath("some/path");
		then(this.adminBuilder).should().tlsKeyFilePath("some/path");
	}

	@Test
	void tlsCertificateFilePath() {
		this.adminBuilderSslSettings.tlsCertificateFilePath("some/path");
		then(this.adminBuilder).should().tlsCertificateFilePath("some/path");

	}

	@Test
	void tlsTrustCertsFilePath() {
		this.adminBuilderSslSettings.tlsTrustCertsFilePath("some/path");
		then(this.adminBuilder).should().tlsTrustCertsFilePath("some/path");

	}

	@Test
	void allowTlsInsecureConnection() {
		this.adminBuilderSslSettings.allowTlsInsecureConnection(true);
		then(this.adminBuilder).should().allowTlsInsecureConnection(true);
	}

	@Test
	void enableTlsHostnameVerification() {
		this.adminBuilderSslSettings.enableTlsHostnameVerification(true);
		then(this.adminBuilder).should().enableTlsHostnameVerification(true);
	}

	@Test
	void useKeyStoreTls() {
		this.adminBuilderSslSettings.useKeyStoreTls(true);
		then(this.adminBuilder).should().useKeyStoreTls(true);
	}

	@Test
	void sslProvider() {
		this.adminBuilderSslSettings.sslProvider("p1");
		then(this.adminBuilder).should().sslProvider("p1");
	}

	@Test
	void tlsKeyStoreType() {
		this.adminBuilderSslSettings.tlsKeyStoreType("jks");
		then(this.adminBuilder).should().tlsKeyStoreType("jks");
	}

	@Test
	void tlsKeyStorePath() {
		this.adminBuilderSslSettings.tlsKeyStorePath("some/path");
		then(this.adminBuilder).should().tlsKeyStorePath("some/path");
	}

	@Test
	void tlsKeyStorePassword() {
		this.adminBuilderSslSettings.tlsKeyStorePassword("pwd");
		then(this.adminBuilder).should().tlsKeyStorePassword("pwd");
	}

	@Test
	void tlsTrustStoreType() {
		this.adminBuilderSslSettings.tlsKeyStorePath("pks");
		then(this.adminBuilder).should().tlsKeyStorePath("pks");
	}

	@Test
	void tlsTrustStorePath() {
		this.adminBuilderSslSettings.tlsTrustStorePath("some/path");
		then(this.adminBuilder).should().tlsTrustStorePath("some/path");
	}

	@Test
	void tlsTrustStorePassword() {
		this.adminBuilderSslSettings.tlsTrustStorePassword("pwd");
		then(this.adminBuilder).should().tlsTrustStorePassword("pwd");
	}

	@Test
	void tlsCiphers() {
		this.adminBuilderSslSettings.tlsCiphers(Set.of("c1", "c2"));
		then(this.adminBuilder).should().tlsCiphers(Set.of("c1", "c2"));
	}

	@Test
	void tlsProtocols() {
		this.adminBuilderSslSettings.tlsProtocols(Set.of("p1", "p2"));
		then(this.adminBuilder).should().tlsProtocols(Set.of("p1", "p2"));
	}

}
