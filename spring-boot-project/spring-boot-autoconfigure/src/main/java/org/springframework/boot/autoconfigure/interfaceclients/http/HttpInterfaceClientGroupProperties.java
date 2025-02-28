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

package org.springframework.boot.autoconfigure.interfaceclients.http;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.http.client.HttpClientProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings.Redirects;

/**
 * Properties for HTTP Interface Client Groups. Contains group registration properties and
 * HTTP client properties.
 *
 * @author Olga Maciaszek-Sharma
 * @since 4.0.0
 */
public class HttpInterfaceClientGroupProperties {

	// FIXME: use for adding clients

	/**
	 * Base url to set in the underlying HTTP client group. By default, set to
	 * {@code null}.
	 */
	private String baseUrl = null;

	/**
	 * Name to set in the underlying HTTP client group. By default, set to {@code null}.
	 */
	private String name = null;

	// basePackages, httpServiceTypes??? - in Boot generally the code concerns would not
	// go there
	// making name obligatory to avoid url being a key

	/**
	 * Default request connect timeout for interface client group. By default, set to
	 * {@code null}.
	 */
	private Duration connectTimeout = null;

	/**
	 * Default request read timeout for interface client group. By default, set to
	 * {@code null}.
	 */
	private Duration readTimeout = null;

	/**
	 * Default request headers for interface client group. By default, set to empty
	 * {@link Map}.
	 */
	private Map<String, List<String>> defaultHeaders = Collections.emptyMap();

	// TODO: add implementation

	/**
	 * Handling for HTTP redirects. By default, set to
	 * {@link Redirects#FOLLOW_WHEN_POSSIBLE}
	 */
	private Redirects redirects = Redirects.FOLLOW_WHEN_POSSIBLE;

	/**
	 * Default SSL configuration for a client HTTP request.
	 */
	private final HttpClientProperties.Ssl ssl = new HttpClientProperties.Ssl();

	public String getBaseUrl() {
		return this.baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

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

	public Map<String, List<String>> getDefaultHeaders() {
		return this.defaultHeaders;
	}

	public void setDefaultHeaders(Map<String, List<String>> defaultHeaders) {
		this.defaultHeaders = defaultHeaders;
	}

	public Redirects getRedirects() {
		return this.redirects;
	}

	public void setRedirects(Redirects redirects) {
		this.redirects = redirects;
	}

	public HttpClientProperties.Ssl getSsl() {
		return this.ssl;
	}

	/**
	 * SSL configuration.
	 */
	public static class Ssl {

		/**
		 * SSL bundle to use.
		 */
		private String bundle;

		public String getBundle() {
			return this.bundle;
		}

		public void setBundle(String bundle) {
			this.bundle = bundle;
		}

	}

}
