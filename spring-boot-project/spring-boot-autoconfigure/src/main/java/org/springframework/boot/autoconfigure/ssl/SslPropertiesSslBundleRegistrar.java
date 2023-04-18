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

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.boot.ssl.certificate.CertificateFileSslDetails;
import org.springframework.boot.ssl.certificate.CertificateFileSslStoreProvider;
import org.springframework.boot.sslx.keystore.JavaKeyStoreSslDetails;
import org.springframework.boot.sslx.keystore.JavaKeyStoreSslStoreProvider;

/**
 * A {@link SslBundleRegistrar} that registers SSL bundles based on configuration
 * properties.
 *
 * @author Scott Frederick
 */
class SslPropertiesSslBundleRegistrar implements SslBundleRegistrar {

	private final SslProperties sslProperties;

	SslPropertiesSslBundleRegistrar(SslProperties sslProperties) {
		this.sslProperties = sslProperties;
	}

	@Override
	public void registerBundles(SslBundleRegistry registry) {
		this.sslProperties.getCertificate()
			.forEach((name, properties) -> registry.registerBundle(name, createCertificateBundle(properties)));
		this.sslProperties.getKeystore()
			.forEach((name, properties) -> registry.registerBundle(name, createJavaKeyStoreBundle(properties)));
	}

	private static SslBundle createCertificateBundle(CertificateFileSslDetails sslDetails) {
		return new SslBundle(sslDetails, CertificateFileSslStoreProvider.from(sslDetails));
	}

	private static SslBundle createJavaKeyStoreBundle(JavaKeyStoreSslDetails sslDetails) {
		return new SslBundle(sslDetails, JavaKeyStoreSslStoreProvider.from(sslDetails));
	}

}
