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

import java.util.List;
import java.util.function.Consumer;

import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.client.reactive.HttpComponentsClientHttpConnector;
import org.springframework.util.ClassUtils;

/**
 * Builder for {@link ClientHttpRequestFactoryBuilder#httpComponents()}.
 *
 * @author Phillip Webb
 */
public class HttpComponentsClientHttpConnectorBuilder
		extends AbstractClientHttpRequestFactoryBuilder<HttpComponentsClientHttpConnector> {

	HttpComponentsClientHttpConnectorBuilder() {
		this(null);
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	protected HttpComponentsClientHttpConnectorBuilder(List<Consumer<HttpComponentsClientHttpConnector>> customizers) {
		super(customizers);
	}

	@Override
	protected HttpComponentsClientHttpConnector createClientHttpConnector(HttpClientSettings settings) {
		CloseableHttpAsyncClient client = null;
		return new HttpComponentsClientHttpConnector(client);
	}

	static class Classes {

		static final String HTTP_CLIENTS = "org.apache.hc.client5.http.impl.classic.HttpClients";

		static final boolean PRESENT = ClassUtils.isPresent(HTTP_CLIENTS, null);

	}

}
