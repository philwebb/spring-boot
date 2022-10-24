/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.web.client;

import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/**
 * {@link ClientHttpRequestFactorySupplier} for
 * {@link HttpComponentsClientHttpRequestFactory}.
 *
 * @author Andy Wilkinson
 */
class HttpComponentsClientHttpRequestFactorySupplier implements ClientHttpRequestFactorySupplier {

	@Override
	public ClientHttpRequestFactory get(Settings settings) {
		HttpComponentsClientHttpRequestFactory factory;
		if (settings.readTimeout() != null) {
			SocketConfig.Builder socketConfigBuilder = SocketConfig.custom();
			socketConfigBuilder = socketConfigBuilder.setSoTimeout((int) settings.readTimeout().toMillis(),
					TimeUnit.MILLISECONDS);
			PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
					.setDefaultSocketConfig(socketConfigBuilder.build()).build();
			CloseableHttpClient httpClient = HttpClientBuilder.create().setConnectionManager(connectionManager).build();
			factory = new HttpComponentsClientHttpRequestFactory(httpClient);
		}
		else {
			factory = new HttpComponentsClientHttpRequestFactory();
		}
		if (settings.connectTimeout() != null) {
			factory.setConnectTimeout((int) settings.connectTimeout().toMillis());
		}
		if (settings.bufferRequestBody() != null) {
			factory.setBufferRequestBody(settings.bufferRequestBody());
		}
		return factory;
	}

}
