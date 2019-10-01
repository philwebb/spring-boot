/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.saml2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * SAML2 relying party properties.
 *
 * @author Madhura Bhave
 * @since 2.2.0
 */
@ConfigurationProperties("spring.security.saml2.relyingparty")
public class SAML2RelyingPartyProperties {

	/**
	 * SAML2 relying party registrations.
	 */
	private Map<String, RelyingParty> registration = new LinkedHashMap<>();

	public Map<String, RelyingParty> getRegistration() {
		return this.registration;
	}

	/**
	 * Represents a SAML Relying Party.
	 */
	public static class RelyingParty {

		/**
		 * Credentials used for signing the SAML authentication request.
		 */
		private List<Signingcredential> signingcredentials = new ArrayList<>();

		/**
		 * Remote SAML Identity Provider.
		 */
		private Identityprovider identityprovider = new Identityprovider();

		List<Signingcredential> getSigningcredentials() {
			return this.signingcredentials;
		}

		Identityprovider getIdentityprovider() {
			return this.identityprovider;
		}

		public static class Signingcredential {

			/**
			 * Private key used for signing or decrypting.
			 */
			private Resource privateKeyLocation;

			/**
			 * Relying Party X509Certificate shared with the identity provider.
			 */
			private Resource certificateLocation;

			public Resource getPrivateKeyLocation() {
				return this.privateKeyLocation;
			}

			public void setPrivateKeyLocation(Resource privateKey) {
				this.privateKeyLocation = privateKey;
			}

			public Resource getCertificateLocation() {
				return this.certificateLocation;
			}

			public void setCertificateLocation(Resource certificate) {
				this.certificateLocation = certificate;
			}

		}

	}

	/**
	 * Represents a remote Identity Provider.
	 */
	public static class Identityprovider {

		/**
		 * Unique identifier for the identity provider.
		 */
		private String entityId;

		/**
		 * Remote endpoint to send authentication requests to.
		 */
		private String ssoUrl;

		/**
		 * Locations of X.509 certificates used for verification of incoming SAML
		 * messages.
		 */
		private List<Verificationcredential> verificationcredentials = new ArrayList<>();

		public String getEntityId() {
			return this.entityId;
		}

		public void setEntityId(String entityId) {
			this.entityId = entityId;
		}

		public String getSsoUrl() {
			return this.ssoUrl;
		}

		public void setSsoUrl(String ssoUrl) {
			this.ssoUrl = ssoUrl;
		}

		List<Verificationcredential> getVerificationcredentials() {
			return this.verificationcredentials;
		}

		public static class Verificationcredential {

			/**
			 * X.509 certificate used for verification of incoming SAML messages.
			 */
			private Resource certificate;

			public Resource getCertificateLocation() {
				return this.certificate;
			}

			public void setCertificateLocation(Resource certificate) {
				this.certificate = certificate;
			}

		}

	}

}
