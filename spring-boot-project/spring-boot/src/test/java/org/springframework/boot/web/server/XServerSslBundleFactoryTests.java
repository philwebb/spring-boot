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

package org.springframework.boot.web.server;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.junit.jupiter.api.Test;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslDetails;
import org.springframework.boot.ssl.SslKeyStores;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServerSslBundleFactory};
 *
 * @author Scott Frederick
 */
class XServerSslBundleFactoryTests {

	@Test
	void createBundleWithSslDisabled() {
		Ssl ssl = new Ssl();
		ssl.setEnabled(false);
		SslBundle bundle = ServerSslBundleFactory.from(ssl);
		assertThat(bundle).isNull();
	}

	@Test
	void createsSslBundleFromJksProperties() {
		Ssl ssl = new Ssl();
		ssl.setKeyStore("classpath:test.p12");
		ssl.setKeyStorePassword("secret");
		ssl.setKeyStoreType("PKCS12");
		ssl.setTrustStore("classpath:test.p12");
		ssl.setTrustStorePassword("secret");
		ssl.setTrustStoreType("PKCS12");
		ssl.setKeyAlias("alias");
		ssl.setKeyPassword("password");
		ssl.setClientAuth(Ssl.ClientAuth.NONE);
		ssl.setCiphers(new String[] { "ONE", "TWO", "THREE" });
		ssl.setEnabledProtocols(new String[] { "TLSv1.1", "TLSv1.2" });
		ssl.setProtocol("TLSv1.1");
		SslBundle bundle = ServerSslBundleFactory.from(ssl);
		assertThat(bundle).isNotNull();
		SslKeyStores keyStores = bundle.getKeyStores();
		assertThat(keyStores.getKeyStorePassword()).isEqualTo("secret");
		assertThat(keyStores.getKeyPassword()).isEqualTo("password");
		assertThat(keyStores.getKeyStore()).isNotNull();
		assertThat(keyStores.getTrustStore()).isNotNull();
		assertPropertiesMapped(ssl, bundle.getDetails());
	}

	@Test
	void createsSslBundleFromJksPropertiesWithPkcs11StoreType() {
		Ssl ssl = new Ssl();
		ssl.setKeyStorePassword("secret");
		ssl.setKeyStoreType("PKCS11");
		ssl.setKeyPassword("password");
		ssl.setClientAuth(Ssl.ClientAuth.NONE);
		SslBundle bundle = ServerSslBundleFactory.from(ssl);
		assertThat(bundle).isNotNull();
		SslKeyStores keyStores = bundle.getKeyStores();
		assertThat(keyStores.getKeyStorePassword()).isEqualTo("secret");
		assertThat(keyStores.getKeyPassword()).isEqualTo("password");
		assertPropertiesMapped(ssl, bundle.getDetails());
	}

	@Test
	void createsSslBundleFromCertificateProperties() {
		Ssl ssl = new Ssl();
		ssl.setCertificate("classpath:test-cert.pem");
		ssl.setCertificatePrivateKey("classpath:test-key.pem");
		ssl.setTrustCertificate("classpath:test-cert-chain.pem");
		ssl.setKeyStoreType("PKCS12");
		ssl.setTrustStoreType("PKCS12");
		ssl.setKeyPassword("password");
		ssl.setClientAuth(Ssl.ClientAuth.NONE);
		ssl.setCiphers(new String[] { "ONE", "TWO", "THREE" });
		ssl.setEnabledProtocols(new String[] { "TLSv1.1", "TLSv1.2" });
		ssl.setProtocol("TLSv1.1");
		SslBundle bundle = ServerSslBundleFactory.from(ssl);
		assertThat(bundle).isNotNull();
		SslKeyStores keyStores = bundle.getKeyStores();
		assertThat(keyStores.getKeyStorePassword()).isNull();
		assertThat(keyStores.getKeyPassword()).isEqualTo("");
		assertThat(keyStores.getKeyStore()).isNotNull();
		assertThat(keyStores.getTrustStore()).isNotNull();
		assertPropertiesMapped(ssl, bundle.getDetails());
	}

	@Test
	@SuppressWarnings("removal")
	void createBundleWithCustomSslStoreProvider() throws Exception {
		SslStoreProvider sslStoreProvider = mock(SslStoreProvider.class);
		KeyStore keyStore = loadStore();
		given(sslStoreProvider.getKeyStore()).willReturn(keyStore);
		given(sslStoreProvider.getTrustStore()).willReturn(keyStore);
		Ssl ssl = new Ssl();
		ssl.setKeyStoreType("PKCS12");
		ssl.setTrustStoreType("PKCS12");
		ssl.setKeyPassword("password");
		ssl.setClientAuth(Ssl.ClientAuth.NONE);
		ssl.setCiphers(new String[] { "ONE", "TWO", "THREE" });
		ssl.setEnabledProtocols(new String[] { "TLSv1.1", "TLSv1.2" });
		ssl.setProtocol("TLSv1.1");
		SslBundle bundle = ServerSslBundleFactory.from(ssl, sslStoreProvider);
		assertThat(bundle).isNotNull();
		SslKeyStores keyStores = bundle.getKeyStores();
		assertThat(keyStores.getKeyPassword()).isEqualTo("password");
		assertThat(keyStores.getKeyStore()).isNotNull();
		assertThat(keyStores.getTrustStore()).isNotNull();
		assertPropertiesMapped(ssl, bundle.getDetails());
	}

	@Test
	void createBundleWithMissingPropertiesThrowsException() {
		Ssl ssl = new Ssl();
		assertThatIllegalStateException().isThrownBy(() -> ServerSslBundleFactory.from(ssl))
			.withMessageContaining("KeyStore location must not be empty or null");
	}

	private void assertPropertiesMapped(Ssl ssl, SslDetails sslDetails) {
		assertThat(sslDetails.getKeyAlias()).isEqualTo(ssl.getKeyAlias());
		assertThat(sslDetails.getKeyPassword()).isEqualTo(ssl.getKeyPassword());
		assertThat(sslDetails.getCiphers()).isEqualTo(ssl.getCiphers());
		assertThat(sslDetails.getEnabledProtocols()).isEqualTo(ssl.getEnabledProtocols());
		assertThat(sslDetails.getKeyStoreType()).isEqualTo(ssl.getKeyStoreType());
		assertThat(sslDetails.getTrustStoreType()).isEqualTo(ssl.getTrustStoreType());
		assertThat(sslDetails.getProtocol()).isEqualTo(ssl.getProtocol());
	}

	private KeyStore loadStore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
		Resource resource = new ClassPathResource("test.p12");
		try (InputStream stream = resource.getInputStream()) {
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(stream, "secret".toCharArray());
			return keyStore;
		}
	}

}
