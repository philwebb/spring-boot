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

/**
 * SSL configuration properties for the Pulsar client and admin client.
 *
 * @author Chris Bono
 * @since 3.2.0
 */
public class SslConfigProperties {

	/**
	 * Whether to enable SSL support. Enabled automatically if "bundle" is provided unless
	 * specified otherwise.
	 */
	private Boolean enabled;

	/**
	 * SSL bundle name.
	 */
	private String bundle;

	/**
	 * Whether the hostname is validated when the proxy creates a TLS connection with
	 * brokers.
	 */
	private boolean verifyHostname = false;

	/**
	 * Whether the client accepts untrusted TLS certificates from the broker.
	 */
	private Boolean allowInsecureConnection = false;

	public boolean isEnabled() {
		return (this.enabled != null) ? this.enabled : this.bundle != null;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getBundle() {
		return this.bundle;
	}

	public void setBundle(String bundle) {
		this.bundle = bundle;
	}

	public boolean isVerifyHostname() {
		return this.verifyHostname;
	}

	public void setVerifyHostname(boolean verifyHostname) {
		this.verifyHostname = verifyHostname;
	}

	public Boolean getAllowInsecureConnection() {
		return this.allowInsecureConnection;
	}

	public void setAllowInsecureConnection(Boolean allowInsecureConnection) {
		this.allowInsecureConnection = allowInsecureConnection;
	}

}
