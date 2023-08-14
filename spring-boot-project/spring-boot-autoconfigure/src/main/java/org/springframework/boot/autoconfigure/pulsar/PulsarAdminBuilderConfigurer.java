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
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.admin.PulsarAdminBuilder;
import org.apache.pulsar.client.api.PulsarClientException;

import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Admin;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Configure the {@link PulsarAdminBuilder} with sensible defaults.
 *
 * @author Chris Bono
 * @since 3.2.0
 */
public class PulsarAdminBuilderConfigurer {

	private final PulsarProperties properties;

	private final ClientBuilderSslConfigurer sslConfigurer;

	/**
	 * Creates a new configurer that will use the given properties for configuration.
	 * @param properties properties to use
	 * @param sslConfigurer optional SSL settings configurer
	 */
	public PulsarAdminBuilderConfigurer(PulsarProperties properties, ClientBuilderSslConfigurer sslConfigurer) {
		Assert.notNull(properties, "properties must not be null");
		Assert.notNull(sslConfigurer, "sslConfigurer must not be null");
		this.properties = properties;
		this.sslConfigurer = sslConfigurer;
	}

	/**
	 * Configure the specified {@link PulsarAdminBuilder}. The builder can be further
	 * tuned and default settings can be overridden.
	 * @param adminBuilder the {@link PulsarAdminBuilder} instance to configure
	 */
	public void configure(PulsarAdminBuilder adminBuilder) {
		applyProperties(this.properties, adminBuilder);
		this.sslConfigurer.applySsl(new PulsarAdminBuilderSslSettings(adminBuilder),
				this.properties.getAdministration().getSsl());
	}

	protected void applyProperties(PulsarProperties pulsarProperties, PulsarAdminBuilder adminBuilder) {
		Admin adminProperties = pulsarProperties.getAdministration();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(adminProperties::getServiceUrl).to(adminBuilder::serviceHttpUrl);
		map.from(adminProperties::getConnectionTimeout)
			.asInt(Duration::toMillis)
			.to(adminBuilder, (ab, val) -> ab.connectionTimeout(val, TimeUnit.MILLISECONDS));
		map.from(adminProperties::getReadTimeout)
			.asInt(Duration::toMillis)
			.to(adminBuilder, (ab, val) -> ab.readTimeout(val, TimeUnit.MILLISECONDS));
		map.from(adminProperties::getRequestTimeout)
			.asInt(Duration::toMillis)
			.to(adminBuilder, (ab, val) -> ab.requestTimeout(val, TimeUnit.MILLISECONDS));
		applyAuthentication(adminProperties, adminBuilder);
	}

	protected void applyAuthentication(Admin adminProperties, PulsarAdminBuilder adminBuilder) {
		String authPluginClass = adminProperties.getAuthPluginClassName();
		if (StringUtils.hasText(authPluginClass)) {
			String authParams = null;
			if (adminProperties.getAuthentication() != null) {
				authParams = AuthParameterUtils.maybeConvertToEncodedParamString(adminProperties.getAuthentication());
			}
			try {
				adminBuilder.authentication(authPluginClass, authParams);
			}
			catch (PulsarClientException.UnsupportedAuthenticationException ex) {
				throw new IllegalArgumentException("Unable to configure authentication", ex);
			}
		}
	}

}
