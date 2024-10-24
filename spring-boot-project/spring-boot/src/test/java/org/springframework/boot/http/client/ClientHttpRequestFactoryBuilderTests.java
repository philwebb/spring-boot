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

import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.Test;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.JdkClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ReactorClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ReflectiveComponentsClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.SimpleClientHttpRequestFactoryBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ClientHttpRequestFactoryBuilder}.
 *
 * @author Phillip Webb
 */
class ClientHttpRequestFactoryBuilderTests {

	@Test
	void detectReturnsExpectedBuilderType() {
		assertThat(ClientHttpRequestFactoryBuilder.detect())
			.isInstanceOf(HttpComponentsClientHttpRequestFactoryBuilder.class);
	}

	@Test
	void getOfGeneralTypeThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> ClientHttpRequestFactoryBuilder.of(ClientHttpRequestFactory.class))
			.withMessage("'requestFactoryType' must be an implementation of ClientHttpRequestFactory");
	}

	@Test
	void getOfSimpleFactoryReturnsSimpleFactoryBuilder() {
		assertThat(ClientHttpRequestFactoryBuilder.of(SimpleClientHttpRequestFactory.class))
			.isInstanceOf(SimpleClientHttpRequestFactoryBuilder.class);
	}

	@Test
	void getOfHttpComponentsFactoryReturnsHttpComponentsFactoryBuilder() {
		assertThat(ClientHttpRequestFactoryBuilder.of(HttpComponentsClientHttpRequestFactory.class))
			.isInstanceOf(HttpComponentsClientHttpRequestFactoryBuilder.class);
	}

	@Test
	void getOfReactorFactoryReturnsReactorFactoryBuilder() {
		assertThat(ClientHttpRequestFactoryBuilder.of(ReactorClientHttpRequestFactory.class))
			.isInstanceOf(ReactorClientHttpRequestFactoryBuilder.class);
	}

	@Test
	void getOfJdkFactoryReturnsJdkFactoryBuilder() {
		assertThat(ClientHttpRequestFactoryBuilder.of(JdkClientHttpRequestFactory.class))
			.isInstanceOf(JdkClientHttpRequestFactoryBuilder.class);
	}

	@Test
	void getOfUnknownTypeCreatesReflectiveFactoryBuilder() {
		ClientHttpRequestFactoryBuilder<TestClientHttpRequestFactory> builder = ClientHttpRequestFactoryBuilder
			.of(TestClientHttpRequestFactory.class);
		assertThat(builder).isInstanceOf(ReflectiveComponentsClientHttpRequestFactoryBuilder.class);
		assertThat(builder.build(null)).isInstanceOf(TestClientHttpRequestFactory.class);
	}

	public static class TestClientHttpRequestFactory implements ClientHttpRequestFactory {

		@Override
		public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
			throw new UnsupportedOperationException();
		}

	}

}
