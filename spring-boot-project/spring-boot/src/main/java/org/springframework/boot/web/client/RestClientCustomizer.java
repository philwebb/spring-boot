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

package org.springframework.boot.web.client;

import java.util.function.Predicate;

import org.springframework.boot.selector.Selectable;
import org.springframework.boot.selector.Selector;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

/**
 * Callback interface that can be used to customize a
 * {@link org.springframework.web.client.RestClient.Builder RestClient.Builder}.
 *
 * @author Arjen Poutsma
 * @author Phillip Webb
 * @since 3.2.0
 */
@FunctionalInterface
public interface RestClientCustomizer extends Selector<RestClientCustomizer> {

	/**
	 * Callback to customize a {@link org.springframework.web.client.RestClient.Builder
	 * RestClient.Builder} instance.
	 * @param restClientBuilder the client builder to customize
	 */
	void customize(RestClient.Builder restClientBuilder);

	/**
	 * Callback to customize a {@link org.springframework.web.client.RestClient.Builder
	 * RestClient.Builder} instance for a selected {@link Selectable}.
	 * @param restClientBuilder the client builder to customize
	 * @param selected the {@link #selects(Selectable) selected} selectable
	 */
	default void customize(RestClient.Builder restClientBuilder, Selectable selected) {
		customize(restClientBuilder);
	}

	@Override
	default RestClientCustomizer onlyWhen(Predicate<Selectable> predicate) {
		return new ForSelected() {

			@Override
			public boolean selects(Selectable selectable) {
				return (selectable != null) && RestClientCustomizer.this.selects(selectable)
						&& predicate.test(selectable);
			}

			@Override
			public void customize(RestClient.Builder restClientBuilder, Selectable selectable) {
				RestClientCustomizer.this.customize(restClientBuilder, selectable);
			}

		};
	}

	/**
	 * Helper method that can be used to create a {@link RestClientCustomizer} from a
	 * lambda for method chaining.
	 * @param customizer the source customizer
	 * @return the customizer
	 */
	static RestClientCustomizer of(RestClientCustomizer customizer) {
		return customizer;
	}

	/**
	 * Helper method that can be used to create a {@link RestClientCustomizer} from a
	 * lambda for method chaining.
	 * @param customizer the source customizer
	 * @return the customizer
	 */
	static RestClientCustomizer of(ForSelected customizer) {
		return customizer;
	}

	/**
	 * {@link RestClientCustomizer} that can be used when the selected {@link Selectable}
	 * is needed during customization`.
	 */
	interface ForSelected extends RestClientCustomizer {

		@Override
		default void customize(Builder restClientBuilder) {
			customize(restClientBuilder, Selectable.blank());
		}

	}

}
