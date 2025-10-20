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

import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.http.server.LocalTestWebServer.Connection;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link LocalTestWebServer}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class LocalTestWebServerTests {

	private final LocalTestWebServer server = LocalTestWebServer.of(true, 8080, "");

	@Test
	void httpsWhenHttpsReturnsTrue() {
		assertThat(LocalTestWebServer.of(true, 8080, "").https()).isTrue();
	}

	@Test
	void httpsWhenNotHttpsReturnsFalse() {
		assertThat(LocalTestWebServer.of(false, 8080, "").https()).isTrue();
	}

	@Test
	void uriBuilderWhenHasSlashUriUsesLocalServer() {
		UriBuilder builder = this.server.uriBuilder("/");
		assertThat(builder.toUriString()).isEqualTo("https://localhost:8080/");
	}

	@Test
	void uriBuilderWhenHasEmptyUriUsesLocalServer() {
		UriBuilder builder = this.server.uriBuilder("");
		assertThat(builder.toUriString()).isEqualTo("https://localhost:8080");
	}

	@Test
	void uriBuilderWhenHasNestedPathUsesLocalServer() {
		UriBuilder builder = this.server.uriBuilder("/foo/bar");
		assertThat(builder.toUriString()).isEqualTo("https://localhost:8080/foo/bar");
	}

	@Test
	void uriBuilderWhenHasPathNoStartingWithSlashUsesLocalServer() {
		UriBuilder builder = this.server.uriBuilder("foo/bar");
		assertThat(builder.toUriString()).isEqualTo("https://localhost:8080/foo/bar");
	}

	@Test
	void uriBuilderWhenHasFullUriDoesNotUseLocalServer() {
		UriBuilder builder = this.server.uriBuilder("https://sub.example.com");
		assertThat(builder.toUriString()).isEqualTo("https://sub.example.com");
	}

	@Test
	void uriBuilderFactoryExpandWithMap() {
		UriBuilderFactory factory = this.server.uriBuilderFactory();
		assertThat(factory.expand("/test/{name}", Map.of("name", "value")))
			.isEqualTo(URI.create("https://localhost:8080/test/value"));
	}

	@Test
	void uriBuilderFactoryExpandsWithMap() {
		UriBuilderFactory factory = this.server.uriBuilderFactory();
		assertThat(factory.expand("/test/{name}", "value")).isEqualTo(URI.create("https://localhost:8080/test/value"));
	}

	@Test
	void uriBuilderFactoryExpandsWithVariables() {
		UriBuilderFactory factory = this.server.uriBuilderFactory();
		assertThat(factory.uriString("https://example.com").build()).isEqualTo(URI.create("https://example.com"));
	}

	@Test
	void uriWhenHttp() {
		assertThat(LocalTestWebServer.of(false, 8080, "").uri()).isEqualTo("http://localhost:8080");
	}

	@Test
	void uriWhenHttps() {
		assertThat(LocalTestWebServer.of(true, 4343, "").uri()).isEqualTo("https://locahost:4343");
	}

	@Test
	void urlWhenHasPath() {
		assertThat(LocalTestWebServer.of(true, 8080, "/path").uri()).isEqualTo("https://localhost:8080/path");
	}

	@Test
	void uriUsesSingletonConnection() {
		AtomicInteger counter = new AtomicInteger();
		LocalTestWebServer server = LocalTestWebServer.of(true,
				() -> new Connection(8080, "/" + counter.incrementAndGet()));
		assertThat(server.uri()).isEqualTo("https://localhost:8080/1");
		assertThat(server.uri()).isEqualTo("https://localhost:8080/1");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void ofWhenConnectionSupplierIsNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> LocalTestWebServer.of(true, null))
			.withMessage("'connectionSupplier' must not be null");
	}

	@Test
	void getProvidedReturnsFirstProvided() {
		fail();
	}

	@Test
	void getProvidedWhenNoneReturnsNull() {
		fail();
	}

}
