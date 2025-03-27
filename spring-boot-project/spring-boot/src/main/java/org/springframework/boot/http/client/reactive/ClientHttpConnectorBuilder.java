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

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.HttpComponentsClientHttpConnector;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.Assert;

/**
 * Interface used to build a fully configured {@link ClientHttpConnector}. Builders for
 * {@link #httpComponents() Apache HTTP Components}, {@link #jetty() Jetty},
 * {@link #reactor() Reactor} and {@link #jdk() JDK} can be obtained using the factory
 * methods on this interface. The {@link #of(Class)} and {@link #of(Supplier)} methods may
 * be used to instantiate other {@link ClientHttpConnector} instances using reflection.
 *
 * @param <T> the {@link ClientHttpConnector} type
 * @author Phillip Webb
 * @since 3.4.0
 */
@FunctionalInterface
public interface ClientHttpConnectorBuilder<T extends ClientHttpConnector> {

	/**
	 * Build a default configured {@link ClientHttpRequestFactory}.
	 * @return a default configured {@link ClientHttpRequestFactory}.
	 */
	default T build() {
		return build(null);
	}

	/**
	 * Build a fully configured {@link ClientHttpRequestFactory}, applying the given
	 * {@code settings} if they are provided.
	 * @param settings the settings to apply or {@code null}
	 * @return a fully configured {@link ClientHttpRequestFactory}.
	 */
	T build(ClientHttpConnectorSettings settings);

	/**
	 * Return a new {@link ClientHttpRequestFactoryBuilder} that applies the given
	 * customizer to the {@link ClientHttpRequestFactory} after it has been built.
	 * @param customizer the customizers to apply
	 * @return a new {@link ClientHttpRequestFactoryBuilder} instance
	 */
	default ClientHttpConnectorBuilder<T> withCustomizer(Consumer<T> customizer) {
		return withCustomizers(List.of(customizer));
	}

	/**
	 * Return a new {@link ClientHttpRequestFactoryBuilder} that applies the given
	 * customizers to the {@link ClientHttpRequestFactory} after it has been built.
	 * @param customizers the customizers to apply
	 * @return a new {@link ClientHttpRequestFactoryBuilder} instance
	 */
	@SuppressWarnings("unchecked")
	default ClientHttpConnectorBuilder<T> withCustomizers(Collection<Consumer<T>> customizers) {
		Assert.notNull(customizers, "'customizers' must not be null");
		Assert.noNullElements(customizers, "'customizers' must not contain null elements");
		return (settings) -> {
			T factory = build(settings);
			LambdaSafe.callbacks(Consumer.class, customizers, factory).invoke((consumer) -> consumer.accept(factory));
			return factory;
		};
	}

	/**
	 * Return a {@link HttpComponentsClientHttpConnectorBuilder} that can be used to build
	 * a {@link HttpComponentsClientHttpConnector}.
	 * @return a new {@link HttpComponentsClientHttpConnectorBuilder}
	 */
	static HttpComponentsClientHttpConnectorBuilder httpComponents() {
		return new HttpComponentsClientHttpConnectorBuilder();
	}

	/**
	 * Return a {@link JettyClientHttpConnectorBuilder} that can be used to build a
	 * {@link JettyClientHttpConnector}.
	 * @return a new {@link JettyClientHttpConnectorBuilder}
	 */
	static JettyClientHttpConnectorBuilder jetty() {
		return new JettyClientHttpConnectorBuilder();
	}

	/**
	 * Return a {@link ReactorClientHttpConnectorBuilder} that can be used to build a
	 * {@link ReactorClientHttpConnector}.
	 * @return a new {@link ReactorClientHttpConnectorBuilder}
	 */
	static ReactorClientHttpConnectorBuilder reactor() {
		return new ReactorClientHttpConnectorBuilder();
	}

	/**
	 * Return a {@link JdkClientHttpConnectorBuilder} that can be used to build a
	 * {@link JdkClientHttpConnector} .
	 * @return a new {@link JdkClientHttpConnectorBuilder}
	 */
	static JdkClientHttpConnectorBuilder jdk() {
		return new JdkClientHttpConnectorBuilder();
	}

	// FIXME of

}
