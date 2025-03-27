/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.http.client.reactive;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.http.client.reactive.ClientHttpConnector;

/**
 * Settings that can be applied when creating a {@link ClientHttpConnector}.
 *
 * @author Phillip Webb
 * @since 3.5.0
 * @see ClientHttpConnectorBuilder
 */
public record ClientHttpConnectorSettings(Redirects redirects, SslBundle sslBundle) {

	public static ClientHttpConnectorSettings defaults() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	// FIXME extract this or copy paste

	/**
	 * Redirect strategies.
	 */
	public enum Redirects {

		/**
		 * Follow redirects (if the underlying library has support).
		 */
		FOLLOW_WHEN_POSSIBLE,

		/**
		 * Follow redirects (fail if the underlying library has no support).
		 */
		FOLLOW,

		/**
		 * Don't follow redirects (fail if the underlying library has no support).
		 */
		DONT_FOLLOW

	}

}
