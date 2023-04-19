/*
 * Copyright 2023 the original author or authors.
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

package org.springframework.boot.ssl;

import javax.net.ssl.KeyManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.ssl.pem.CertificateFileSslDetails;
import org.springframework.boot.ssl.pem.CertificateFileSslStoreProvider;
import org.springframework.boot.sslx.keystore.JavaKeyStoreSslDetails;
import org.springframework.boot.sslx.keystore.JavaKeyStoreSslStoreProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link SslManagerBundle}.
 *
 * @author Scott Frederick
 */
@ExtendWith(MockitoExtension.class)
class XSslManagerBundleTests {

	@Mock
	private SslStoreProvider storeProvider;

	@Test
	void getFactoriesWhenKeyStoreIsReturnsManagers() throws Exception {
		given(this.storeProvider.getKeyStore()).willReturn(null);
		given(this.storeProvider.getTrustStore()).willReturn(null);
		SslManagerBundle managers = createSslManagers(this.storeProvider, null, null);
		assertThat(managers.getKeyManagerFactory()).isNotNull();
		assertThat(managers.getKeyManagers()).isNotNull();
		assertThat(managers.getTrustManagerFactory()).isNotNull();
		assertThat(managers.getTrustManagers()).isNotNull();
	}

	@Test
	void getKeyManagerFactoryWhenAliasIsNotNullShouldWrap() throws Exception {
		CertificateFileSslDetails ssl = new CertificateFileSslDetails();
		ssl.setCertificate("classpath:test-cert.pem");
		ssl.setCertificatePrivateKey("classpath:test-key.pem");
		ssl.setKeyAlias("spring-boot");
		SslManagerBundle managers = createSslManagers(CertificateFileSslStoreProvider.from(ssl), null, ssl.getKeyAlias());
		KeyManager[] keyManagers = managers.getKeyManagerFactory().getKeyManagers();
		Class<?> wrapper = Class.forName("org.springframework.boot.ssl.SslManagers$ConfigurableAliasKeyManager");
		assertThat(keyManagers[0]).isInstanceOf(wrapper);
	}

	@Test
	void getKeyManagerFactoryWhenAliasIsNullShouldNotWrap() throws Exception {
		JavaKeyStoreSslDetails ssl = new JavaKeyStoreSslDetails();
		ssl.setKeyStore("classpath:test.jks");
		ssl.setKeyPassword("password");
		SslManagerBundle managers = createSslManagers(JavaKeyStoreSslStoreProvider.from(ssl), ssl.getKeyPassword(), null);
		KeyManager[] keyManagers = managers.getKeyManagerFactory().getKeyManagers();
		Class<?> wrapper = Class.forName("org.springframework.boot.ssl.SslManagers$ConfigurableAliasKeyManager");
		assertThat(keyManagers[0]).isNotInstanceOf(wrapper);
	}

	private static SslManagerBundle createSslManagers(SslStoreProvider sslStoreProvider, String keyPassword,
			String keyAlias) {
		SslKeyStores keyStores = new SslKeyStores(sslStoreProvider, keyPassword);
		return new SslManagerBundle(keyStores, keyAlias);
	}

}
