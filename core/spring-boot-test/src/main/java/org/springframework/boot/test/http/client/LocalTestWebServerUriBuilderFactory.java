/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.test.http.client;

import java.net.URI;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.util.Assert;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author pwebb
 */
public class LocalTestWebServerUriBuilderFactory implements UriBuilderFactory {

	private LocalTestWebServer localTestWebServer;

	public LocalTestWebServerUriBuilderFactory(LocalTestWebServer localTestWebServer) {
		Assert.notNull(localTestWebServer, "'localTestWebServer' must not be null");
		this.localTestWebServer = localTestWebServer;
	}

	@Override
	public UriBuilder uriString(String uriTemplate) {
		return getUriBuilderFactory().uriString(uriTemplate);
	}

	@Override
	public UriBuilder builder() {
		return UriComponentsBuilder.newInstance();
	}

	@Override
	public URI expand(String uriTemplate, Map<String, ?> uriVariables) {
		return getUriBuilderFactory().expand(uriTemplate, uriVariables);
	}

	@Override
	public URI expand(String uriTemplate, @Nullable Object... uriVariables) {
		return getUriBuilderFactory().expand(uriTemplate, uriVariables);
	}

	private DefaultUriBuilderFactory getUriBuilderFactory() {
		return this.localTestWebServer.connection().uriBuilderFactory();
	}

	/**
	 * Get a {@link UriBuilderFactory} instance for the given local test web server.
	 * @param localTestWebServer the local test web server or {@code null} if no local
	 * test server is running
	 * @return a factory for the given local test web server
	 */
	public static UriBuilderFactory get(@Nullable LocalTestWebServer localTestWebServer) {
		return (localTestWebServer != null) ? new LocalTestWebServerUriBuilderFactory(localTestWebServer)
				: new DefaultUriBuilderFactory();
	}

}
