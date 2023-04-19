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

import java.util.Map;
import java.util.function.Function;

import org.springframework.boot.autoconfigure.ssl.SslBundleProperties.Key;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.boot.ssl.SslKeyReference;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;

/**
 * A {@link SslBundleRegistrar} that registers SSL bundles based
 * {@link SslProperties#getBundle() configuration properties}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
class PropertiesSslBundleRegistrar implements SslBundleRegistrar {

	private final SslProperties.Bundles properties;

	PropertiesSslBundleRegistrar(SslProperties properties) {
		this.properties = properties.getBundle();
	}

	@Override
	public void registerBundles(SslBundleRegistry registry) {
		registerBundles(registry, this.properties.getPem(), this::asSslStoreBundle);
		registerBundles(registry, this.properties.getJks(), this::asSslStoreBundle);
	}

	private <P extends SslBundleProperties> void registerBundles(SslBundleRegistry registry, Map<String, P> properties,
			Function<P, SslStoreBundle> storeBundleAdapter) {
		properties.forEach((bundleName, bundleProperties) -> {
			SslStoreBundle storeBundle = storeBundleAdapter.apply(bundleProperties);
			SslBundle bundle = new PropertiesSslBundle(bundleProperties, storeBundle);
			registry.registerBundle(bundleName, bundle);
		});
	}

	private SslStoreBundle asSslStoreBundle(PemSslBundleProperties properties) {
		PemSslStoreDetails keyStoreDetails = asStoreDetails(properties.getKeystore());
		PemSslStoreDetails trustStoreDetails = asStoreDetails(properties.getTruststore());
		return new PemSslStoreBundle(keyStoreDetails, trustStoreDetails, properties.getKey().getAlias());
	}

	private PemSslStoreDetails asStoreDetails(PemSslBundleProperties.Store properties) {
		return new PemSslStoreDetails(properties.getType(), properties.getCertificate(), properties.getPrivateKey());
	}

	private SslStoreBundle asSslStoreBundle(JksSslBundleProperties properties) {
		JksSslStoreDetails keyStoreDetails = asStoreDetails(properties.getKeystore());
		JksSslStoreDetails trustStoreDetails = asStoreDetails(properties.getTruststore());
		return new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
	}

	private JksSslStoreDetails asStoreDetails(JksSslBundleProperties.Store properties) {
		return new JksSslStoreDetails(properties.getType(), properties.getProvider(), properties.getLocation(),
				properties.getPassword());
	}

	/**
	 * {@link SslBundle} backed by {@link SslBundleProperties}.
	 */
	static class PropertiesSslBundle implements SslBundle {

		private final String protocol;

		private final SslKeyReference key;

		private final SslStoreBundle stores;

		private final SslManagerBundle managers;

		private final SslOptions options;

		PropertiesSslBundle(SslBundleProperties properties, SslStoreBundle stores) {
			this.protocol = properties.getProtocol();
			this.key = asSslKeyReference(properties.getKey());
			this.stores = null;
			this.managers = SslManagerBundle.from(this.stores, this.key);
			this.options = asSslOptions(properties.getOptions());
		}

		private static SslKeyReference asSslKeyReference(Key key) {
			return (key != null) ? SslKeyReference.of(key.getAlias(), key.getPassword()) : SslKeyReference.NONE;
		}

		private static SslOptions asSslOptions(SslBundleProperties.Options properties) {
			return (properties != null) ? SslOptions.of(properties.getCiphers(), properties.getEnabledProtocols())
					: SslOptions.NONE;
		}

		@Override
		public String getProtocol() {
			return this.protocol;
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
