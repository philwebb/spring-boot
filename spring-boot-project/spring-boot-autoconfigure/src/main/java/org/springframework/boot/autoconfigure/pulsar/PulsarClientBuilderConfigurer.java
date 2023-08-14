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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.PulsarClientException;

import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Client;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.pulsar.core.PulsarClientBuilderCustomizer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Configure Pulsar {@link ClientBuilder} with sensible defaults and apply a list of
 * optional {@link PulsarClientBuilderCustomizer customizers}.
 *
 * @author Chris Bono
 * @since 3.2.0
 */
public class PulsarClientBuilderConfigurer {

	private final PulsarProperties properties;

	private final List<PulsarClientBuilderCustomizer> customizers;

	/**
	 * Creates a new configurer that will use the given properties for configuration.
	 * @param properties properties to use
	 * @param customizers list of customizers to apply or empty list if no customizers
	 */
	public PulsarClientBuilderConfigurer(PulsarProperties properties, List<PulsarClientBuilderCustomizer> customizers) {
		Assert.notNull(properties, "properties must not be null");
		Assert.notNull(customizers, "customizers must not be null");
		this.properties = properties;
		this.customizers = customizers;
	}

	/**
	 * Configure the specified {@link ClientBuilder}. The builder can be further tuned and
	 * default settings can be overridden.
	 * @param clientBuilder the {@link ClientBuilder} instance to configure
	 */
	public void configure(ClientBuilder clientBuilder) {
		applyProperties(this.properties, clientBuilder);
		applyCustomizers(this.customizers, clientBuilder);
	}

	protected void applyProperties(PulsarProperties pulsarProperties, ClientBuilder clientBuilder) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		Client clientProperties = pulsarProperties.getClient();
		map.from(clientProperties::getServiceUrl).to(clientBuilder::serviceUrl);
		map.from(clientProperties::getConnectionTimeout)
			.asInt(Duration::toMillis)
			.to(clientBuilder, (cb, val) -> cb.connectionTimeout(val, TimeUnit.MILLISECONDS));
		map.from(clientProperties::getOperationTimeout)
			.asInt(Duration::toMillis)
			.to(clientBuilder, (cb, val) -> cb.operationTimeout(val, TimeUnit.MILLISECONDS));
		map.from(clientProperties::getLookupTimeout)
			.asInt(Duration::toMillis)
			.to(clientBuilder, (cb, val) -> cb.lookupTimeout(val, TimeUnit.MILLISECONDS));

		// Authentication properties
		applyAuthentication(clientProperties, clientBuilder);
	}

	private void applyAuthentication(Client clientProperties, ClientBuilder clientBuilder) {
		String authPluginClass = clientProperties.getAuthPluginClassName();
		if (StringUtils.hasText(authPluginClass)) {
			String authParams = null;
			if (clientProperties.getAuthentication() != null) {
				authParams = AuthParameterUtils.maybeConvertToEncodedParamString(clientProperties.getAuthentication());
			}
			try {
				clientBuilder.authentication(authPluginClass, authParams);
			}
			catch (PulsarClientException.UnsupportedAuthenticationException ex) {
				throw new IllegalArgumentException("Unable to configure authentication", ex);
			}
		}
	}

	protected void applyCustomizers(List<PulsarClientBuilderCustomizer> clientBuilderCustomizers,
			ClientBuilder clientBuilder) {
		LambdaSafe.callbacks(PulsarClientBuilderCustomizer.class, clientBuilderCustomizers, clientBuilder)
			.withLogger(PulsarClientBuilderConfigurer.class)
			.invoke((customizer) -> customizer.customize(clientBuilder));
	}

}
