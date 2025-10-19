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

package org.springframework.boot.test.http.server;

import java.util.function.Supplier;

import org.springframework.context.ApplicationContext;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;

/**
 * @author Phillip Webb
 */
public class LocalTestWebServer {

	private LocalTestWebServer(boolean ssl, Supplier<Connection> connection) {

	}

	public UriBuilder uriBuilder(String uri) {
		return uriBuilderFactory().uriString(uri);
	}

	public UriBuilderFactory uriBuilderFactory() {
		// return new DefaultUriBuilderFactory(baseUrl());
		return null;
	}

	static LocalTestWebServer of(boolean ssl, int port, String contextPath) {
		return of(ssl, () -> new Connection(port, contextPath));
	}

	static LocalTestWebServer of(boolean ssl, Supplier<Connection> connection) {
		return new LocalTestWebServer(ssl, connection);
	}

	static LocalTestWebServer getProvided(ApplicationContext applicationContext) {
		return null;
	}

	record Connection(int port, String contextPath) {

	}

	@FunctionalInterface
	interface Provider {

		LocalTestWebServer getLocalTestWebServer();

	}

}
