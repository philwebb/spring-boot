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

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.saml2.credentials.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationFilter;
import org.springframework.util.Assert;

/**
 * {@link Configuration @Configuration} used to map {@link SAML2RelyingPartyProperties} to
 * relying party registrations in a {@link RelyingPartyRegistrationRepository}.
 *
 * @author Madhura Bhave
 */
@Configuration(proxyBeanMethods = false)
@Conditional(RegistrationsConfiguredCondition.class)
@ConditionalOnMissingBean(RelyingPartyRegistrationRepository.class)
class SAML2RelyingPartyRegistrationConfiguration {

	@Bean
	RelyingPartyRegistrationRepository relyingPartyRegistrationRepository(SAML2RelyingPartyProperties properties) {
		List<RelyingPartyRegistration> registrations = getRegistrations(properties.getRegistration());
		return new InMemoryRelyingPartyRegistrationRepository(registrations);
	}

	private List<RelyingPartyRegistration> getRegistrations(
			Map<String, SAML2RelyingPartyProperties.RelyingParty> registration) {
		List<RelyingPartyRegistration> registrations = new ArrayList<>();
		registration.forEach((key, value) -> {
			SAML2RelyingPartyProperties.Identityprovider identityProvider = value.getIdentityprovider();
			List<Saml2X509Credential> credentials = new ArrayList<>();
			addSigningCredentials(value.getSigningcredentials(), credentials);
			addVerificationCredentials(identityProvider, credentials);
			addRegistration(registrations, key, identityProvider, credentials);
		});
		return registrations;
	}

	private void addRegistration(List<RelyingPartyRegistration> registrations, String key,
			SAML2RelyingPartyProperties.Identityprovider identityProvider, List<Saml2X509Credential> credentials) {
		String acsUrlTemplate = "{baseUrl}" + Saml2WebSsoAuthenticationFilter.DEFAULT_FILTER_PROCESSES_URI;
		registrations.add(
				RelyingPartyRegistration.withRegistrationId(key).assertionConsumerServiceUrlTemplate(acsUrlTemplate)
						.idpWebSsoUrl(identityProvider.getSsoUrl()).remoteIdpEntityId(identityProvider.getEntityId())
						.credentials((saml2X509Credentials) -> saml2X509Credentials.addAll(credentials)).build());
	}

	private void addVerificationCredentials(SAML2RelyingPartyProperties.Identityprovider identityProvider,
			List<Saml2X509Credential> credentials) {
		identityProvider.getVerificationcredentials()
				.forEach((c) -> credentials.add(new Saml2X509Credential(readCertificate(c.getCertificateLocation()),
						Saml2X509Credential.Saml2X509CredentialType.ENCRYPTION,
						Saml2X509Credential.Saml2X509CredentialType.VERIFICATION)));
	}

	private void addSigningCredentials(
			List<SAML2RelyingPartyProperties.RelyingParty.Signingcredential> signingcredentials,
			List<Saml2X509Credential> credentials) {
		signingcredentials
				.forEach((c) -> credentials.add(new Saml2X509Credential(readPrivateKey(c.getPrivateKeyLocation()),
						readCertificate(c.getCertificateLocation()),
						Saml2X509Credential.Saml2X509CredentialType.SIGNING,
						Saml2X509Credential.Saml2X509CredentialType.DECRYPTION)));
	}

	private RSAPrivateKey readPrivateKey(Resource privateKeyLocation) {
		Assert.notNull(privateKeyLocation, "PrivateKeyLocation must not be null");
		Assert.isTrue(privateKeyLocation.exists(), "Private key location must exist.");
		try (InputStream inputStream = privateKeyLocation.getInputStream()) {
			return RsaKeyConverters.pkcs8().convert(inputStream);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	private X509Certificate readCertificate(Resource certificateLocation) {
		Assert.notNull(certificateLocation, "Certificate location must not be null");
		Assert.isTrue(certificateLocation.exists(), "Certificate location must exist.");
		try (InputStream inputStream = certificateLocation.getInputStream()) {
			CertificateFactory factory = CertificateFactory.getInstance("X.509");
			return (X509Certificate) factory.generateCertificate(inputStream);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException(ex);
		}
	}

}
