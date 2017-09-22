/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.Provider;
import org.springframework.boot.context.properties.bind.convert.BinderConversionService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.Builder;

/**
 * Adapter class to convert {@link OAuth2ClientProperties} to a
 * {@link ClientRegistration}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
class OAuth2ClientPropertiesRegistrationAdapter {

	public static Map<String, ClientRegistration> getClientRegistrations(
			OAuth2ClientProperties properties) {
		Map<String, ClientRegistration> clientRegistrations = new HashMap<>();
		properties.getRegistration().forEach((key, value) -> {
			clientRegistrations.put(key,
					getClientRegistration(value, properties.getProvider()));
		});
		return clientRegistrations;
	}

	private static ClientRegistration getClientRegistration(
			OAuth2ClientProperties.Registration properties,
			Map<String, Provider> providers) {
		ClientRegistration.Builder builder = getBuilder(properties.getClientId(),
				properties.getProvider(), providers);
		return builder.build();
	}

	private static ClientRegistration.Builder getBuilder(String clientId,
			String providerId, Map<String, Provider> providers) {
		if (providers.containsKey(providerId)) {
			return getBuilder(clientId, providers.get(providerId));
		}
		CommonOAuth2Provider commonProvider = findCommonProvider(providerId);
		if (commonProvider != null) {
			return commonProvider.getBuilder(clientId);
		}
		return new ClientRegistration.Builder(clientId);
	}

	private static CommonOAuth2Provider findCommonProvider(String providerId) {
		try {
			return new BinderConversionService(null).convert(providerId,
					CommonOAuth2Provider.class);
		}
		catch (Exception ex) {
			return null;
		}
	}

	private static Builder getBuilder(String clientId, Provider provider) {
		return null;
	}

}
