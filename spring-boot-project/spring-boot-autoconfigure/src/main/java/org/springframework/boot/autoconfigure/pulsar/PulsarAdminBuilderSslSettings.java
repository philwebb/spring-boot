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

import java.util.Set;

import org.apache.pulsar.client.admin.PulsarAdminBuilder;

/**
 * Adapts SSL settings for the {@link PulsarAdminBuilder}.
 *
 * @author Chris Bono
 */
class PulsarAdminBuilderSslSettings implements ClientBuilderSslSettings<PulsarAdminBuilder> {

	private final PulsarAdminBuilder adminBuilder;

	/**
	 * Constructs an instance that adapts the specified Pulsar admin builder.
	 * @param adminBuilder the Pulsar admin builder to adapt
	 */
	PulsarAdminBuilderSslSettings(PulsarAdminBuilder adminBuilder) {
		this.adminBuilder = adminBuilder;
	}

	@Override
	public PulsarAdminBuilder adaptedClientBuilder() {
		return this.adminBuilder;
	}

	@Override
	public PulsarAdminBuilder enableTls(boolean enableTls) {
		return this.adminBuilder;
	}

	@Override
	public PulsarAdminBuilder tlsKeyFilePath(String tlsKeyFilePath) {
		return this.adminBuilder.tlsKeyFilePath(tlsKeyFilePath);
	}

	@Override
	public PulsarAdminBuilder tlsCertificateFilePath(String tlsCertificateFilePath) {
		return this.adminBuilder.tlsCertificateFilePath(tlsCertificateFilePath);
	}

	@Override
	public PulsarAdminBuilder tlsTrustCertsFilePath(String tlsTrustCertsFilePath) {
		return this.adminBuilder.tlsTrustCertsFilePath(tlsTrustCertsFilePath);
	}

	@Override
	public PulsarAdminBuilder allowTlsInsecureConnection(boolean allowTlsInsecureConnection) {
		return this.adminBuilder.allowTlsInsecureConnection(allowTlsInsecureConnection);
	}

	@Override
	public PulsarAdminBuilder enableTlsHostnameVerification(boolean enableTlsHostnameVerification) {
		return this.adminBuilder.enableTlsHostnameVerification(enableTlsHostnameVerification);
	}

	@Override
	public PulsarAdminBuilder useKeyStoreTls(boolean useKeyStoreTls) {
		return this.adminBuilder.useKeyStoreTls(useKeyStoreTls);
	}

	@Override
	public PulsarAdminBuilder sslProvider(String sslProvider) {
		return this.adminBuilder.sslProvider(sslProvider);
	}

	@Override
	public PulsarAdminBuilder tlsKeyStoreType(String tlsKeyStoreType) {
		return this.adminBuilder.tlsKeyStoreType(tlsKeyStoreType);
	}

	@Override
	public PulsarAdminBuilder tlsKeyStorePath(String tlsKeyStorePath) {
		return this.adminBuilder.tlsKeyStorePath(tlsKeyStorePath);
	}

	@Override
	public PulsarAdminBuilder tlsKeyStorePassword(String tlsKeyStorePassword) {
		return this.adminBuilder.tlsKeyStorePassword(tlsKeyStorePassword);
	}

	@Override
	public PulsarAdminBuilder tlsTrustStoreType(String tlsTrustStoreType) {
		return this.adminBuilder.tlsTrustStoreType(tlsTrustStoreType);
	}

	@Override
	public PulsarAdminBuilder tlsTrustStorePath(String tlsTrustStorePath) {
		return this.adminBuilder.tlsTrustStorePath(tlsTrustStorePath);
	}

	@Override
	public PulsarAdminBuilder tlsTrustStorePassword(String tlsTrustStorePassword) {
		return this.adminBuilder.tlsTrustStorePassword(tlsTrustStorePassword);
	}

	@Override
	public PulsarAdminBuilder tlsCiphers(Set<String> tlsCiphers) {
		return this.adminBuilder.tlsCiphers(tlsCiphers);
	}

	@Override
	public PulsarAdminBuilder tlsProtocols(Set<String> tlsProtocols) {
		return this.adminBuilder.tlsProtocols(tlsProtocols);
	}

}
