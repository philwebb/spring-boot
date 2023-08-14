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

import org.apache.pulsar.client.api.ClientBuilder;

/**
 * Adapts a {@link ClientBuilder} SSL settings.
 *
 * @author Chris Bono
 */
class PulsarClientBuilderSslSettings implements ClientBuilderSslSettings<ClientBuilder> {

	private final ClientBuilder clientBuilder;

	/**
	 * Constructs an instance that adapts the specified Pulsar client builder.
	 * @param clientBuilder the Pulsar client builder to adapt
	 */
	PulsarClientBuilderSslSettings(ClientBuilder clientBuilder) {
		this.clientBuilder = clientBuilder;
	}

	@Override
	public ClientBuilder adaptedClientBuilder() {
		return this.clientBuilder;
	}

	@SuppressWarnings("deprecation")
	@Override
	public ClientBuilder enableTls(boolean enableTls) {
		return this.clientBuilder.enableTls(enableTls);
	}

	@Override
	public ClientBuilder tlsKeyFilePath(String tlsKeyFilePath) {
		return this.clientBuilder.tlsKeyFilePath(tlsKeyFilePath);
	}

	@Override
	public ClientBuilder tlsCertificateFilePath(String tlsCertificateFilePath) {
		return this.clientBuilder.tlsCertificateFilePath(tlsCertificateFilePath);
	}

	@Override
	public ClientBuilder tlsTrustCertsFilePath(String tlsTrustCertsFilePath) {
		return this.clientBuilder.tlsTrustCertsFilePath(tlsTrustCertsFilePath);
	}

	@Override
	public ClientBuilder allowTlsInsecureConnection(boolean allowTlsInsecureConnection) {
		return this.clientBuilder.allowTlsInsecureConnection(allowTlsInsecureConnection);
	}

	@Override
	public ClientBuilder enableTlsHostnameVerification(boolean enableTlsHostnameVerification) {
		return this.clientBuilder.enableTlsHostnameVerification(enableTlsHostnameVerification);
	}

	@Override
	public ClientBuilder useKeyStoreTls(boolean useKeyStoreTls) {
		return this.clientBuilder.useKeyStoreTls(useKeyStoreTls);
	}

	@Override
	public ClientBuilder sslProvider(String sslProvider) {
		return this.clientBuilder.sslProvider(sslProvider);
	}

	@Override
	public ClientBuilder tlsKeyStoreType(String tlsKeyStoreType) {
		return this.clientBuilder.tlsKeyStoreType(tlsKeyStoreType);
	}

	@Override
	public ClientBuilder tlsKeyStorePath(String tlsKeyStorePath) {
		return this.clientBuilder.tlsKeyStorePath(tlsKeyStorePath);
	}

	@Override
	public ClientBuilder tlsKeyStorePassword(String tlsKeyStorePassword) {
		return this.clientBuilder.tlsKeyStorePassword(tlsKeyStorePassword);
	}

	@Override
	public ClientBuilder tlsTrustStoreType(String tlsTrustStoreType) {
		return this.clientBuilder.tlsTrustStoreType(tlsTrustStoreType);
	}

	@Override
	public ClientBuilder tlsTrustStorePath(String tlsTrustStorePath) {
		return this.clientBuilder.tlsTrustStorePath(tlsTrustStorePath);
	}

	@Override
	public ClientBuilder tlsTrustStorePassword(String tlsTrustStorePassword) {
		return this.clientBuilder.tlsTrustStorePassword(tlsTrustStorePassword);
	}

	@Override
	public ClientBuilder tlsCiphers(Set<String> tlsCiphers) {
		return this.clientBuilder.tlsCiphers(tlsCiphers);
	}

	@Override
	public ClientBuilder tlsProtocols(Set<String> tlsProtocols) {
		return this.clientBuilder.tlsProtocols(tlsProtocols);
	}

}
