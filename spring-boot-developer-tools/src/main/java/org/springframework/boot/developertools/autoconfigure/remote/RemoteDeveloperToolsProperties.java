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
 */
@ConfigurationProperties(prefix = "spring.developertools.remote")
public class RemoteDeveloperToolsProperties {

	public static final String DEFAULT_CONTEXT_PATH = "/~~~springboot~~~";

	private boolean enabled = false;

	private Debug debug = new Debug();

	private String contextPath = DEFAULT_CONTEXT_PATH;

	public boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

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
