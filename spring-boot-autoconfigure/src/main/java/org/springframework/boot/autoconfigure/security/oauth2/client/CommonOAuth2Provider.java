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

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.Builder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * Common OAuth2 Providers that can be used to create
 * {@link org.springframework.security.oauth2.client.registration.ClientRegistration.Builder
 * builders} pre-configured with sensible defaults.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public enum CommonOAuth2Provider {

	// FIXME move this to Spring Security OAuth

	GOOGLE {

		@Override
		public Builder getBuilder(String clientId) {
			ClientRegistration.Builder builder = getBuilder(clientId,
					ClientAuthenticationMethod.BASIC, DEFAULT_REDIRECT_URL);
			builder.scope("openid", "profile", "email", "address", "phone");
			builder.authorizationUri("https://accounts.google.com/o/oauth2/v2/auth");
			builder.tokenUri("https://www.googleapis.com/oauth2/v4/token");
			builder.userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo");
			builder.jwkSetUri("https://www.googleapis.com/oauth2/v3/certs");
			builder.clientName("Google");
			builder.clientAlias("google");
			return builder;
		}

	},
	GITHUB {

		@Override
		public Builder getBuilder(String clientId) {
			ClientRegistration.Builder builder = getBuilder(clientId,
					ClientAuthenticationMethod.BASIC, DEFAULT_REDIRECT_URL);
			builder.scope("user");
			builder.authorizationUri("https://github.com/login/oauth/authorize");
			builder.tokenUri("https://github.com/login/oauth/access_token");
			builder.userInfoUri("https://api.github.com/user");
			builder.clientName("GitHub");
			builder.clientAlias("github");
			return builder;
		}

		@Override
		public String getUserNameAttributeName() {
			return "name";
		}

	},

	FACEBOOK {

		@Override
		public Builder getBuilder(String clientId) {
			ClientRegistration.Builder builder = getBuilder(clientId,
					ClientAuthenticationMethod.POST, DEFAULT_REDIRECT_URL);
			builder.scope("public_profile", "email");
			builder.authorizationUri("https://www.facebook.com/v2.8/dialog/oauth");
			builder.tokenUri("https://graph.facebook.com/v2.8/oauth/access_token");
			builder.userInfoUri("https://graph.facebook.com/me");
			builder.clientName("Facebook");
			builder.clientAlias("facebook");
			return builder;
		}

		@Override
		public String getUserNameAttributeName() {
			return "name";
		}

	},

	OKTA {

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

	protected final ClientRegistration.Builder getBuilder(String clientId,
			ClientAuthenticationMethod method, String redirectUri) {
		ClientRegistration.Builder builder = new ClientRegistration.Builder(clientId);
		builder.clientAuthenticationMethod(method);
		builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
		builder.redirectUri(redirectUri);
		return builder;
	}

	public abstract ClientRegistration.Builder getBuilder(String clientId);

	public String getUserNameAttributeName() {
		return null;
	}

}
