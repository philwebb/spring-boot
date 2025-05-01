package org.springframework.boot.autoconfigure.http.client.reactive.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.http.client.reactive.AbstractClientHttpConnectorProperties;

/**
 * {@link AbstractClientHttpConnectorProperties} for reactive HTTP Service clients.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 */
abstract class AbstractHttpReactiveClientServiceProperties extends AbstractClientHttpConnectorProperties {

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