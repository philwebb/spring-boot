package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Madhura Bhave
 */
@ConfigurationProperties(prefix = "spring.security.oauth2.client")
public class OAuth2ClientProperties {

	private Map<String, Registration> registration;

	private Map<String, Provider> provider;

	private class Registration {

		private String provider;

		private String clientId;

		private String clientSecret;

		private ClientAuthenticationMethod clientAuthenticationMethod;

		private AuthorizationGrantType authorizationGrantType;

		private String redirectUri;

		private Set<String> scope;

		private String clientName;

		private String clientAlias;

		public String getClientId() {
			return clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public String getClientSecret() {
			return clientSecret;
		}

		public void setClientSecret(String clientSecret) {
			this.clientSecret = clientSecret;
		}

		public ClientAuthenticationMethod getClientAuthenticationMethod() {
			return clientAuthenticationMethod;
		}

		public void setClientAuthenticationMethod(ClientAuthenticationMethod clientAuthenticationMethod) {
			this.clientAuthenticationMethod = clientAuthenticationMethod;
		}

		public AuthorizationGrantType getAuthorizationGrantType() {
			return authorizationGrantType;
		}

		public void setAuthorizationGrantType(AuthorizationGrantType authorizationGrantType) {
			this.authorizationGrantType = authorizationGrantType;
		}

		public String getRedirectUri() {
			return redirectUri;
		}

		public void setRedirectUri(String redirectUri) {
			this.redirectUri = redirectUri;
		}

		public Set<String> getScope() {
			return scope;
		}

		public void setScope(Set<String> scope) {
			this.scope = scope;
		}

		public String getProvider() {
			return provider;
		}

		public void setProvider(String provider) {
			this.provider = provider;
		}

		public String getClientName() {
			return clientName;
		}

		public void setClientName(String clientName) {
			this.clientName = clientName;
		}

		public String getClientAlias() {
			return clientAlias;
		}

		public void setClientAlias(String clientAlias) {
			this.clientAlias = clientAlias;
		}
	}

	private class Provider {

		private String authorizationUri;

		private String tokenUri;

		private String userInfoUri;

		private String jwkSetUri;

		private ClientAuthenticationMethod authenticationMethod;

		public String getAuthorizationUri() {
			return authorizationUri;
		}

		public void setAuthorizationUri(String authorizationUri) {
			this.authorizationUri = authorizationUri;
		}

		public String getTokenUri() {
			return tokenUri;
		}

		public void setTokenUri(String tokenUri) {
			this.tokenUri = tokenUri;
		}

		public String getUserInfoUri() {
			return userInfoUri;
		}

		public void setUserInfoUri(String userInfoUri) {
			this.userInfoUri = userInfoUri;
		}

		public String getJwkSetUri() {
			return jwkSetUri;
		}

		public void setJwkSetUri(String jwkSetUri) {
			this.jwkSetUri = jwkSetUri;
		}

		public ClientAuthenticationMethod getAuthenticationMethod() {
			return authenticationMethod;
		}

		public void setAuthenticationMethod(ClientAuthenticationMethod authenticationMethod) {
			this.authenticationMethod = authenticationMethod;
		}
	}
}
