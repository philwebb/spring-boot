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
 * @author pwebb
 */
@FunctionalInterface
public interface HttpServiceProxyFactoryCustomizer extends Selector<HttpServiceProxyFactoryCustomizer> {

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

	static HttpServiceProxyFactoryCustomizer of(HttpServiceProxyFactoryCustomizer customizer) {
		return customizer;
	}

}
