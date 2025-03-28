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
 * Settings that can be applied when creating a {@link ClientHttpRequestFactory}.
 *
 * @param httpRedirects the follow redirect strategy to use or null to redirect whenever
 * the underlying library allows it
 * @param connectTimeout the connect timeout
 * @param readTimeout the read timeout
 * @param sslBundle the SSL bundle providing SSL configuration
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 3.4.0
 * @see ClientHttpRequestFactoryBuilder
 */
public record ClientHttpRequestFactorySettings(HttpRedirects httpRedirects, Duration connectTimeout,
		Duration readTimeout, SslBundle sslBundle) {

	private static final ClientHttpRequestFactorySettings defaults = new ClientHttpRequestFactorySettings(
			(HttpRedirects) null, null, null, null);

	public ClientHttpRequestFactorySettings {
		httpRedirects = (httpRedirects != null) ? httpRedirects : HttpRedirects.FOLLOW_WHEN_POSSIBLE;
	}

	/**
	 * Create a new {@link ClientHttpRequestFactorySettings} instance.
	 * @param redirects the follow redirect strategy to use or null to redirect whenever
	 * the underlying library allows it
	 * @param connectTimeout the connect timeout
	 * @param readTimeout the read timeout
	 * @param sslBundle the SSL bundle providing SSL configuration
	 * @deprecated since 3.5.0 for removal in 4.0.0 in favor of
	 * {@link #ClientHttpRequestFactorySettings(HttpRedirects, Duration, Duration, SslBundle)}
	 */
	@Deprecated
	public ClientHttpRequestFactorySettings(Redirects redirects, Duration connectTimeout, Duration readTimeout,
			SslBundle sslBundle) {
		this(Redirects.replace(redirects), connectTimeout, readTimeout, sslBundle);
	}

	/**
	 * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated
	 * connect timeout setting.
	 * @param connectTimeout the new connect timeout setting
	 * @return a new {@link ClientHttpRequestFactorySettings} instance
	 */
	public ClientHttpRequestFactorySettings withConnectTimeout(Duration connectTimeout) {
		return new ClientHttpRequestFactorySettings(this.httpRedirects, connectTimeout, this.readTimeout,
				this.sslBundle);
	}

	/**
	 * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated read
	 * timeout setting.
	 * @param readTimeout the new read timeout setting
	 * @return a new {@link ClientHttpRequestFactorySettings} instance
	 */

	public ClientHttpRequestFactorySettings withReadTimeout(Duration readTimeout) {
		return new ClientHttpRequestFactorySettings(this.httpRedirects, this.connectTimeout, readTimeout,
				this.sslBundle);
	}

	/**
	 * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated SSL
	 * bundle setting.
	 * @param sslBundle the new SSL bundle setting
	 * @return a new {@link ClientHttpRequestFactorySettings} instance
	 */
	public ClientHttpRequestFactorySettings withSslBundle(SslBundle sslBundle) {
		return new ClientHttpRequestFactorySettings(this.httpRedirects, this.connectTimeout, this.readTimeout,
				sslBundle);
	}

	/**
	 * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated
	 * redirect setting.
	 * @param redirects the new redirects setting
	 * @return a new {@link ClientHttpRequestFactorySettings} instance
	 */
	public ClientHttpRequestFactorySettings withRedirects(Redirects redirects) {
		return new ClientHttpRequestFactorySettings(redirects, this.connectTimeout, this.readTimeout, this.sslBundle);
	}

	/**
	 * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated
	 * redirect setting.
	 * @param httpRedirects the new redirects setting
	 * @return a new {@link ClientHttpRequestFactorySettings} instance
	 * @since 3.5.0
	 */
	public ClientHttpRequestFactorySettings withHttpRedirects(HttpRedirects httpRedirects) {
		return new ClientHttpRequestFactorySettings(httpRedirects, this.connectTimeout, this.readTimeout,
				this.sslBundle);
	}

	/**
	 * Return the follow redirect strategy to use or null to redirect whenever the
	 * underlying library allows it.
	 * @return the redirect strategy
	 * @deprecated since 3.5.0 for removal in 4.0.0 in favor of {@link #httpRedirects()}
	 */
	@Deprecated
	public Redirects redirects() {
		return Redirects.forReplaced(httpRedirects());
	}

	/**
	 * Return a new {@link ClientHttpRequestFactorySettings} using defaults for all
	 * settings other than the provided SSL bundle.
	 * @param sslBundle the SSL bundle setting
	 * @return a new {@link ClientHttpRequestFactorySettings} instance
	 */
	public static ClientHttpRequestFactorySettings ofSslBundle(SslBundle sslBundle) {
		return defaults().withSslBundle(sslBundle);
	}

	/**
	 * Use defaults for the {@link ClientHttpRequestFactory} which can differ depending on
	 * the implementation.
	 * @return default settings
	 */
	public static ClientHttpRequestFactorySettings defaults() {
		return defaults;
	}

	/**
	 * Redirect strategies.
	 *
	 * @deprecated since 3.5.0 for removal in 4.0.0 in favor of {@link HttpRedirects}.
	 */
	@Deprecated
	public enum Redirects {

		/**
		 * Follow redirects (if the underlying library has support).
		 */
		FOLLOW_WHEN_POSSIBLE(HttpRedirects.FOLLOW_WHEN_POSSIBLE),

		/**
		 * Follow redirects (fail if the underlying library has no support).
		 */
		FOLLOW(HttpRedirects.FOLLOW_WHEN_POSSIBLE),

		/**
		 * Don't follow redirects (fail if the underlying library has no support).
		 */
		DONT_FOLLOW(HttpRedirects.DONT_FOLLOW);

		private final HttpRedirects replacement;

		Redirects(HttpRedirects replacement) {
			this.replacement = replacement;
		}

		static HttpRedirects replace(Redirects redirects) {
			return (redirects != null) ? redirects.replacement : null;
		}

		static Redirects forReplaced(HttpRedirects httpRedirects) {
			for (Redirects candidate : values()) {
				if (candidate.replacement == httpRedirects) {
					return candidate;
				}
			}
			throw new IllegalStateException("Unable to find Redirects enum value for " + httpRedirects);
		}

	}

}
