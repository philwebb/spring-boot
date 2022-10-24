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

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

class ReflectiveClientHttpRequestFactorySupplierRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		// FIXME
		// hints.reflection().registerField(Objects.requireNonNull(
		// ReflectionUtils.findField(AbstractClientHttpRequestFactoryWrapper.class,
		// "requestFactory")));
		// KnownFactoriesClientHttpRequestFactorySupplier.KnownFactoriesClientHttpRequestFactorySupplierRuntimeHints
		// .registerHints(hints, classLoader, (hint) -> {
		// hint.withMethod("setConnectTimeout", TypeReference.listOf(int.class),
		// ExecutableMode.INVOKE);
		// hint.withMethod("setReadTimeout", TypeReference.listOf(int.class),
		// ExecutableMode.INVOKE);
		// hint.withMethod("setBufferRequestBody", TypeReference.listOf(boolean.class),
		// ExecutableMode.INVOKE);
		// });
	}

}