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

package org.springframework.boot.ssl.jks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@link SslStoreBundle} backed by a Java keystore.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 3.1.0
 */
public class JksSslStoreBundle implements SslStoreBundle {

	private final StoreDetails keyStoreDetails;

	private final StoreDetails trustStoreDetails;

	/**
	 * Create a new {@link JksSslStoreBundle} instance.
	 * @param keyStoreDetails the key store details
	 * @param trustStoreDetails the trust store details
	 */
	public JksSslStoreBundle(StoreDetails keyStoreDetails, StoreDetails trustStoreDetails) {
		this.keyStoreDetails = keyStoreDetails;
		this.trustStoreDetails = trustStoreDetails;
	}

	@Override
	public KeyStore getKeyStore() {
		return createKeyStore("key", this.keyStoreDetails);
	}

	@Override
	public String getKeyStorePassword() {
		return this.keyStoreDetails.password();
	}

	@Override
	public KeyStore getTrustStore() {
		return createKeyStore("trust", this.trustStoreDetails);
	}

	private KeyStore createKeyStore(String name, StoreDetails details) {
		if (details == null || details.isEmpty()) {
			return null;
		}
		try {
			String type = (!StringUtils.hasText(details.type())) ? details.type() : KeyStore.getDefaultType();
			char[] password = (details.password() != null) ? details.password().toCharArray() : null;
			String location = details.location();
			KeyStore store = getKeyStoreInstance(type, details.provider());
			if (isHardwareKeystoreType(type)) {
				loadHardwareKeyStore(store, location, password);
			}
			else {
				loadKeyStore(store, location, password);
			}
			return store;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to create %s store: %s".formatted(name, ex.getMessage()), ex);
		}
	}

	private KeyStore getKeyStoreInstance(String type, String provider)
			throws KeyStoreException, NoSuchProviderException {
		return (!StringUtils.hasText(type)) ? KeyStore.getInstance(type, provider) : KeyStore.getInstance(type);
	}

	private boolean isHardwareKeystoreType(String type) {
		return type.equalsIgnoreCase("PKCS11");
	}

	private void loadHardwareKeyStore(KeyStore store, String location, char[] password)
			throws IOException, NoSuchAlgorithmException, CertificateException {
		Assert.state(!StringUtils.hasText(location),
				() -> "Location is '%s', but must be empty or null for PKCS11 hardware key stores".formatted(location));
		store.load(null, password);
	}

	private void loadKeyStore(KeyStore store, String location, char[] password) {
		Assert.state(StringUtils.hasText(location), () -> "Location must not be empty or null");
		try {
			URL url = ResourceUtils.getURL(location);
			try (InputStream stream = url.openStream()) {
				store.load(stream, password);
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not load store from '" + location + "'", ex);
		}
	}

	/**
	 * Details for an individual trust or key store.
	 *
	 * @param type the key store type, for example {@code JKS} or {@code PKCS11}. A
	 * {@code null} value will use {@link KeyStore#getDefaultType()}).
	 * @param provider the name of the key store provider
	 * @param location the location of the key store file or {@code null} if using a
	 * {@code PKCS11} hardware store
	 * @param password the password used to unlock the store or {@code null}
	 */
	public static record StoreDetails(String type, String provider, String location, String password) {

		public StoreDetails(String location) {
			this(null, null, location, null);
		}

		boolean isEmpty() {
			return isEmpty(this.type) && isEmpty(this.provider) && isEmpty(this.location);
		}

		private boolean isEmpty(String value) {
			return !StringUtils.hasText(value);
		}

	}

}
