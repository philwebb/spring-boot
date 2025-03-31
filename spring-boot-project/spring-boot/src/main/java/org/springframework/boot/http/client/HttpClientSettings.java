/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.http.client;

import java.time.Duration;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.http.client.ClientHttpRequestFactory;

/**
 * Settings that can be applied when creating a blocking or reactive HTTP client.
 *
 * @param redirects the follow redirect strategy to use or null to redirect whenever the
 * underlying library allows it
 * @param connectTimeout the connect timeout
 * @param readTimeout the read timeout
 * @param sslBundle the SSL bundle providing SSL configuration
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 3.5.0
 * @see ClientHttpRequestFactoryBuilder
 */
public record HttpClientSettings(Redirects redirects, Duration connectTimeout, Duration readTimeout,
		SslBundle sslBundle) {

	private static final HttpClientSettings defaults = new HttpClientSettings(null, null, null, null);

	public HttpClientSettings {
		redirects = (redirects != null) ? redirects : Redirects.FOLLOW_WHEN_POSSIBLE;
	}

	/**
	 * Return a new {@link HttpClientSettings} instance with an updated connect timeout
	 * setting.
	 * @param connectTimeout the new connect timeout setting
	 * @return a new {@link HttpClientSettings} instance
	 */
	public HttpClientSettings withConnectTimeout(Duration connectTimeout) {
		return new HttpClientSettings(this.redirects, connectTimeout, this.readTimeout, this.sslBundle);
	}

	/**
	 * Return a new {@link HttpClientSettings} instance with an updated read timeout
	 * setting.
	 * @param readTimeout the new read timeout setting
	 * @return a new {@link HttpClientSettings} instance
	 */

	public HttpClientSettings withReadTimeout(Duration readTimeout) {
		return new HttpClientSettings(this.redirects, this.connectTimeout, readTimeout, this.sslBundle);
	}

	/**
	 * Return a new {@link HttpClientSettings} instance with an updated timeout setting.
	 * @param connectTimeout the new connect timeout
	 * @param readTimeout the new read timeout
	 * @return a new {@link HttpClientSettings} instance
	 */
	public HttpClientSettings withTimeouts(Duration connectTimeout, Duration readTimeout) {
		return new HttpClientSettings(this.redirects, connectTimeout, readTimeout, this.sslBundle);
	}

	/**
	 * Return a new {@link HttpClientSettings} instance with an updated SSL bundle
	 * setting.
	 * @param sslBundle the new SSL bundle setting
	 * @return a new {@link HttpClientSettings} instance
	 */
	public HttpClientSettings withSslBundle(SslBundle sslBundle) {
		return new HttpClientSettings(this.redirects, this.connectTimeout, this.readTimeout, sslBundle);
	}

	/**
	 * Return a new {@link HttpClientSettings} instance with an updated redirect setting.
	 * @param redirects the new redirects setting
	 * @return a new {@link HttpClientSettings} instance
	 */
	public HttpClientSettings withRedirects(Redirects redirects) {
		return new HttpClientSettings(redirects, this.connectTimeout, this.readTimeout, this.sslBundle);
	}

	/**
	 * Return a new {@link HttpClientSettings} using defaults for all settings other than
	 * the provided SSL bundle.
	 * @param sslBundle the SSL bundle setting
	 * @return a new {@link HttpClientSettings} instance
	 */
	public static HttpClientSettings ofSslBundle(SslBundle sslBundle) {
		return defaults().withSslBundle(sslBundle);
	}

	/**
	 * Use defaults for the {@link ClientHttpRequestFactory} which can differ depending on
	 * the implementation.
	 * @return default settings
	 */
	public static HttpClientSettings defaults() {
		return defaults;
	}

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
