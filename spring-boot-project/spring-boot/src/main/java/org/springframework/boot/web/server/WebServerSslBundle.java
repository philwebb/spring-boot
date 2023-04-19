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

import java.security.KeyStore;
import java.util.Collections;
import java.util.Set;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslKeyReference;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.util.function.ThrowingSupplier;

/**
 * @author Scott Frederick
 * @author Phillip Webb
 */
public class WebServerSslBundle implements SslBundle {

	private final SslKeyReference key;

	private final SslStoreBundle stores;

	private final SslManagerBundle managers;

	private final SslOptions options;

	@SuppressWarnings({ "removal", "deprecation" })
	WebServerSslBundle(Ssl ssl, SslStoreProvider sslStoreProvider) {
		String keyPassword = (sslStoreProvider != null) ? sslStoreProvider.getKeyPassword() : ssl.getKeyPassword();
		this.key = SslKeyReference.of(ssl.getKeyAlias(), keyPassword);
		this.stores = (sslStoreProvider != null) ? new SslStoreProviderBundleAdapter(sslStoreProvider)
				: createStoreBundle(ssl);
		this.managers = SslManagerBundle.from(this.stores, this.key);
		Set<String> enabledProtocols = (ssl.getEnabledProtocols() != null) ? Set.of(ssl.getEnabledProtocols())
				: Collections.emptySet();
		Set<String> ciphers = (ssl.getCiphers() != null) ? Set.of(ssl.getCiphers()) : Collections.emptySet();
		this.options = new SslOptions() {

			@Override
			public Set<String> getEnabledProtocols() {
				return enabledProtocols;
			}

			@Override
			public Set<String> getCiphers() {
				return ciphers;
			}
		};
	}

	private static SslStoreBundle createStoreBundle(Ssl ssl) {
		if (hasCertificateProperties(ssl)) {
			return createPemStoreBundle(ssl);
		}
		if (hasJavaKeyStoreProperties(ssl)) {
			return createJksStoreBundle(ssl);
		}
		return SslStoreBundle.NONE;
	}

	private static boolean hasCertificateProperties(Ssl ssl) {
		return ssl.getCertificate() != null && ssl.getCertificatePrivateKey() != null;
	}

	private static SslStoreBundle createPemStoreBundle(Ssl ssl) {
		PemSslStoreBundle.StoreDetails keyStoreDetails = new PemSslStoreBundle.StoreDetails(ssl.getKeyStoreType(),
				ssl.getCertificate(), ssl.getCertificatePrivateKey());
		PemSslStoreBundle.StoreDetails trustStoreDetails = new PemSslStoreBundle.StoreDetails(ssl.getTrustStoreType(),
				ssl.getTrustCertificate(), ssl.getTrustCertificatePrivateKey());
		return new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
	}

	private static boolean hasJavaKeyStoreProperties(Ssl ssl) {
		return ssl.getKeyStore() != null || (ssl.getKeyStoreType() != null && ssl.getKeyStoreType().equals("PKCS11"));
	}

	private static SslStoreBundle createJksStoreBundle(Ssl ssl) {
		JksSslStoreBundle.StoreDetails keyStoreDetails = new JksSslStoreBundle.StoreDetails(ssl.getKeyStoreType(),
				ssl.getKeyStoreProvider(), ssl.getKeyStore(), ssl.getKeyStorePassword());
		JksSslStoreBundle.StoreDetails trustStoreDetails = new JksSslStoreBundle.StoreDetails(ssl.getTrustStoreType(),
				ssl.getTrustStoreProvider(), ssl.getTrustStore(), ssl.getTrustStorePassword());
		return new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
	}

	@Override
	public SslKeyReference getKey() {
		return this.key;
	}

	@Override
	public SslStoreBundle getStores() {
		return this.stores;
	}

	@Override
	public SslManagerBundle getManagers() {
		return this.managers;
	}

	@Override
	public SslOptions getOptions() {
		return this.options;
	}

	/**
	 * Class to adapt a {@link SslStoreProvider} into a {@link SslStoreBundle}.
	 */
	@SuppressWarnings({ "removal", "deprecation" })
	private static class SslStoreProviderBundleAdapter implements SslStoreBundle {

		private final SslStoreProvider sslStoreProvider;

		SslStoreProviderBundleAdapter(SslStoreProvider sslStoreProvider) {
			this.sslStoreProvider = sslStoreProvider;
		}

		@Override
		public KeyStore getKeyStore() {
			return ThrowingSupplier.of(this.sslStoreProvider::getKeyStore).get();
		}

		@Override
		public String getKeyStorePassword() {
			return null;
		}

		@Override
		public KeyStore getTrustStore() {
			return ThrowingSupplier.of(this.sslStoreProvider::getTrustStore).get();
		}

	}

}
