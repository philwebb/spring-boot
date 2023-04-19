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

import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslKeyReference;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.function.ThrowingSupplier;

/**
 * {@link SslBundle} backed by {@link Ssl} or an {@link SslStoreProvider}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
public class WebServerSslBundle implements SslBundle {

	private final SslKeyReference key;

	private final SslStoreBundle stores;

	private final SslManagerBundle managers;

	private final SslOptions options;

	private WebServerSslBundle(String keyPassword, Ssl ssl, SslStoreBundle stores) {
		this.key = SslKeyReference.of(ssl.getKeyAlias(), keyPassword);
		this.stores = stores;
		this.managers = SslManagerBundle.from(this.stores, this.key);
		this.options = SslOptions.of(ssl.getCiphers(), ssl.getEnabledProtocols());
	}

	private static SslStoreBundle createPemStoreBundle(Ssl ssl) {
		PemSslStoreBundle.StoreDetails keyStoreDetails = new PemSslStoreBundle.StoreDetails(ssl.getKeyStoreType(),
				ssl.getCertificate(), ssl.getCertificatePrivateKey());
		PemSslStoreBundle.StoreDetails trustStoreDetails = new PemSslStoreBundle.StoreDetails(ssl.getTrustStoreType(),
				ssl.getTrustCertificate(), ssl.getTrustCertificatePrivateKey());
		return new PemSslStoreBundle(ssl.getKeyAlias(), keyStoreDetails, trustStoreDetails);
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
	 * Get the {@link SslBundle} that should be used for the given {@link Ssl} instance.
	 * @param sslBundles the bundles that should be used when {@link Ssl#getBundle()} is
	 * set
	 * @param ssl the source ssl instance
	 * @return a {@link SslBundle} instance
	 * @throws NoSuchSslBundleException if a bundle lookup fails
	 */
	public static SslBundle get(Ssl ssl) throws NoSuchSslBundleException {
		return get(ssl, null, null);
	}

	/**
	 * Get the {@link SslBundle} that should be used for the given {@link Ssl} instance.
	 * @param ssl the source ssl instance
	 * @param sslBundles the bundles that should be used when {@link Ssl#getBundle()} is
	 * set
	 * @return a {@link SslBundle} instance
	 * @throws NoSuchSslBundleException if a bundle lookup fails
	 */
	public static SslBundle get(Ssl ssl, SslBundles sslBundles) throws NoSuchSslBundleException {
		return get(ssl, sslBundles, null);
	}

	/**
	 * Get the {@link SslBundle} that should be used for the given {@link Ssl} and
	 * {@link SslStoreProvider} instances.
	 * @param ssl the source {@link Ssl} instance
	 * @param sslBundles the bundles that should be used when {@link Ssl#getBundle()} is
	 * set
	 * @param sslStoreProvider the {@link SslStoreProvider} to use or {@code null}
	 * @return a {@link SslBundle} instance
	 * @throws NoSuchSslBundleException if a bundle lookup fails
	 * @deprecated since 3.1.0 for removal in 3.3.0 along with {@link SslStoreProvider}
	 */
	@Deprecated
	@SuppressWarnings("deprecation")
	public static SslBundle get(Ssl ssl, SslBundles sslBundles, SslStoreProvider sslStoreProvider) {
		Assert.state(Ssl.isEnabled(ssl), "SSL is not enabled");
		String keyPassword = (sslStoreProvider != null) ? sslStoreProvider.getKeyPassword() : ssl.getKeyPassword();
		if (sslStoreProvider != null) {
			SslStoreBundle stores = new SslStoreProviderBundleAdapter(sslStoreProvider);
			return new WebServerSslBundle(keyPassword, ssl, stores);
		}
		String bundleName = ssl.getBundle();
		if (StringUtils.hasText(bundleName)) {
			Assert.state(sslBundles != null,
					() -> "SSL bundle '%s' was requested but no SslBundles instance was provided"
						.formatted(bundleName));
			return sslBundles.getBundle(bundleName);
		}
		SslStoreBundle stores = createStoreBundle(ssl);
		return new WebServerSslBundle(keyPassword, ssl, stores);
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

	static SslBundle createCertificateFileSslStoreProviderDelegate(Ssl ssl) {
		if (!hasCertificateProperties(ssl)) {
			return null;
		}
		SslStoreBundle stores = createPemStoreBundle(ssl);
		return new WebServerSslBundle(ssl.getKeyPassword(), ssl, stores);
	}

	private static boolean hasCertificateProperties(Ssl ssl) {
		return Ssl.isEnabled(ssl) && ssl.getCertificate() != null && ssl.getCertificatePrivateKey() != null;
	}

	private static boolean hasJavaKeyStoreProperties(Ssl ssl) {
		return Ssl.isEnabled(ssl) && ssl.getKeyStore() != null
				|| (ssl.getKeyStoreType() != null && ssl.getKeyStoreType().equals("PKCS11"));
	}

	/**
	 * Class to adapt a {@link SslStoreProvider} into a {@link SslStoreBundle}.
	 */
	@SuppressWarnings("deprecation")
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
