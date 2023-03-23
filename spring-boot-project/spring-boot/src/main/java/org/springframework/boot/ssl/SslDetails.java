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

/**
 * Centralized SSL trust material configuration.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
public class SslDetails {

	/**
	 * Supported SSL ciphers.
	 */
	private String[] ciphers;

	/**
	 * Enabled SSL protocols.
	 */
	private String[] enabledProtocols;

	/**
	 * The alias that identifies the key in the key store.
	 */
	private String keyAlias;

	/**
	 * The password used to access the key in the key store.
	 */
	private String keyPassword;

	/**
	 * The type of the key store to create.
	 */
	private String keyStoreType;

	/**
	 * The type of the trust store to create.
	 */
	private String trustStoreType;

	/**
	 * The SSL protocol to use. Defaults to "TLS".
	 */
	private String protocol = "TLS";

	public String[] getCiphers() {
		return this.ciphers;
	}

	public void setCiphers(String[] ciphers) {
		this.ciphers = ciphers;
	}

	public String[] getEnabledProtocols() {
		return this.enabledProtocols;
	}

	public void setEnabledProtocols(String[] enabledProtocols) {
		this.enabledProtocols = enabledProtocols;
	}

	public String getKeyAlias() {
		return this.keyAlias;
	}

	public void setKeyAlias(String keyAlias) {
		this.keyAlias = keyAlias;
	}

	public String getKeyPassword() {
		return this.keyPassword;
	}

	public void setKeyPassword(String keyPassword) {
		this.keyPassword = keyPassword;
	}

	public String getKeyStoreType() {
		return this.keyStoreType;
	}

	public void setKeyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
	}

	public String getTrustStoreType() {
		return this.trustStoreType;
	}

	public void setTrustStoreType(String trustStoreType) {
		this.trustStoreType = trustStoreType;
	}

	public String getProtocol() {
		return this.protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

}
