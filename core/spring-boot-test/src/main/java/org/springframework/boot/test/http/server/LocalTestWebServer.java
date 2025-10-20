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

import java.util.Objects;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Provides details of a locally running test web server which may have been started on a
 * dynamic port.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class LocalTestWebServer {

	private final boolean https;

	private final SingletonSupplier<Connection> connection;

	private LocalTestWebServer(boolean https, Supplier<Connection> connectionSupplier) {
		Assert.notNull(connectionSupplier, "'connectionSupplier' must not be null");
		this.https = https;
		this.connection = SingletonSupplier.of(connectionSupplier);
	}

	/**
	 * Return if the server should be connected to over a {@code HTTPS} URI. This method
	 * can be safely called before the local test server is fully running.
	 * @return if the web server uses an HTTPS address
	 */
	public boolean https() {
		return this.https;
	}

	/**
	 * Return a new {@link UriBuilder} with the base URI template initialized from the
	 * local server {@link #uri()}. This method should only be called once the local test
	 * server is fully running.
	 * @param uri a URI template for the builder or {@code null}
	 * @return a new {@link UriBuilder} instance
	 */
	public UriBuilder uriBuilder(@Nullable String uri) {
		UriBuilderFactory factory = uriBuilderFactory();
		return (uri != null) ? factory.uriString(uri) : factory.builder();
	}

	/**
	 * Return a new {@link UriBuilderFactory} with the base URI template initialized from
	 * the local server {@link #uri()}.This method should only be called once the local
	 * test server is fully running.
	 * @return a new {@link UriBuilderFactory}
	 */
	public UriBuilderFactory uriBuilderFactory() {
		return new DefaultUriBuilderFactory(uri());
	}

	/**
	 * Return the URI of the running local test server.This method should only be called
	 * once the local test server is fully running.
	 * @return the URI of the server
	 */
	public String uri() {
		return this.connection.get().uri(this.https);
	}

	/**
	 * Factory method to create a new {@link LocalTestWebServer} instance.
	 * @param https if the server is HTTPS
	 * @param port the port of the running server
	 * @param contextPath the context path of the running server
	 * @return a new {@link LocalTestWebServer} instance
	 */
	static LocalTestWebServer of(boolean https, int port, String contextPath) {
		return of(https, () -> new Connection(port, contextPath));
	}

	/**
	 * Factory method to create a new {@link LocalTestWebServer} instance.
	 * @param https if the server is HTTPS
	 * @param connectionSupplier a supplier to provide the server connection
	 * @return a new {@link LocalTestWebServer} instance
	 */
	static LocalTestWebServer of(boolean https, Supplier<Connection> connectionSupplier) {
		return new LocalTestWebServer(https, connectionSupplier);
	}

	/**
	 * Return a {@link LocalTestWebServer} instance provided from the
	 * {@link ApplicationContext} or {@code null} of no local server is started or could
	 * be provided.
	 * @param applicationContext the application context
	 * @return the local test web server or {@code null}
	 */
	static @Nullable LocalTestWebServer getProvided(ApplicationContext applicationContext) {
		Assert.notNull(applicationContext, "'applicationContext' must not be null");
		SpringFactoriesLoader loader = SpringFactoriesLoader
			.forDefaultResourceLocation(applicationContext.getClassLoader());
		return loader.load(Provider.class, ArgumentResolver.of(ApplicationContext.class, applicationContext))
			.stream()
			.map(Provider::getLocalTestWebServer)
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Details of a connection to the local test web server.
	 *
	 * @param port the port of the running server
	 * @param path the path of the running server
	 */
	record Connection(int port, String path) {

		String uri(boolean https) {
			StringBuilder uri = new StringBuilder((!https) ? "http://" : "https://");
			uri.append("localhost:");
			uri.append(port());
			uri.append(path());
			return uri.toString();
		}

	}

	/**
	 * Strategy used to provide the running {@link LocalTestWebServer}. Implementations
	 * can be registered in {@code spring.factories} and may accept an
	 * {@link ApplicationContext} constructor argument.
	 *
	 */
	@FunctionalInterface
	interface Provider {

		/**
		 * Return the provided {@link LocalTestWebServer} or {@code null}.
		 * @return the local test web server
		 */
		@Nullable LocalTestWebServer getLocalTestWebServer();

	}

}
