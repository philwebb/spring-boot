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

import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.springframework.boot.sslx.SslKeyStores;
import org.springframework.boot.sslx.SslManagers;

/**
 * A bundle of trust material that can be used for managing SSL connections.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
public interface SslBundle {

	/**
	 * Return the SSL bundle configuration.
	 * @return the properties
	 */
	SslDetails getDetails();

	/**
	 * Return an {@link SslKeyStores} that can be used to access this bundle's
	 * {@link KeyStore}s.
	 * @return the {@code SslKeyStores}
	 */
	SslKeyStores getKeyStores();

	/**
	 * Return an {@link SslManagers} that can be used to access this bundle's
	 * {@link KeyManager}s and {@link TrustManager}s.
	 * @return the {@code SslManagers}
	 */
	SslManagers getManagers();

	/**
	 * Return the {@link SSLContext}.
	 * @return the SSL context
	 */
	SSLContext getSslContext();

}
