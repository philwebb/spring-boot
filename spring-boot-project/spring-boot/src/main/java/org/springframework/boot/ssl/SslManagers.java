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

import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Provides access to key and trust managers and manager factories from
 * {@link SslKeyStores}.
 *
 * @author Scott Frederick
 * @since 3.1.0
 * @see SslBundle#getManagers()
 */
public class SslManagers {

	private final SslKeyStores keyStores;

	private final String keyAlias;

	SslManagers(SslKeyStores keyStores, String keyAlias) {
		this.keyStores = keyStores;
		this.keyAlias = keyAlias;
	}

	/**
	 * Return the {@code KeyManager}s derived from the key store.
	 * @return the key managers
	 */
	public KeyManager[] getKeyManagers() {
		return getKeyManagerFactory().getKeyManagers();
	}

	/**
	 * Return the {@code KeyManagerFactory} derived from the key store.
	 * @return the key manager factory
	 */
	public KeyManagerFactory getKeyManagerFactory() {
		try {
			KeyStore keyStore = this.keyStores.getKeyStore();
			validateKeyAlias(keyStore, this.keyAlias);
			KeyManagerFactory keyManagerFactory = (this.keyAlias == null)
					? KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
					: new ConfigurableAliasKeyManagerFactory(this.keyAlias, KeyManagerFactory.getDefaultAlgorithm());
			String keyPassword = this.keyStores.getKeyPassword();
			if (keyPassword == null) {
				keyPassword = this.keyStores.getKeyStorePassword();
			}
			keyManagerFactory.init(keyStore, (keyPassword != null) ? keyPassword.toCharArray() : null);
			return keyManagerFactory;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not load key manager factory: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Return the {@link TrustManager}s derived from the trust store.
	 * @return the trust managers
	 */
	public TrustManager[] getTrustManagers() {
		return getTrustManagerFactory().getTrustManagers();
	}

	/**
	 * Return the {@link TrustManagerFactory} derived from the trust store.
	 * @return the trust manager factory
	 */
	public TrustManagerFactory getTrustManagerFactory() {
		try {
			KeyStore store = this.keyStores.getTrustStore();
			TrustManagerFactory trustManagerFactory = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(store);
			return trustManagerFactory;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not load trust manager factory: " + ex.getMessage(), ex);
		}
	}

	private void validateKeyAlias(KeyStore keyStore, String keyAlias) {
		if (StringUtils.hasLength(keyAlias) && keyStore != null) {
			try {
				Assert.state(keyStore.containsAlias(keyAlias),
						() -> String.format("Keystore does not contain specified alias '%s'", keyAlias));
			}
			catch (KeyStoreException ex) {
				throw new IllegalStateException(
						String.format("Could not determine if keystore contains alias '%s'", keyAlias), ex);
			}
		}
	}

	/**
	 * A {@link KeyManagerFactory} that allows a configurable key alias to be used. Due to
	 * the fact that the actual calls to retrieve the key by alias are done at request
	 * time the approach is to wrap the actual key managers with a
	 * {@link ConfigurableAliasKeyManager}. The actual SPI has to be wrapped as well due
	 * to the fact that {@link KeyManagerFactory#getKeyManagers()} is final.
	 */
	private static final class ConfigurableAliasKeyManagerFactory extends KeyManagerFactory {

		private ConfigurableAliasKeyManagerFactory(String alias, String algorithm) throws NoSuchAlgorithmException {
			this(KeyManagerFactory.getInstance(algorithm), alias, algorithm);
		}

		private ConfigurableAliasKeyManagerFactory(KeyManagerFactory delegate, String alias, String algorithm) {
			super(new ConfigurableAliasKeyManagerFactorySpi(delegate, alias), delegate.getProvider(), algorithm);
		}

	}

	private static final class ConfigurableAliasKeyManagerFactorySpi extends KeyManagerFactorySpi {

		private final KeyManagerFactory delegate;

		private final String alias;

		private ConfigurableAliasKeyManagerFactorySpi(KeyManagerFactory delegate, String alias) {
			this.delegate = delegate;
			this.alias = alias;
		}

		@Override
		protected void engineInit(KeyStore keyStore, char[] chars)
				throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
			this.delegate.init(keyStore, chars);
		}

		@Override
		protected void engineInit(ManagerFactoryParameters managerFactoryParameters)
				throws InvalidAlgorithmParameterException {
			throw new InvalidAlgorithmParameterException("Unsupported ManagerFactoryParameters");
		}

		@Override
		protected KeyManager[] engineGetKeyManagers() {
			return Arrays.stream(this.delegate.getKeyManagers())
				.filter(X509ExtendedKeyManager.class::isInstance)
				.map(X509ExtendedKeyManager.class::cast)
				.map(this::wrap)
				.toArray(KeyManager[]::new);
		}

		private ConfigurableAliasKeyManager wrap(X509ExtendedKeyManager keyManager) {
			return new ConfigurableAliasKeyManager(keyManager, this.alias);
		}

	}

	private static final class ConfigurableAliasKeyManager extends X509ExtendedKeyManager {

		private final X509ExtendedKeyManager delegate;

		private final String alias;

		private ConfigurableAliasKeyManager(X509ExtendedKeyManager keyManager, String alias) {
			this.delegate = keyManager;
			this.alias = alias;
		}

		@Override
		public String chooseEngineClientAlias(String[] strings, Principal[] principals, SSLEngine sslEngine) {
			return this.delegate.chooseEngineClientAlias(strings, principals, sslEngine);
		}

		@Override
		public String chooseEngineServerAlias(String s, Principal[] principals, SSLEngine sslEngine) {
			return (this.alias != null) ? this.alias : this.delegate.chooseEngineServerAlias(s, principals, sslEngine);
		}

		@Override
		public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
			return this.delegate.chooseClientAlias(keyType, issuers, socket);
		}

		@Override
		public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
			return this.delegate.chooseServerAlias(keyType, issuers, socket);
		}

		@Override
		public X509Certificate[] getCertificateChain(String alias) {
			return this.delegate.getCertificateChain(alias);
		}

		@Override
		public String[] getClientAliases(String keyType, Principal[] issuers) {
			return this.delegate.getClientAliases(keyType, issuers);
		}

		@Override
		public PrivateKey getPrivateKey(String alias) {
			return this.delegate.getPrivateKey(alias);
		}

		@Override
		public String[] getServerAliases(String keyType, Principal[] issuers) {
			return this.delegate.getServerAliases(keyType, issuers);
		}

	}

}
