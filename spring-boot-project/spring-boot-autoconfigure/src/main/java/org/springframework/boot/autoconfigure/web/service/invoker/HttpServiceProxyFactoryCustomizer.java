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

import java.util.function.Predicate;

import org.springframework.boot.selector.Selectable;
import org.springframework.boot.selector.Selector;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Callback interface that can be used to customize a
 * {@link org.springframework.web.service.invoker.HttpServiceProxyFactory.Builder
 * HttpServiceProxyFactory.Builder}.
 *
 * @author Phillip Webb
 */
@FunctionalInterface
public interface HttpServiceProxyFactoryCustomizer extends Selector<HttpServiceProxyFactoryCustomizer> {

	/**
	 * Callback to customize a
	 * {@link org.springframework.web.service.invoker.HttpServiceProxyFactory.Builder
	 * HttpServiceProxyFactory.Builder}.instance.
	 * @param httpServiceProxyFactoryBuilder the HTTP service proxy factory builder to
	 * customize
	 */
	void customize(HttpServiceProxyFactory.Builder httpServiceProxyFactoryBuilder);

	@Override
	default HttpServiceProxyFactoryCustomizer onlyWhen(Predicate<Selectable> predicate) {
		return new HttpServiceProxyFactoryCustomizer() {

			@Override
			public boolean selects(Selectable selectable) {
				return (selectable != null) && HttpServiceProxyFactoryCustomizer.this.selects(selectable)
						&& predicate.test(selectable);
			}

			@Override
			public void customize(HttpServiceProxyFactory.Builder restClientBuilder) {
				HttpServiceProxyFactoryCustomizer.this.customize(restClientBuilder);
			}

		};
	}

	/**
	 * Helper method that can be used to create a
	 * {@link HttpServiceProxyFactoryCustomizer} from a lambda for method chaining.
	 * @param customizer the source customizer
	 * @return the customizer
	 */
	static HttpServiceProxyFactoryCustomizer of(HttpServiceProxyFactoryCustomizer customizer) {
		return customizer;
	}

}
