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

package org.springframework.boot.http.client;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * @author pwebb
 */
class TempJdkHttpClientBuilderTests {

	@Test
	void testName() throws Exception {
		ProxySelector delegatePs = ProxySelector.getDefault();
		JdkHttpClientBuilder builder = new JdkHttpClientBuilder().withCustomizer((b) -> {
			b.proxy(new ProxySelector() {

				@Override
				public List<Proxy> select(URI uri) {
					System.err.println(uri);
					if (true) {
						throw new IllegalStateException("uri");
					}
					return delegatePs.select(uri);
				}

				@Override
				public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
					delegatePs.connectFailed(uri, sa, ioe);
				}

			});
		});
		HttpClient client = builder.build(HttpClientSettings.defaults());
		HttpRequest request = HttpRequest.newBuilder(new URI("http://example.com")).build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		// 4. Process the response
		System.out.println("Status Code: " + response.statusCode());
		System.out.println("Response Body: " + response.body().substring(0, 200) + "...");
	}

}
