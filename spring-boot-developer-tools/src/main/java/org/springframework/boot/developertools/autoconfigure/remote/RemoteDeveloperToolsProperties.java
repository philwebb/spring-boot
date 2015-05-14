/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.developertools.autoconfigure.remote;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Phillip Webb
 * @author Rob Winch
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.developertools.remote")
public class RemoteDeveloperToolsProperties {

	public static final String DEFAULT_CONTEXT_PATH = "/~~~springboot~~~";

	/**
	 * The default value for {@link #getSecretHeaderName()}
	 */
	public static final String DEFAULT_SECRET_HEADER_NAME = "X-AUTH-TOKEN";

	private Debug debug = new Debug();

	private String contextPath = DEFAULT_CONTEXT_PATH;

	private String secret;

	private String secretHeaderName = DEFAULT_SECRET_HEADER_NAME;

	public String getContextPath() {
		return this.contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public Debug getDebug() {
		return this.debug;
	}

	public void setDebug(Debug debug) {
		this.debug = debug;
	}

	/**
	 * Gets the secret required to be present in the header defined by
	 * {@link #getSecretHeaderName()}.
	 *
	 * @return the secret required to be present in the header defined by
	 * {@link #getSecretHeaderName()}
	 */
	public String getSecret() {
		return secret;
	}

	/**
	 * Sets the secret required to be present in the header defined by
	 * {@link #getSecretHeaderName()}.
	 *
	 * @param secret the secret that will be required.
	 */
	public void setSecret(String secret) {
		this.secret = secret;
	}

	/**
	 * Gets the header name that must have {@link #getSecret()}. The default is
	 * {@link #DEFAULT_SECRET_HEADER_NAME}.
	 *
	 * @return the header name that must have {@link #getSecret()}
	 */
	public String getSecretHeaderName() {
		return secretHeaderName;
	}

	/**
	 * Sets the header name that must have {@link #getSecret()}
	 *
	 * @param secretHeaderName the header name that must have {@link #getSecret()}
	 */
	public void setSecretHeaderName(String secretHeaderName) {
		this.secretHeaderName = secretHeaderName;
	}

	public static class Debug {

		public static final Integer DEFAULT_LOCAL_PORT = 8000;

		private boolean enabled = true;

		private int localPort = DEFAULT_LOCAL_PORT;

		public boolean getEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public int getLocalPort() {
			return this.localPort;
		}

		public void setLocalPort(int localPort) {
			this.localPort = localPort;
		}

	}

}
