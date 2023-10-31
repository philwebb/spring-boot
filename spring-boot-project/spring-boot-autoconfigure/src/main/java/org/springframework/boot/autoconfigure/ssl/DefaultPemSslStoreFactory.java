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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.springframework.boot.ssl.pem.PemSslStore;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.util.Assert;
import org.springframework.util.function.ThrowingFunction;

/**
 * Default {@link PemSslStoreFactory} used to create {@link PemSslStore} instances from
 * {@link PemSslBundleProperties.Store store properties} taking into consideration
 * {@link CertificateFileSelector certificate file selectors} and
 * {@link PrivateKeyFileSelector private key file selectors} for directory glob patterns.
 *
 * @author Phillip Webb
 */
class DefaultPemSslStoreFactory implements PemSslStoreFactory {

	private final List<CertificateFileSelector> certificateFileSelectors;

	private final List<PrivateKeyFileSelector> privateKeyFileSelectors;

	/**
	 * Create a new {@link DefaultPemSslStoreFactory} instance without any custom
	 * selectors.
	 */
	DefaultPemSslStoreFactory() {
		this(Collections.emptyList(), Collections.emptyList());
	}

	/**
	 * Create a new {@link DefaultPemSslStoreFactory} instance.
	 * @param certificateFileSelectors the certificate file selectors to use
	 * @param privateKeyFileSelectors the private key file selectors to use
	 */
	DefaultPemSslStoreFactory(List<CertificateFileSelector> certificateFileSelectors,
			List<PrivateKeyFileSelector> privateKeyFileSelectors) {
		this.certificateFileSelectors = certificateFileSelectors;
		this.privateKeyFileSelectors = privateKeyFileSelectors;
	}

	/**
	 * Create a new {@link PemSslStore} instance based on the given properties.
	 * @param bundleName the bundle name being created
	 * @param storePropertyName the property name prefix
	 * @param storeProperties the properties to use
	 * @return a new {@link PemSslStore} instance
	 */
	@Override
	public PemSslStore getPemSslStore(String bundleName, String storePropertyName,
			PemSslBundleProperties.Store storeProperties) {
		try {
			return createPemSslStore(bundleName, storeProperties,
					new BundleContentProperty(storePropertyName + ".certificate", storeProperties.getCertificate()),
					new BundleContentProperty(storePropertyName + ".private-key", storeProperties.getPrivateKey()));
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private PemSslStore createPemSslStore(String bundleName, PemSslBundleProperties.Store properties,
			BundleContentProperty certificateProperty, BundleContentProperty privateKeyProperty) throws IOException {
		if (!certificateProperty.isDirectoryGlob()) {
			privateKeyProperty.assertIsNotDirectoryGlob();
			PemSslStoreDetails details = new PemSslStoreDetails(properties.getType(), properties.getCertificate(),
					properties.getPrivateKey(), properties.getPrivateKeyPassword());
			return PemSslStore.load(details);
		}
		CertificateFile selectedCertificateFile = selectCertificateFile(bundleName, properties, certificateProperty);
		PrivateKeyFile selectedPrivateKeyFile = selectPrivateKeyFile(bundleName, properties, privateKeyProperty,
				selectedCertificateFile);
		return PemSslStore.of(properties.getType(), selectedCertificateFile.certificates(),
				(selectedPrivateKeyFile != null) ? selectedPrivateKeyFile.privateKey() : null);
	}

	private CertificateFile selectCertificateFile(String bundleName, PemSslBundleProperties.Store properties,
			BundleContentProperty certificateProperty) throws IOException {
		CertificateFile selected = select(certificateProperty, CertificateFile::loadFromPemFile,
				this.certificateFileSelectors, properties.getSelect().getCertificate().getSelector(),
				(selector, candidates) -> selector.selectCertificateFile(bundleName, candidates),
				(candidates) -> "No certificate file selected from the following candidates: %s".formatted(candidates));
		return selected;
	}

	private PrivateKeyFile selectPrivateKeyFile(String bundleName, PemSslBundleProperties.Store properties,
			BundleContentProperty privateKeyProperty, CertificateFile selectedCertificateFile) throws IOException {
		if (!privateKeyProperty.hasValue()) {
			return null;
		}
		PrivateKeyFile selected = select(privateKeyProperty,
				(path) -> PrivateKeyFile.loadFromPemFile(path, properties.getPrivateKeyPassword()),
				this.privateKeyFileSelectors, properties.getSelect().getPrivateKey().getSelector(),
				(selector, candidates) -> selector.selectPrivateKeyFile(bundleName, selectedCertificateFile,
						candidates),
				(candidates) -> "No private key file selected for certificate file %s from the following candidates: %s"
					.formatted(selectedCertificateFile, candidates));
		return selected;
	}

	private <F, S> F select(BundleContentProperty bundleContentProperty, ThrowingFunction<Path, F> fileFactory,
			List<S> selectors, S defaultSelector, BiFunction<S, List<F>, F> selectAction,
			Function<List<F>, String> nonSelectedMessage) throws IOException {
		List<F> candidates = getFiles(bundleContentProperty, fileFactory);
		for (S selector : selectors) {
			F selected = selectAction.apply(selector, candidates);
			if (selected != null) {
				return selected;
			}
		}
		F selected = selectAction.apply(defaultSelector, candidates);
		Assert.notNull(selected, () -> nonSelectedMessage.apply(candidates));
		return selected;
	}

	private <F> List<F> getFiles(BundleContentProperty bundleContentProperty, ThrowingFunction<Path, F> fileFactory)
			throws IOException {
		try (DirectoryStream<Path> paths = bundleContentProperty.getDirectoryGlobMatches()) {
			return StreamSupport.stream(paths.spliterator(), false)
				.filter(Files::isRegularFile)
				.map(fileFactory)
				.toList();
		}
	}

}
