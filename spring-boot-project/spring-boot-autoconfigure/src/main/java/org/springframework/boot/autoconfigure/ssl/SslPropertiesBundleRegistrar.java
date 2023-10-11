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

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.boot.ssl.pem.PemSslStoreDetails.Type;
import org.springframework.util.ResourceUtils;

/**
 * A {@link SslBundleRegistrar} that registers SSL bundles based
 * {@link SslProperties#getBundle() configuration properties}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class SslPropertiesBundleRegistrar implements SslBundleRegistrar {

	private final SslProperties.Bundles properties;

	private final FileWatcher fileWatcher;

	SslPropertiesBundleRegistrar(SslProperties properties, FileWatcher fileWatcher) {
		this.properties = properties.getBundle();
		this.fileWatcher = fileWatcher;
	}

	@Override
	public void registerBundles(SslBundleRegistry registry) {
		registerBundles(registry, this.properties.getPem(), PropertiesSslBundle::get, this::getPathsToWatch);
		registerBundles(registry, this.properties.getJks(), PropertiesSslBundle::get, this::getPathsToWatch);
	}

	private <P extends SslBundleProperties> void registerBundles(SslBundleRegistry registry, Map<String, P> properties,
			Function<P, SslBundle> bundleFactory, Function<P, Set<Path>> watchedPathsSupplier) {
		properties.forEach((bundleName, bundleProperties) -> {
			SslBundle bundle = bundleFactory.apply(bundleProperties);
			registry.registerBundle(bundleName, bundle);
			if (bundleProperties.isReloadOnUpdate()) {
				Set<Path> watchedPaths = watchedPathsSupplier.apply(bundleProperties);
				this.fileWatcher.watch(watchedPaths,
						(changes) -> registry.updateBundle(bundleName, bundleFactory.apply(bundleProperties)));
			}
		});
	}

	private Set<Path> getPathsToWatch(JksSslBundleProperties properties) {
		Set<Path> result = new HashSet<>();
		if (properties.getKeystore().getLocation() != null) {
			result.add(toPath(properties.getKeystore().getLocation()));
		}
		if (properties.getTruststore().getLocation() != null) {
			result.add(toPath(properties.getTruststore().getLocation()));
		}
		return result;
	}

	private Set<Path> getPathsToWatch(PemSslBundleProperties properties) {
		PemSslStoreDetails keystore = properties.getKeystore().asPemSslStoreDetails();
		PemSslStoreDetails truststore = properties.getTruststore().asPemSslStoreDetails();
		Set<Path> result = new HashSet<>();
		if (keystore.privateKey() != null) {
			if (keystore.getPrivateKeyType() != Type.URL) {
				throw new IllegalStateException("Keystore private key is not a URL and can't be watched");
			}
			result.add(toPath(keystore.privateKey()));
		}
		if (keystore.certificate() != null) {
			if (keystore.getCertificateType() != Type.URL) {
				throw new IllegalStateException("Keystore certificate is not a URL and can't be watched");
			}
			result.add(toPath(keystore.certificate()));
		}
		if (truststore.privateKey() != null) {
			if (truststore.getPrivateKeyType() != Type.URL) {
				throw new IllegalStateException("Truststore private key is not a URL and can't be watched");
			}
			result.add(toPath(truststore.privateKey()));
		}
		if (truststore.certificate() != null) {
			if (truststore.getCertificateType() != Type.URL) {
				throw new IllegalStateException("Truststore certificate is not a URL and can't be watched");
			}
			result.add(toPath(truststore.certificate()));
		}
		return result;
	}

	private Path toPath(String location) {
		try {
			URL url = ResourceUtils.getURL(location);
			if (!"file".equals(url.getProtocol())) {
				throw new IllegalStateException("Location '%s' doesn't point to a file".formatted(location));
			}
			return Path.of(url.getFile()).toAbsolutePath();
		}
		catch (FileNotFoundException ex) {
			throw new UncheckedIOException("Failed to get URI to location '%s'".formatted(location), ex);
		}
	}

}
