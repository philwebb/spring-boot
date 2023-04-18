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

package org.springframework.boot.ssl.pem;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.springframework.boot.ssl.SslStores;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;

/**
 * {@link SslStores} backed by PEM-encoded certificates and private keys.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
public class PemSslStores implements SslStores {

	@Override
	public KeyStore getKeyStore() {
		if (this.ssl.getCertificate() == null) {
			return null;
		}
		return createKeyStoreFromResources(this.ssl.getCertificate(), this.ssl.getCertificatePrivateKey(),
				this.ssl.getKeyStoreType(), this.ssl.getKeyAlias());
	}

	@Override
	public String getKeyStorePassword() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public KeyStore getTrustStore() {
		if (this.ssl.getTrustCertificate() == null) {
			return null;
		}
		return createKeyStoreFromResources(this.ssl.getTrustCertificate(), this.ssl.getTrustCertificatePrivateKey(),
				this.ssl.getTrustStoreType(), this.ssl.getKeyAlias());
	}

	/**
	 * Create a new {@link KeyStore} populated with the certificate stored at the
	 * specified file path and an optional private key file.
	 * @param certificatePath the certificate file
	 * @param privateKeyPath the private key file, or {@code null} if no private key is
	 * required
	 * @param storeType the {@code KeyStore} type to create, or {@code null} to create the
	 * system default type
	 * @param keyAlias the alias to use when adding keys to the {@code KeyStore}
	 * @return the {@code KeyStore}
	 */
	protected KeyStore createKeyStoreFromResources(String certificatePath, String privateKeyPath, String storeType,
			String keyAlias) {
		Assert.notNull(certificatePath, "CertificatePath must not be null");
		return createKeyStore(readText(certificatePath), readText(privateKeyPath), storeType, keyAlias);
	}

	/**
	 * Create a new {@link KeyStore} populated with the certificate and optional private
	 * privateKey.
	 * @param certificate the certificate
	 * @param privateKey the private key, or {@code null} if no private key is required
	 * @param storeType the {@code KeyStore} type to create, or {@code null} to create the
	 * system default type
	 * @param keyAlias the alias to use when adding keys to the {@code KeyStore}
	 * @return the {@code KeyStore}
	 */
	protected KeyStore createKeyStore(String certificate, String privateKey, String storeType, String keyAlias) {
		Assert.notNull(certificate, "Certificate must not be null");
		try {
			KeyStore keyStore = KeyStore.getInstance((storeType != null) ? storeType : KeyStore.getDefaultType());
			keyStore.load(null);
			X509Certificate[] certificates = CertificateParser.parse(certificate);
			PrivateKey key = (privateKey != null) ? PrivateKeyParser.parse(privateKey) : null;
			try {
				addCertificates(keyStore, certificates, key, keyAlias);
			}
			catch (KeyStoreException ex) {
				throw new IllegalStateException("Error adding certificates to KeyStore: " + ex.getMessage(), ex);
			}
			return keyStore;
		}
		catch (GeneralSecurityException | IOException ex) {
			throw new IllegalStateException("Error creating KeyStore: " + ex.getMessage(), ex);
		}
	}

	private void addCertificates(KeyStore keyStore, X509Certificate[] certificates, PrivateKey privateKey,
			String keyAlias) throws KeyStoreException {
		String alias = (keyAlias != null) ? keyAlias : DEFAULT_KEY_ALIAS;
		if (privateKey != null) {
			keyStore.setKeyEntry(alias, privateKey, KEY_PASSWORD.toCharArray(), certificates);
		}
		else {
			for (int index = 0; index < certificates.length; index++) {
				keyStore.setCertificateEntry(alias + "-" + index, certificates[index]);
			}
		}
	}

	private String readText(String resource) {
		if (resource == null) {
			return null;
		}
		try {
			URL url = ResourceUtils.getURL(resource);
			try (Reader reader = new InputStreamReader(url.openStream())) {
				return FileCopyUtils.copyToString(reader);
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException(
					"Error reading certificate or key from file '" + resource + "':" + ex.getMessage(), ex);
		}
	}

}
