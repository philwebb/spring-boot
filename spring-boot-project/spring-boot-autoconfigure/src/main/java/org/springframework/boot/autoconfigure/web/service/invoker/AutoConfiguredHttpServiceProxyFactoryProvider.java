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

package org.springframework.boot.autoconfigure.web.service.invoker;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.invoker.HttpServiceProxyFactoryProvider;

/**
 * {@link AutoConfiguredHttpServiceProxyFactoryProvider} that uses a composite of all
 * discovered providers.
 *
 * @author Phillip Webb
 */
class AutoConfiguredHttpServiceProxyFactoryProvider implements HttpServiceProxyFactoryProvider {

	private final HttpServiceProxyFactoryProvider composite;

	public AutoConfiguredHttpServiceProxyFactoryProvider(List<HttpServiceProxyFactoryProvider> providers) {
		this.composite = HttpServiceProxyFactoryProvider.composite(providers);
	}

	@Override
	public @Nullable HttpServiceProxyFactory getHttpServiceProxyFactory(String id) {
		return this.composite.getHttpServiceProxyFactory(id);
	}

}
