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

package org.springframework.boot.ssl.keystore;

import org.springframework.boot.ssl.SslDetails;

/**
 * SSL configuration with Java keystore files.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
public class JavaKeyStoreSslDetails extends SslDetails {

	/**
	 * The path to the key store that holds the SSL certificate (typically a jks file).
	 */
	private String keyStore;

	/**
	 * The password used to access the key store.
	 */
	private String keyStorePassword;

	/**
	 * The provider for the key store.
	 */
	private String keyStoreProvider;

	/**
	 * Return the trust store that holds SSL certificates.
	 */
	private String trustStore;

	/**
	 * Return the password used to access the trust store.
	 */
	private String trustStorePassword;

	/**
	 * Return the provider for the trust store.
	 */
	private String trustStoreProvider;

	public String getKeyStore() {
		return this.keyStore;
	}

	public void setKeyStore(String keyStore) {
		this.keyStore = keyStore;
	}

	public String getKeyStorePassword() {
		return this.keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	public String getKeyStoreProvider() {
		return this.keyStoreProvider;
	}

	public void setKeyStoreProvider(String keyStoreProvider) {
		this.keyStoreProvider = keyStoreProvider;
	}

	public String getTrustStore() {
		return this.trustStore;
	}

	public void setTrustStore(String trustStore) {
		this.trustStore = trustStore;
	}

	public String getTrustStorePassword() {
		return this.trustStorePassword;
	}

	public void setTrustStorePassword(String trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

	public String getTrustStoreProvider() {
		return this.trustStoreProvider;
	}

	public void setTrustStoreProvider(String trustStoreProvider) {
		this.trustStoreProvider = trustStoreProvider;
	}

}
