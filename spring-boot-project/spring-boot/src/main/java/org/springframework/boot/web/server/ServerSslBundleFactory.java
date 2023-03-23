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

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslDetails;
import org.springframework.boot.ssl.certificate.CertificateFileSslDetails;
import org.springframework.boot.ssl.certificate.CertificateFileSslStoreProvider;
import org.springframework.boot.ssl.keystore.JavaKeyStoreSslDetails;
import org.springframework.boot.ssl.keystore.JavaKeyStoreSslStoreProvider;

/**
 * Creates an {@link SslBundle} based on server SSL configuration properties.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
@SuppressWarnings("removal")
public final class ServerSslBundleFactory {

	private ServerSslBundleFactory() {
	}

	/**
	 * Create an {@link SslBundle} if the appropriate server SSL properties are
	 * configured.
	 * @param ssl the SSL properties
	 * @param sslStoreProvider an {@code SslStoreProvider} to use when building the
	 * bundle, or {@code null} to create a provider from properties
	 * @return an {@code SslBundle} or {@code null}
	 * @deprecated since 3.1.0 for removal in 3.3.0
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public static SslBundle from(Ssl ssl, SslStoreProvider sslStoreProvider) {
		if (ssl != null && ssl.isEnabled()) {
			SslDetails sslDetails = mapProperties(ssl);
			org.springframework.boot.ssl.SslStoreProvider storeProvider;
			if (sslStoreProvider != null) {
				storeProvider = new SslStoreProviderAdapter(sslStoreProvider);
			}
			else {
				storeProvider = createSslStoreProvider(sslDetails);
			}
			return new SslBundle(sslDetails, storeProvider);
		}
		return null;
	}

	/**
	 * Create an {@link SslBundle} if the appropriate server SSL properties are
	 * configured.
	 * @param ssl the SSL properties
	 * @return an {@code SslBundle} or {@code null}
	 */
	public static SslBundle from(Ssl ssl) {
		if (ssl != null && ssl.isEnabled()) {
			SslDetails sslDetails = mapProperties(ssl);
			return new SslBundle(sslDetails, createSslStoreProvider(sslDetails));
		}
		return null;
	}

	private static org.springframework.boot.ssl.SslStoreProvider createSslStoreProvider(SslDetails sslDetails) {
		if (sslDetails instanceof CertificateFileSslDetails) {
			return CertificateFileSslStoreProvider.from(sslDetails);
		}
		if (sslDetails instanceof JavaKeyStoreSslDetails) {
			return JavaKeyStoreSslStoreProvider.from(sslDetails);
		}
		throw new IllegalStateException("KeyStore location must not be empty or null");
	}

	private static SslDetails mapProperties(Ssl ssl) {
		if (hasCertificateProperties(ssl)) {
			return mapCertificateProperties(ssl);
		}
		if (hasJavaKeyStoreProperties(ssl)) {
			return mapJksProperties(ssl);
		}
		SslDetails sslDetails = new SslDetails();
		mapSslProperties(ssl, sslDetails);
		return sslDetails;
	}

	private static boolean hasCertificateProperties(Ssl ssl) {
		return ssl.getCertificate() != null && ssl.getCertificatePrivateKey() != null;
	}

	private static boolean hasJavaKeyStoreProperties(Ssl ssl) {
		return ssl.getKeyStore() != null || (ssl.getKeyStoreType() != null && ssl.getKeyStoreType().equals("PKCS11"));
	}

	private static CertificateFileSslDetails mapCertificateProperties(Ssl ssl) {
		CertificateFileSslDetails sslDetails = new CertificateFileSslDetails();
		sslDetails.setCertificate(ssl.getCertificate());
		sslDetails.setCertificatePrivateKey(ssl.getCertificatePrivateKey());
		sslDetails.setTrustCertificate(ssl.getTrustCertificate());
		sslDetails.setTrustCertificatePrivateKey(ssl.getTrustCertificatePrivateKey());
		mapSslProperties(ssl, sslDetails);
		return sslDetails;
	}

	private static JavaKeyStoreSslDetails mapJksProperties(Ssl ssl) {
		JavaKeyStoreSslDetails sslDetails = new JavaKeyStoreSslDetails();
		sslDetails.setKeyStore(ssl.getKeyStore());
		sslDetails.setKeyStorePassword(ssl.getKeyStorePassword());
		sslDetails.setKeyStoreProvider(ssl.getKeyStoreProvider());
		sslDetails.setTrustStore(ssl.getTrustStore());
		sslDetails.setTrustStorePassword(ssl.getTrustStorePassword());
		sslDetails.setTrustStoreProvider(ssl.getTrustStoreProvider());
		mapSslProperties(ssl, sslDetails);
		return sslDetails;
	}

	private static void mapSslProperties(Ssl ssl, SslDetails sslDetails) {
		sslDetails.setCiphers(ssl.getCiphers());
		sslDetails.setEnabledProtocols(ssl.getEnabledProtocols());
		sslDetails.setKeyAlias(ssl.getKeyAlias());
		sslDetails.setKeyPassword(ssl.getKeyPassword());
		sslDetails.setKeyStoreType(ssl.getKeyStoreType());
		sslDetails.setTrustStoreType(ssl.getTrustStoreType());
		sslDetails.setProtocol(ssl.getProtocol());
	}

	static final class SslStoreProviderAdapter implements org.springframework.boot.ssl.SslStoreProvider {

		private final SslStoreProvider delegate;

		SslStoreProviderAdapter(SslStoreProvider delegate) {
			this.delegate = delegate;
		}

		@Override
		public KeyStore getKeyStore() throws Exception {
			return this.delegate.getKeyStore();
		}

		@Override
		public KeyStore getTrustStore() throws Exception {
			return this.delegate.getTrustStore();
		}

		@Override
		public String getKeyPassword() {
			return this.delegate.getKeyPassword();
		}

	}

}
