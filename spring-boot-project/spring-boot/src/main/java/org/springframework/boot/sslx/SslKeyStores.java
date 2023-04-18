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

package org.springframework.boot.sslx;

import java.security.KeyStore;

/**
 * Generates {@link KeyStore}s from a source of trust material.
 *
 * @author Scott Frederick
 * @since 3.1.0
 * @see SslBundle#getKeyStores()
 */
public class SslKeyStores {

	private final SslStoreProvider sslStoreProvider;

	private final String keyPassword;

	/**
	 * Create a new {@code SslKeyStores} from the provided properties and trust material.
	 * @param sslStoreProvider the provider of trust material
	 * @param keyPassword the password for the keys in the key stores
	 */
	SslKeyStores(SslStoreProvider sslStoreProvider, String keyPassword) {
		this.keyPassword = keyPassword;
		this.sslStoreProvider = sslStoreProvider;
	}

	/**
	 * Return a key store generated from the provided trust material.
	 * @return the key store
	 */
	public KeyStore getKeyStore() {
		try {
			return this.sslStoreProvider.getKeyStore();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not load key store: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Return a trust store generated from the provided trust material.
	 * @return the trust store
	 */
	public KeyStore getTrustStore() {
		try {
			return this.sslStoreProvider.getTrustStore();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not load trust store: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Return the password for the key in the key store.
	 * @return the key password
	 */
	public String getKeyPassword() {
		if (this.sslStoreProvider.getKeyPassword() != null) {
			return this.sslStoreProvider.getKeyPassword();
		}
		return this.keyPassword;
	}

	/**
	 * Return the password for the key store.
	 * @return the key store password
	 */
	public String getKeyStorePassword() {
		return this.sslStoreProvider.getKeyStorePassword();
	}

}
