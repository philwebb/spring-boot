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
import org.apache.pulsar.client.api.ClientBuilder;

/**
 * Adapts a client builder's SSL settings. The Pulsar {@link ClientBuilder} and
 * {@link PulsarAdminBuilder} do not share a common interface for SSL settings eventhough
 * they have the same exact properties. This allows them to be treated the same and
 * therefore the SSL logic can be in a single place.
 *
 * @param <T> the client builder
 * @author Chris Bono
 * @since 3.2.0
 */
public interface ClientBuilderSslSettings<T> {

	/**
	 * Returns the adapted client builder.
	 * @return the adapted client builder
	 */
	T adaptedClientBuilder();

	/**
	 * Configure whether to use TLS encryption on the connection <i>(default: true if
	 * serviceUrl starts with "pulsar+ssl://", false otherwise)</i>.
	 * @param enableTls whether to enable tls
	 * @return the client builder instance
	 */
	T enableTls(boolean enableTls);

	/**
	 * Set the path to the TLS key file.
	 * @param tlsKeyFilePath the path to the tls key file
	 * @return the client builder instance
	 */
	T tlsKeyFilePath(String tlsKeyFilePath);

	/**
	 * Set the path to the TLS certificate file.
	 * @param tlsCertificateFilePath the path to the tls cert file
	 * @return the client builder instance
	 */
	T tlsCertificateFilePath(String tlsCertificateFilePath);

	/**
	 * Set the path to the trusted TLS certificate file.
	 * @param tlsTrustCertsFilePath the path to the tls trust certs file
	 * @return the client builder instance
	 */
	T tlsTrustCertsFilePath(String tlsTrustCertsFilePath);

	/**
	 * Configure whether the Pulsar client accept untrusted TLS certificate from broker
	 * <i>(default: false)</i>.
	 * @param allowTlsInsecureConnection whether to accept a untrusted TLS certificate
	 * @return the client builder instance
	 */
	T allowTlsInsecureConnection(boolean allowTlsInsecureConnection);

	/**
	 * It allows to validate hostname verification when client connects to broker over
	 * tls. It validates incoming x509 certificate and matches provided hostname(CN/SAN)
	 * with expected broker's host name. It follows RFC 2818, 3.1. Server Identity
	 * hostname verification.
	 * @param enableTlsHostnameVerification whether to enable TLS hostname verification
	 * @return the client builder instance
	 * @see <a href="https://tools.ietf.org/html/rfc2818">RFC 818</a>
	 */
	T enableTlsHostnameVerification(boolean enableTlsHostnameVerification);

	/**
	 * If Tls is enabled, whether to use KeyStore type as tls configuration parameter.
	 * False means use default pem type configuration.
	 * @param useKeyStoreTls whether to use key store for tls
	 * @return the client builder instance
	 */
	T useKeyStoreTls(boolean useKeyStoreTls);

	/**
	 * The name of the security provider used for SSL connections. Default value is the
	 * default security provider of the JVM.
	 * @param sslProvider the ssl provider
	 * @return the client builder instance
	 */
	T sslProvider(String sslProvider);

	/**
	 * The file format of the key store file.
	 * @param tlsKeyStoreType the tls key store file format
	 * @return the client builder instance
	 */
	T tlsKeyStoreType(String tlsKeyStoreType);

	/**
	 * The location of the key store file.
	 * @param tlsKeyStorePath the path to the tls key store
	 * @return the client builder instance
	 */
	T tlsKeyStorePath(String tlsKeyStorePath);

	/**
	 * The store password for the key store file.
	 * @param tlsKeyStorePassword the password for the tls key store
	 * @return the client builder instance
	 */
	T tlsKeyStorePassword(String tlsKeyStorePassword);

	/**
	 * The file format of the trust store file.
	 * @param tlsTrustStoreType the tls trust store file format
	 * @return the client builder instance
	 */
	T tlsTrustStoreType(String tlsTrustStoreType);

	/**
	 * The location of the trust store file.
	 * @param tlsTrustStorePath the path to the tls trust store
	 * @return the client builder instance
	 */
	T tlsTrustStorePath(String tlsTrustStorePath);

	/**
	 * The store password for the trust store file.
	 * @param tlsTrustStorePassword the password for the tls trust store
	 * @return the client builder instance
	 */
	T tlsTrustStorePassword(String tlsTrustStorePassword);

	/**
	 * A list of cipher suites. This is a named combination of authentication, encryption,
	 * MAC and key exchange algorithm used to negotiate the security settings for a
	 * network connection using TLS or SSL network protocol. By default, all the available
	 * cipher suites are supported.
	 * @param tlsCiphers the tls ciphers
	 * @return the client builder instance
	 */
	T tlsCiphers(Set<String> tlsCiphers);

	/**
	 * The SSL protocol used to generate the SSLContext. Default setting is TLS, which is
	 * fine for most cases. Allowed values in recent JVMs are TLS, TLSv1.3, TLSv1.2 and
	 * TLSv1.1.
	 * @param tlsProtocols the enabled protocols
	 * @return the client builder instance
	 */
	T tlsProtocols(Set<String> tlsProtocols);

}
