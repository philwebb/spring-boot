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

package org.springframework.boot.autoconfigure.ssl;

import org.springframework.boot.autoconfigure.ssl.SslBundleProperties.Key;
import org.springframework.boot.autoconfigure.ssl.SslProperties.Bundles;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.boot.ssl.SslKeyReference;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;

/**
 * A {@link SslBundleRegistrar} that registers SSL bundles based on configuration
 * properties.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
class SslPropertiesBundlesRegistrar implements SslBundleRegistrar {

	private final Bundles properties;

	SslPropertiesBundlesRegistrar(SslProperties.Bundles properties) {
		this.properties = properties;
	}

	@Override
	public void registerBundles(SslBundleRegistry registry) {
		this.properties.getPem()
			.forEach((bundleName, bundleProperties) -> registry.registerBundle(bundleName, asBundle(bundleProperties)));
		this.properties.getJks()
			.forEach((bundleName, bundleProperties) -> registry.registerBundle(bundleName, asBundle(bundleProperties)));
	}

	private SslBundle asBundle(PemSslBundleProperties properties) {
		return new PropertiesSslBundle(properties, asSslStoreBundle(properties));
	}

	private PemSslStoreBundle asSslStoreBundle(PemSslBundleProperties properties) {
		PemSslStoreBundle.StoreDetails keyStoreDetails = asStoreDetails(properties.getKeystore());
		PemSslStoreBundle.StoreDetails trustStoreDetails = asStoreDetails(properties.getTruststore());
		return new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
	}

	private PemSslStoreBundle.StoreDetails asStoreDetails(PemSslBundleProperties.Store properties) {
		return new PemSslStoreBundle.StoreDetails(properties.getType(), properties.getCertificate(),
				properties.getPrivateKey());
	}

	private SslBundle asBundle(JksSslBundleProperties properties) {
		return new PropertiesSslBundle(properties, asSslStoreBundle(properties));
	}

	private JksSslStoreBundle asSslStoreBundle(JksSslBundleProperties properties) {
		JksSslStoreBundle.StoreDetails keyStoreDetails = asStoreDetails(properties.getKeystore());
		JksSslStoreBundle.StoreDetails trustStoreDetails = asStoreDetails(properties.getTruststore());
		return new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
	}

	private JksSslStoreBundle.StoreDetails asStoreDetails(JksSslBundleProperties.Store properties) {
		return new JksSslStoreBundle.StoreDetails(properties.getType(), properties.getProvider(),
				properties.getContent(), properties.getPassword());
	}

	/**
	 * {@link SslBundle} backed by {@link SslBundleProperties}.
	 */
	static class PropertiesSslBundle implements SslBundle {

		private final SslKeyReference key;

		private final SslStoreBundle stores;

		private final SslManagerBundle managers;

		private final SslOptions options;

		PropertiesSslBundle(SslBundleProperties properties, SslStoreBundle stores) {
			this.key = asSslKeyReference(properties.getKey());
			this.stores = null;
			this.managers = SslManagerBundle.from(this.stores, this.key);
			this.options = asSslOptions(properties.getOptions());
		}

		private static SslKeyReference asSslKeyReference(Key key) {
			return (key != null) ? SslKeyReference.of(key.getAlias(), key.getPassword()) : SslKeyReference.NONE;
		}

		private static SslOptions asSslOptions(SslBundleProperties.Options properties) {
			return (properties != null) ? SslOptions.of(properties.getEnabledProtocols(), properties.getCiphers())
					: SslOptions.NONE;
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

	}

}
