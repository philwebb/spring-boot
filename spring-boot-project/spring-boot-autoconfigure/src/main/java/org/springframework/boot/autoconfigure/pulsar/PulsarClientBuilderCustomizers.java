/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.pulsar;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.pulsar.client.api.ClientBuilder;

import org.springframework.boot.util.LambdaSafe;
import org.springframework.pulsar.core.PulsarClientBuilderCustomizer;
import org.springframework.util.Assert;

/**
 * Configure Pulsar {@link ClientBuilder} with sensible defaults and apply a list of
 * optional {@link PulsarClientBuilderCustomizer customizers}.
 *
 * @author Chris Bono
 */
class PulsarClientBuilderCustomizers implements PulsarClientBuilderCustomizer {

	// FIXME

	private final List<PulsarClientBuilderCustomizer> customizers;

	/**
	 * Creates a new configurer that will use the given properties for configuration.
	 * @param properties properties to use
	 * @param customizers list of customizers to apply or empty list if no customizers
	 */
	public PulsarClientBuilderCustomizers(XPulsarProperties properties,
			List<PulsarClientBuilderCustomizer> customizers) {
		Assert.notNull(customizers, "customizers must not be null");
		this.customizers = customizers;
	}

	/**
	 *
	 */
	public PulsarClientBuilderCustomizers() {
		this.customizers = new ArrayList<>();
	}

	/**
	 * Configure the specified {@link ClientBuilder}. The builder can be further tuned and
	 * default settings can be overridden.
	 * @param clientBuilder the {@link ClientBuilder} instance to configure
	 */
	void configure(ClientBuilder clientBuilder) {
		applyCustomizers(this.customizers, clientBuilder);
	}

	protected void applyCustomizers(List<PulsarClientBuilderCustomizer> clientBuilderCustomizers,
			ClientBuilder clientBuilder) {
		LambdaSafe.callbacks(PulsarClientBuilderCustomizer.class, clientBuilderCustomizers, clientBuilder)
			.withLogger(PulsarClientBuilderCustomizers.class)
			.invoke((customizer) -> customizer.customize(clientBuilder));
	}

	/**
	 * @param pulsarPropertiesClientBuilderCustomizer
	 */
	public void add(PulsarPropertiesClientBuilderCustomizer pulsarPropertiesClientBuilderCustomizer) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @param orderedStream
	 */
	public void addAll(Stream<PulsarClientBuilderCustomizer> orderedStream) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.pulsar.core.PulsarClientBuilderCustomizer#customize(org.apache.
	 * pulsar.client.api.ClientBuilder)
	 */
	@Override
	public void customize(ClientBuilder clientBuilder) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
