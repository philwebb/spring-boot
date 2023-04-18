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

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

/**
 * A bundle of trust material that can be used for managing SSL connections.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
public class SslBundle {

	private final SslDetails sslDetails;

	private final SslKeyStores keyStores;

	private final SslManagers managers;

	public SslBundle(SslDetails sslDetails, SslStoreProvider sslStoreProvider) {
		this.sslDetails = sslDetails;
		this.keyStores = new SslKeyStores(sslStoreProvider, sslDetails.getKeyPassword());
		this.managers = new SslManagers(this.keyStores, sslDetails.getKeyAlias());
	}

	/**
	 * Return the SSL bundle configuration.
	 * @return the properties
	 */
	public SslDetails getDetails() {
		return this.sslDetails;
	}

	/**
	 * Return an {@link SslKeyStores} that can be used to access this bundle's
	 * {@link KeyStore}s.
	 * @return the {@code SslKeyStores}
	 */
	public SslKeyStores getKeyStores() {
		return this.keyStores;
	}

	/**
	 * Return an {@link SslManagers} that can be used to access this bundle's
	 * {@link KeyManager}s and {@link TrustManager}s.
	 * @return the {@code SslManagers}
	 */
	public SslManagers getManagers() {
		return this.managers;
	}

	/**
	 * Return the {@link SSLContext}.
	 * @return the SSL context
	 */
	public SSLContext getSslContext() {
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(this.managers.getKeyManagers(), this.managers.getTrustManagers(), null);
			return sslContext;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not load SSL context: " + ex.getMessage(), ex);
		}
	}

}
