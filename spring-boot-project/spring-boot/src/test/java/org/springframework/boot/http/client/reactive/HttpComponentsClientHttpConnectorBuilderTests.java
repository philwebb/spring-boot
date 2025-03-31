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

package org.springframework.boot.http.client.reactive;

import org.springframework.http.client.reactive.HttpComponentsClientHttpConnector;

/**
 * Tests for {@link HttpComponentsClientHttpConnectorBuilder}.
 *
 * @author Phillip Webb
 */
class HttpComponentsClientHttpConnectorBuilderTests
		extends AbstractClientHttpConnectorBuilderTests<HttpComponentsClientHttpConnector> {

	HttpComponentsClientHttpConnectorBuilderTests() {
		super(HttpComponentsClientHttpConnector.class, ClientHttpConnectorBuilder.httpComponents());
	}
	//
	// @Test
	// void withCustomizers() {
	// TestCustomizer<HttpClientBuilder> httpClientCustomizer1 = new TestCustomizer<>();
	// TestCustomizer<HttpClientBuilder> httpClientCustomizer2 = new TestCustomizer<>();
	// TestCustomizer<PoolingHttpClientConnectionManagerBuilder>
	// connectionManagerCustomizer = new TestCustomizer<>();
	// TestCustomizer<SocketConfig.Builder> socketConfigCustomizer = new
	// TestCustomizer<>();
	// TestCustomizer<SocketConfig.Builder> socketConfigCustomizer1 = new
	// TestCustomizer<>();
	// TestCustomizer<RequestConfig.Builder> defaultRequestConfigCustomizer = new
	// TestCustomizer<>();
	// TestCustomizer<RequestConfig.Builder> defaultRequestConfigCustomizer1 = new
	// TestCustomizer<>();
	// ClientHttpRequestFactoryBuilder.httpComponents()
	// .withHttpClientCustomizer(httpClientCustomizer1)
	// .withHttpClientCustomizer(httpClientCustomizer2)
	// .withConnectionManagerCustomizer(connectionManagerCustomizer)
	// .withSocketConfigCustomizer(socketConfigCustomizer)
	// .withSocketConfigCustomizer(socketConfigCustomizer1)
	// .withDefaultRequestConfigCustomizer(defaultRequestConfigCustomizer)
	// .withDefaultRequestConfigCustomizer(defaultRequestConfigCustomizer1)
	// .build();
	// httpClientCustomizer1.assertCalled();
	// httpClientCustomizer2.assertCalled();
	// connectionManagerCustomizer.assertCalled();
	// socketConfigCustomizer.assertCalled();
	// socketConfigCustomizer1.assertCalled();
	// defaultRequestConfigCustomizer.assertCalled();
	// defaultRequestConfigCustomizer1.assertCalled();
	// }
	//
	// @Test
	// @WithPackageResources("test.jks")
	// void withTlsSocketStrategyFactory() {
	// ClientHttpRequestFactorySettings settings =
	// ClientHttpRequestFactorySettings.ofSslBundle(sslBundle());
	// List<SslBundle> bundles = new ArrayList<>();
	// Function<SslBundle, TlsSocketStrategy> tlsSocketStrategyFactory = (bundle) -> {
	// bundles.add(bundle);
	// return (socket, target, port, attachment, context) -> null;
	// };
	// ClientHttpRequestFactoryBuilder.httpComponents()
	// .withTlsSocketStrategyFactory(tlsSocketStrategyFactory)
	// .build(settings);
	// assertThat(bundles).contains(settings.sslBundle());
	// }
	//
	// @Override
	// protected long connectTimeout(HttpComponentsClientHttpRequestFactory
	// requestFactory) {
	// return (long) ReflectionTestUtils.getField(requestFactory, "connectTimeout");
	// }
	//
	// @Override
	// @SuppressWarnings("unchecked")
	// protected long readTimeout(HttpComponentsClientHttpRequestFactory requestFactory) {
	// HttpClient httpClient = requestFactory.getHttpClient();
	// Object connectionManager = ReflectionTestUtils.getField(httpClient, "connManager");
	// SocketConfig socketConfig = ((Resolver<HttpRoute, SocketConfig>)
	// ReflectionTestUtils.getField(connectionManager,
	// "socketConfigResolver"))
	// .resolve(null);
	// return socketConfig.getSoTimeout().toMilliseconds();
	// }

}
