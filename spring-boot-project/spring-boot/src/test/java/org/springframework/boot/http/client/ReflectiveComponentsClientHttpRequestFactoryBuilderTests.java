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

import org.eclipse.jetty.client.HttpClient;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for {@link ReflectiveComponentsClientHttpRequestFactoryBuilder}.
 *
 * @author Phillip Webb
 */
class ReflectiveComponentsClientHttpRequestFactoryBuilderTests
		extends AbstractClientHttpRequestFactoryBuilderTests<ClientHttpRequestFactory> {

	ReflectiveComponentsClientHttpRequestFactoryBuilderTests() {
		super(ClientHttpRequestFactory.class, ClientHttpRequestFactoryBuilder.of(JettyClientHttpRequestFactory::new));
	}

	@Override
	void connectWithSslBundle(String httpMethod) throws Exception {
		// FIXME
	}

	@Override
	protected long connectTimeout(ClientHttpRequestFactory requestFactory) {
		return ((HttpClient) ReflectionTestUtils.getField(requestFactory, "httpClient")).getConnectTimeout();
	}

	@Override
	protected long readTimeout(ClientHttpRequestFactory requestFactory) {
		return (long) ReflectionTestUtils.getField(requestFactory, "readTimeout");
	}

}
