/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.reactive.function.client;

import java.net.URI;
import java.util.function.Function;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.util.function.SingletonSupplier;

/**
 * Lazy {@link ClientHttpConnector} that delegates to a {@link Supplier} on first use.
 *
 * @author Phillip Webb
 */
class LazyClientHttpConnector implements ClientHttpConnector {

	private final SingletonSupplier<ClientHttpConnector> clientHttpConnectorSupplier;

	LazyClientHttpConnector(Supplier<ClientHttpConnector> clientHttpConnectorSupplier) {
		this.clientHttpConnectorSupplier = SingletonSupplier.of(clientHttpConnectorSupplier);
	}

	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
			Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {
		return getClientHttpConnector().connect(method, uri, requestCallback);
	}

	ClientHttpConnector getClientHttpConnector() {
		return this.clientHttpConnectorSupplier.get();
	}

}
