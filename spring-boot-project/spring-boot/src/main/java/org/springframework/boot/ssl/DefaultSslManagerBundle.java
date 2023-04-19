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

package org.springframework.boot.ssl;

import java.security.KeyStore;
import java.security.KeyStoreException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link SslManagerBundle}.
 *
 * @author Scott Frederick
 * @see SslManagerBundle#from(SslStoreBundle, SslKeyReference)
 */
class DefaultSslManagerBundle implements SslManagerBundle {

	private final SslStoreBundle storeBundle;

	private final SslKeyReference key;

	DefaultSslManagerBundle(SslStoreBundle storeBundle, SslKeyReference key) {
		this.storeBundle = (storeBundle != null) ? storeBundle : SslStoreBundle.NONE;
		this.key = (key != null) ? key : SslKeyReference.NONE;
	}

	@Override
	public KeyManagerFactory getKeyManagerFactory() {
		try {
			KeyStore store = this.storeBundle.getKeyStore();
			String alias = this.key.getAlias();
			validateAlias(store, alias);
			KeyManagerFactory factory = (alias != null)
					? new AliasKeyManagerFactory(alias, KeyManagerFactory.getDefaultAlgorithm())
					: KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			String password = (this.key.getPassword() != null) ? this.key.getPassword()
					: this.storeBundle.getKeyStorePassword();
			factory.init(store, (password != null) ? password.toCharArray() : null);
			return factory;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not load key manager factory: " + ex.getMessage(), ex);
		}
	}

	private void validateAlias(KeyStore store, String alias) {
		if (StringUtils.hasLength(alias) && store != null) {
			try {
				Assert.state(store.containsAlias(alias),
						() -> String.format("Keystore does not contain specified alias '%s'", alias));
			}
			catch (KeyStoreException ex) {
				throw new IllegalStateException(
						String.format("Could not determine if keystore contains alias '%s'", alias), ex);
			}
		}
	}

	@Override
	public TrustManagerFactory getTrustManagerFactory() {
		try {
			KeyStore store = this.storeBundle.getTrustStore();
			TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			factory.init(store);
			return factory;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not load trust manager factory: " + ex.getMessage(), ex);
		}
	}

}
