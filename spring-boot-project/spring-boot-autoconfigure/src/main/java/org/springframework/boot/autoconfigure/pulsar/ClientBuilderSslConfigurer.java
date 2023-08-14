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

package org.springframework.boot.autoconfigure.pulsar;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.ssl.JksSslBundleProperties;
import org.springframework.boot.autoconfigure.ssl.JksSslBundleProperties.Store;
import org.springframework.boot.autoconfigure.ssl.PemSslBundleProperties;
import org.springframework.boot.autoconfigure.ssl.SslProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

/**
 * Configures the SSL settings on {@link ClientBuilderSslSettings}.
 *
 * @author Chris Bono
 * @since 3.2.0
 */
public class ClientBuilderSslConfigurer {

	private final SslBundles sslBundles;

	private final SslProperties sslProperties;

	/**
	 * Construct a configurer using the specified SSL bundle and properties.
	 * @param sslBundles optional ssl bundles configured for the application
	 * @param sslProperties optional ssl properties for the application
	 */
	public ClientBuilderSslConfigurer(SslBundles sslBundles, SslProperties sslProperties) {
		this.sslBundles = sslBundles;
		this.sslProperties = sslProperties;
	}

	/**
	 * Applies the specified SSL properties to the specified client SSL builder.
	 * @param clientBuilderSslSettings the ssl builder
	 * @param clientSslProperties the ssl properties
	 */
	public void applySsl(ClientBuilderSslSettings<?> clientBuilderSslSettings,
			SslConfigProperties clientSslProperties) {
		if (!clientSslProperties.isEnabled()) {
			return;
		}
		clientBuilderSslSettings.enableTls(true);
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(clientSslProperties::isVerifyHostname).to(clientBuilderSslSettings::enableTlsHostnameVerification);
		map.from(clientSslProperties::getAllowInsecureConnection)
			.to(clientBuilderSslSettings::allowTlsInsecureConnection);
		if (clientSslProperties.getBundle() == null) {
			return;
		}
		Assert.state(this.sslBundles != null, "SSL enabled but no SSL bundles configured");
		Assert.state(this.sslProperties != null, "SSL enabled but no SSL properties configured");
		String bundleName = clientSslProperties.getBundle();
		SslBundle sslBundle = this.sslBundles.getBundle(bundleName);
		SslOptions sslOptions = sslBundle.getOptions();
		if (sslOptions.getCiphers() != null) {
			Set<String> tlsCiphers = Arrays.stream(sslOptions.getCiphers()).collect(Collectors.toSet());
			clientBuilderSslSettings.tlsCiphers(tlsCiphers);
		}
		if (sslOptions.getEnabledProtocols() != null) {
			Set<String> tlsProtocols = Arrays.stream(sslOptions.getEnabledProtocols()).collect(Collectors.toSet());
			clientBuilderSslSettings.tlsProtocols(tlsProtocols);
		}
		SslType sslBundleType = SslType.forSslStoreBundle(sslBundle.getStores());
		switch (sslBundleType) {
			case JKS -> applyJksSslProperties(clientBuilderSslSettings, map, bundleName);
			case PEM -> applyPemSslProperties(clientBuilderSslSettings, map, bundleName);
			case UNSUPPORTED -> throw new IllegalArgumentException(
					"Unsupported store bundle type %s".formatted(sslBundle.getStores().getClass().getName()));
		}
	}

	private void applyJksSslProperties(ClientBuilderSslSettings<?> clientBuilderSslSettings, PropertyMapper map,
			String bundleName) {
		clientBuilderSslSettings.useKeyStoreTls(true);
		// Dip down into the ssl props for the info we need to set on the client builder
		JksSslBundleProperties jksProperties = this.sslProperties.getBundle().getJks().get(bundleName);
		Assert.state(jksProperties != null,
				() -> "SslStoreBundle is JKS but unable to find 'spring.ssl.bundle.jks.%s.*' properties"
					.formatted(bundleName));
		Store trustStore = jksProperties.getTruststore();
		map.from(trustStore::getType).to(clientBuilderSslSettings::tlsTrustStoreType);
		map.from(trustStore::getLocation).as(this::resolvePath).to(clientBuilderSslSettings::tlsTrustStorePath);
		map.from(trustStore::getPassword).to(clientBuilderSslSettings::tlsTrustStorePassword);
		map.from(trustStore::getProvider).to(clientBuilderSslSettings::sslProvider);
		Store keyStore = jksProperties.getKeystore();
		map.from(keyStore::getType).to(clientBuilderSslSettings::tlsKeyStoreType);
		map.from(keyStore::getLocation).as(this::resolvePath).to(clientBuilderSslSettings::tlsKeyStorePath);
		map.from(keyStore::getPassword).to(clientBuilderSslSettings::tlsKeyStorePassword);
		// Key store provider overrides trust store provider
		map.from(keyStore::getProvider).to(clientBuilderSslSettings::sslProvider);
	}

	private void applyPemSslProperties(ClientBuilderSslSettings<?> clientBuilderSslSettings, PropertyMapper map,
			String bundleName) {
		// Dip down into the properties for the info we need to set on the client builder
		PemSslBundleProperties pemProperties = this.sslProperties.getBundle().getPem().get(bundleName);
		Assert.state(pemProperties != null,
				() -> "SslStoreBundle is PEM but unable to find 'spring.ssl.bundle.pem.%s.*' properties"
					.formatted(bundleName));
		PemSslBundleProperties.Store trustStore = pemProperties.getTruststore();
		map.from(trustStore::getCertificate).as(this::resolvePath).to(clientBuilderSslSettings::tlsTrustCertsFilePath);
		PemSslBundleProperties.Store keyStore = pemProperties.getKeystore();
		map.from(keyStore::getPrivateKey).as(this::resolvePath).to(clientBuilderSslSettings::tlsKeyFilePath);
		map.from(keyStore::getCertificate).as(this::resolvePath).to(clientBuilderSslSettings::tlsCertificateFilePath);
	}

	/**
	 * Resolves a location into an actual path. The Pulsar client builders TLS related
	 * methods all expect the locations passed in to be file paths. Adding this resolve
	 * allows us to use 'classpath:' locations.
	 * @param resourceLocation the location of the resource
	 * @return path to the resource
	 */
	private String resolvePath(String resourceLocation) {
		try {
			return ResourceUtils.getURL(resourceLocation).getPath();
		}
		catch (FileNotFoundException ex) {
			throw new RuntimeException(ex);
		}
	}

	enum SslType {

		JKS, PEM, UNSUPPORTED;

		static SslType forSslStoreBundle(SslStoreBundle sslStoreBundle) {
			if (sslStoreBundle instanceof JksSslStoreBundle) {
				return SslType.JKS;
			}
			if (sslStoreBundle instanceof PemSslStoreBundle) {
				return SslType.PEM;
			}
			return UNSUPPORTED;
		}

	}

}
