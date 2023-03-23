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

package org.springframework.boot.ssl.certificate;

import java.security.KeyStore;

import org.springframework.boot.ssl.SslDetails;
import org.springframework.boot.ssl.SslStoreProvider;

/**
 * An {@link SslStoreProvider} that creates key and trust stores from certificate and
 * private key PEM files.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
public final class CertificateFileSslStoreProvider extends CertificateSslStoreProvider {

	private final CertificateFileSslDetails ssl;

	private CertificateFileSslStoreProvider(CertificateFileSslDetails ssl) {
		this.ssl = ssl;
	}

	@Override
	public KeyStore getKeyStore() throws Exception {
		if (this.ssl.getCertificate() == null) {
			return null;
		}
		return createKeyStoreFromResources(this.ssl.getCertificate(), this.ssl.getCertificatePrivateKey(),
				this.ssl.getKeyStoreType(), this.ssl.getKeyAlias());
	}

	@Override
	public KeyStore getTrustStore() throws Exception {
		if (this.ssl.getTrustCertificate() == null) {
			return null;
		}
		return createKeyStoreFromResources(this.ssl.getTrustCertificate(), this.ssl.getTrustCertificatePrivateKey(),
				this.ssl.getTrustStoreType(), this.ssl.getKeyAlias());
	}

	/**
	 * Create an {@link SslStoreProvider} if the appropriate SSL details are configured.
	 * @param ssl the SSL details
	 * @return an {@code SslStoreProvider} or {@code null}
	 */
	public static SslStoreProvider from(SslDetails ssl) {
		if (ssl instanceof CertificateFileSslDetails certificateSsl) {
			return new CertificateFileSslStoreProvider(certificateSsl);
		}
		return null;
	}

}
