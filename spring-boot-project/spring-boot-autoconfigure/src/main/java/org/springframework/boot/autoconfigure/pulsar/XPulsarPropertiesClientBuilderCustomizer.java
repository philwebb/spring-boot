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

package org.springframework.boot.autoconfigure.pulsar;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.PulsarClientException.UnsupportedAuthenticationException;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.pulsar.core.PulsarClientBuilderCustomizer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link PulsarClientBuilderCustomizer} to apply {@link PulsarProperties.Client client
 * properties}.
 *
 * @author Chris Bono
 */
class XPulsarPropertiesClientBuilderCustomizer implements PulsarClientBuilderCustomizer {

	// FIXME move?

	private final PulsarProperties.Client properties;

	XPulsarPropertiesClientBuilderCustomizer(PulsarProperties properties) {
		Assert.notNull(properties, "properties must not be null");
		this.properties = properties.getClient();
	}

	@Override
	public void customize(ClientBuilder clientBuilder) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(this.properties::getServiceUrl).to(clientBuilder::serviceUrl);
		map.from(this.properties::getConnectionTimeout).to(timeoutProperty(clientBuilder::connectionTimeout));
		map.from(this.properties::getOperationTimeout).to(timeoutProperty(clientBuilder::operationTimeout));
		map.from(this.properties::getLookupTimeout).to(timeoutProperty(clientBuilder::lookupTimeout));
		applyAuthentication(clientBuilder);
	}

	private Consumer<Duration> timeoutProperty(BiConsumer<Integer, TimeUnit> setter) {
		return (duration) -> setter.accept((int) duration.toMillis(), TimeUnit.MILLISECONDS);
	}

	private void applyAuthentication(ClientBuilder clientBuilder) {
		String authPluginClassName = this.properties.getAuthPluginClassName();
		Map<String, String> authentication = this.properties.getAuthentication();
		if (StringUtils.hasText(authPluginClassName)) {
			try {
				String maybeConvertToEncodedParamString = XAuthParameterUtils
					.maybeConvertToEncodedParamString(authentication);
				clientBuilder.authentication(authPluginClassName, maybeConvertToEncodedParamString);
			}
			catch (UnsupportedAuthenticationException ex) {
				throw new IllegalStateException("Unable to configure authentication", ex);
			}
		}
	}

}
