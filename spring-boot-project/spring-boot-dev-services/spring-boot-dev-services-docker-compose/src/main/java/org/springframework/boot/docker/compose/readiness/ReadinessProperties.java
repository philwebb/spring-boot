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

package org.springframework.boot.docker.compose.readiness;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Readiness configuration properties.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@ConfigurationProperties(ReadinessProperties.NAME)
class ReadinessProperties {

	static final String NAME = "spring.docker.compose.readiness";

	/**
	 * Timeout of the readiness checks.
	 */
	private Duration timeout = Duration.ofSeconds(30);

	/**
	 * TCP properties.
	 */
	private final Tcp tcp = new Tcp();

	public Duration getTimeout() {
		return this.timeout;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	Tcp getTcp() {
		return this.tcp;
	}

	/**
	 * TCP properties.
	 */
	static class Tcp {

		/**
		 * Timeout for connections.
		 */
		private Duration connectTimeout = Duration.ofMillis(200);

		/**
		 * Timeout for reads.
		 */
		private Duration readTimeout = Duration.ofMillis(200);

		public Duration getConnectTimeout() {
			return this.connectTimeout;
		}

		public void setConnectTimeout(Duration connectTimeout) {
			this.connectTimeout = connectTimeout;
		}

		public Duration getReadTimeout() {
			return this.readTimeout;
		}

		public void setReadTimeout(Duration readTimeout) {
			this.readTimeout = readTimeout;
		}

	}

}
