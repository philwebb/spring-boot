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

import java.util.function.Consumer;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint.Builder;

class ClientHttpRequestFactorySupplierRuntimeHints {

	static void registerHints(RuntimeHints hints, ClassLoader classLoader, Consumer<Builder> callback) {
		// FIXME
		// if
		// (ClassUtils.isPresent(ClientHttpRequestFactorySupplier.APACHE_HTTP_CLIENT_CLASS,
		// classLoader)) {
		// hints.reflection().registerType(HttpComponentsClientHttpRequestFactory.class,
		// (typeHint) -> callback
		// .accept(typeHint.onReachableType(TypeReference.of(ClientHttpRequestFactorySupplier.APACHE_HTTP_CLIENT_CLASS))));
		// }
		// if (ClassUtils.isPresent(ClientHttpRequestFactorySupplier.OKHTTP_CLIENT_CLASS,
		// classLoader)) {
		// hints.reflection().registerType(OkHttp3ClientHttpRequestFactory.class,
		// (typeHint) ->
		// callback.accept(typeHint.onReachableType(TypeReference.of(ClientHttpRequestFactorySupplier.OKHTTP_CLIENT_CLASS))));
		// }
		// hints.reflection().registerType(SimpleClientHttpRequestFactory.class,
		// (typeHint) -> callback
		// .accept(typeHint.onReachableType(TypeReference.of(SimpleClientHttpRequestFactory.class))));
	}

}