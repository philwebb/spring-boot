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

package org.springframework.boot.autoconfigure.http.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for a Spring's blocking HTTP
 * clients.
 *
 * @author Phillip Webb
 * @author Olga Maciaszek-Sharma
 * @since 4.0.0
 */
@ConfigurationProperties("spring.http.client")
public class HttpClientProperties {

	/**
	 * Default settings.
	 */
	private Settings settings = new Settings();

	/**
	 * Group settings.
	 */
	private Map<String, Group> group = new LinkedHashMap<>();

	public Settings getSettings() {
		return this.settings;
	}

	public void setSettings(Settings settings) {
		this.settings = settings;
	}

	public Map<String, Group> getGroup() {
		return this.group;
	}

	public void setGroup(Map<String, Group> group) {
		this.group = group;
	}

	/**
	 * {@link AbstractHttpRequestFactoryProperties} for the default settings.
	 */
	public static class Settings extends AbstractHttpRequestFactoryProperties {

	}

	/**
	 * {@link AbstractHttpRequestFactoryProperties} for an individual group.
	 */
	public static class Group extends AbstractHttpRequestFactoryProperties {

		/**
		 * Base url to set in the underlying HTTP client group. By default, set to
		 * {@code null}.
		 */
		private String baseUrl;

		/**
		 * Default request headers for interface client group. By default, set to empty
		 * {@link Map}.
		 */
		private Map<String, List<String>> defaultHeaders = Collections.emptyMap();

		public String getBaseUrl() {
			return this.baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		public Map<String, List<String>> getDefaultHeaders() {
			return this.defaultHeaders;
		}

		public void setDefaultHeaders(Map<String, List<String>> defaultHeaders) {
			this.defaultHeaders = defaultHeaders;
		}

	}

}
