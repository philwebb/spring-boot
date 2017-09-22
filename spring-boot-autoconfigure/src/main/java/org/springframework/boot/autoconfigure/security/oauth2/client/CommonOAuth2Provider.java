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

import com.google.common.base.Objects;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.Builder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.Assert;

/**
 * Common OAuth2 Providers that can be used to create
 * {@link org.springframework.security.oauth2.client.registration.ClientRegistration.Builder
 * builders} pre-configured with sensible defaults.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public enum CommonOAuth2Provider {

	GOOGLE("https://www.googleapis.com/oauth2/v3/userinfo", null) {

		@Override
		public Builder getBuilder(String clientId) {
			ClientRegistration.Builder builder = getBuilder(clientId,
					ClientAuthenticationMethod.BASIC, DEFAULT_REDIRECT_URL);
			builder.scope("openid", "profile", "email", "address", "phone");
			builder.authorizationUri("https://accounts.google.com/o/oauth2/v2/auth");
			builder.tokenUri("https://www.googleapis.com/oauth2/v4/token");
			builder.jwkSetUri("https://www.googleapis.com/oauth2/v3/certs");
			builder.clientName("Google");
			builder.clientAlias("google");
			return builder;
		}

	},

	GITHUB("https://api.github.com/user", "name") {

		@Override
		public Builder getBuilder(String clientId) {
			ClientRegistration.Builder builder = getBuilder(clientId,
					ClientAuthenticationMethod.BASIC, DEFAULT_REDIRECT_URL);
			builder.scope("user");
			builder.authorizationUri("https://github.com/login/oauth/authorize");
			builder.tokenUri("https://github.com/login/oauth/access_token");
			builder.clientName("GitHub");
			builder.clientAlias("github");
			return builder;
		}

	},

	FACEBOOK("https://graph.facebook.com/me", "name") {

		@Override
		public Builder getBuilder(String clientId) {
			ClientRegistration.Builder builder = getBuilder(clientId,
					ClientAuthenticationMethod.POST, DEFAULT_REDIRECT_URL);
			builder.scope("public_profile", "email");
			builder.authorizationUri("https://www.facebook.com/v2.8/dialog/oauth");
			builder.tokenUri("https://graph.facebook.com/v2.8/oauth/access_token");
			builder.clientName("Facebook");
			builder.clientAlias("facebook");
			return builder;
		}

	},

	OKTA(null, null) {

		@Override
		public Builder getBuilder(String clientId) {
			ClientRegistration.Builder builder = getBuilder(clientId,
					ClientAuthenticationMethod.BASIC, DEFAULT_REDIRECT_URL);
			builder.scope("openid", "profile", "email", "address", "phone");
			builder.clientName("Okta");
			builder.clientAlias("okta");
			return builder;
		}

	};

	private static final String DEFAULT_REDIRECT_URL = "{scheme}://{serverName}:{serverPort}{contextPath}/oauth2/authorize/code/{clientAlias}";

	private final String userInfoUri;

	private final String userNameAttribute;

	private CommonOAuth2Provider(String userInfoUri, String userNameAttribute) {
		this.userInfoUri = userInfoUri;
		this.userNameAttribute = userNameAttribute;
	}

	protected final String getUserInfoUri() {
		return this.userInfoUri;
	}

	protected final ClientRegistration.Builder getBuilder(String clientId,
			ClientAuthenticationMethod method, String redirectUri) {
		ClientRegistration.Builder builder = new ClientRegistration.Builder(clientId);
		builder.clientAuthenticationMethod(method);
		builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
		builder.redirectUri(redirectUri);
		if (getUserInfoUri() != null) {
			builder.userInfoUri(getUserInfoUri());
		}
		return builder;
	}

	/**
	 * Create a new
	 * {@link org.springframework.security.oauth2.client.registration.ClientRegistration.Builder
	 * ClientRegistration.Builder} pre-initialized with the provider settings.
	 * @param clientId the client-id used with the new builder
	 * @return a builder instance
	 */
	public abstract ClientRegistration.Builder getBuilder(String clientId);

	/**
	 * Return the attribute that can be used to extract the username or {@code null} if
	 * not attribute is defined.
	 * @return the username attribute name or {@code null}
	 */
	public final String getUserNameAttribute() {
		return this.userNameAttribute;
	}

	/**
	 * Find a {@link CommonOAuth2Provider} that has the given {@code userInfoUri}.
	 * @param userInfoUri the user info URI
	 * @return the matching {@link CommonOAuth2Provider} or {@code null}
	 */
	public static CommonOAuth2Provider forUserInfoUri(String userInfoUri) {
		Assert.notNull(userInfoUri, "UserInfoUri must not be null");
		for (CommonOAuth2Provider candidate : values()) {
			if (Objects.equal(candidate.getUserInfoUri(), userInfoUri)) {
				return candidate;
			}
		}
		return null;
	}

}
