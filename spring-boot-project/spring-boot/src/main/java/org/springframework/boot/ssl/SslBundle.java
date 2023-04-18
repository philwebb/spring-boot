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

package org.springframework.boot.ssl;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

/**
 * A bundle of trust material that can be used for managing SSL connections.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
public interface SslBundle {

	/**
	 * The default protocol to use.
	 */
	String DEFAULT_PROTOCOL = "TLS";

	/**
	 * Return the protocol to use when establishing the connection. Values should be
	 * supported by {@link SSLContext#getInstance(String)} Defaults to
	 * {@value #DEFAULT_PROTOCOL}.
	 * @return the SSL protocol
	 * @see SSLContext#getInstance(String)
	 */
	default String getProtocol() {
		return DEFAULT_PROTOCOL;
	}

	/**
	 * Return a reference the key that should be used for this bundle.
	 * @return a reference to the SSL key that should be used
	 */
	SslKeyReference getKey();

	/**
	 * Return the {@link SslStores} that can be used to access this bundle's key and trust
	 * stores.
	 * @return the {@code SslKeyStores} instance for this bundle
	 */
	SslStores getStores();

	/**
	 * Return the {@link SslManagers} that can be used to access this bundle's
	 * {@link KeyManager key} and {@link TrustManager trust} managers.
	 * @return the {@code SslManagers} instance for this bundle
	 */
	SslManagers getManagers();

	/**
	 * Return {@link SslOptions} that should be applied when establishing the SSL
	 * connection.
	 * @return the options that should be applied
	 */
	SslOptions getOptions();

	/**
	 * Create a new {@link SSLContext} for this bundle.
	 * @return a new {@link SSLContext} instance
	 */
	default SSLContext createSslContext() {
		return getManagers().createSslContext(getProtocol());
	}

}
