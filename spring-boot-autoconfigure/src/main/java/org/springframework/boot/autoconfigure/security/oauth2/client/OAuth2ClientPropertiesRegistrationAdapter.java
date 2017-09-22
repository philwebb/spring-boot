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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.Provider;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.Registration;
import org.springframework.boot.context.properties.bind.convert.BinderConversionService;
import org.springframework.core.convert.ConversionException;
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

	private static ClientRegistration getClientRegistration(Registration properties,
			Map<String, Provider> providers) {
		Builder builder = getBuilder(properties.getClientId(), properties.getProvider(),
				providers);
		copyIfNotNull(properties::getClientSecret, builder::clientSecret);
		copyIfNotNull(() -> properties.getClientAuthenticationMethod(),
				builder::clientAuthenticationMethod,
				ClientAuthenticationMethod::getMethod);
		copyIfNotNull(() -> properties.getAuthorizationGrantType(),
				builder::authorizationGrantType, AuthorizationGrantType::getType);
		copyIfNotNull(properties::getRedirectUri, builder::redirectUri);
		copyIfNotNull(properties::getScope, builder::scope,
				(scope) -> scope.toArray(new String[scope.size()]));
		copyIfNotNull(properties::getClientName, builder::clientName);
		copyIfNotNull(properties::getClientAlias, builder::clientAlias);
		return builder.build();
	}

	private static Builder getBuilder(String clientId, String providerId,
			Map<String, Provider> providers) {
		if (providers.containsKey(providerId)) {
			return getBuilder(clientId, providers.get(providerId));
		}
		return getCommonProvider(providerId).getBuilder(clientId);
	}

	private static Builder getBuilder(String clientId, Provider provider) {
		Builder builder = new Builder(clientId);
		copyIfNotNull(provider::getAuthorizationUri, builder::authorizationUri);
		copyIfNotNull(provider::getTokenUri, builder::tokenUri);
		copyIfNotNull(provider::getUserInfoUri, builder::userInfoUri);
		copyIfNotNull(provider::getJwkSetUri, builder::jwkSetUri);
		return builder;
	}

	private static CommonOAuth2Provider getCommonProvider(String providerId) {
		try {
			return new BinderConversionService(null).convert(providerId,
					CommonOAuth2Provider.class);
		}
		catch (ConversionException ex) {
			throw new IllegalStateException("Unknown provider ID '" + providerId + "'",
					ex);
		}
	}

	private static <T> void copyIfNotNull(Supplier<T> supplier, Consumer<T> consumer) {
		copyIfNotNull(supplier, consumer, Function.identity());
	}

	private static <S, C> void copyIfNotNull(Supplier<S> supplier, Consumer<C> consumer,
			Function<S, C> converter) {
		S value = supplier.get();
		if (value != null) {
			consumer.accept(converter.apply(value));
		}
	}

}
