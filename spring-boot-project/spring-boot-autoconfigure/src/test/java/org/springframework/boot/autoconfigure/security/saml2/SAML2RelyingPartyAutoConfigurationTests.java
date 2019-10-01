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

import java.util.List;

import javax.servlet.Filter;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.BeanIds;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SAML2RelyingPartyAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
public class SAML2RelyingPartyAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(SAML2RelyingPartyAutoConfiguration.class, SecurityAutoConfiguration.class));

	private static final String REGISTRATION_PREFIX = "spring.security.saml2.relyingparty.registration";

	@Test
	void autoConfigurationShouldBeConditionalOnRelyingPartyRegistrationRepositoryClass() {
		this.contextRunner.withPropertyValues(getPropertyValues()).withClassLoader(new FilteredClassLoader(
				"org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository"))
				.run((context) -> assertThat(context).doesNotHaveBean(RelyingPartyRegistrationRepository.class));
	}

	@Test
	void autoConfigurationShouldBeConditionalOnServletWebApplication() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(SAML2RelyingPartyAutoConfiguration.class))
				.withPropertyValues(getPropertyValues())
				.run((context) -> assertThat(context).doesNotHaveBean(RelyingPartyRegistrationRepository.class));
	}

	@Test
	void relyingPartyRegistrationRepositoryBeanShouldNotBeCreatedWhenPropertiesAbsent() {
		this.contextRunner
				.run((context) -> assertThat(context).doesNotHaveBean(RelyingPartyRegistrationRepository.class));
	}

	@Test
	void relyingPartyRegistrationRepositoryBeanShouldBeCreatedWhenPropertiesPresent() {
		this.contextRunner.withPropertyValues(getPropertyValues()).run((context) -> {
			RelyingPartyRegistrationRepository repository = context.getBean(RelyingPartyRegistrationRepository.class);
			RelyingPartyRegistration registration = repository.findByRegistrationId("foo");
			assertThat(registration.getIdpWebSsoUrl())
					.isEqualTo("https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/SSOService.php");
			assertThat(registration.getRemoteIdpEntityId())
					.isEqualTo("https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/metadata.php");
			assertThat(registration.getAssertionConsumerServiceUrlTemplate())
					.isEqualTo("{baseUrl}" + Saml2WebSsoAuthenticationFilter.DEFAULT_FILTER_PROCESSES_URI);
			assertThat(registration.getSigningCredentials()).isNotNull();
			assertThat(registration.getVerificationCredentials()).isNotNull();
		});
	}

	@Test
	void relyingPartyRegistrationRepositoryShouldBeConditionalOnMissingBean() {
		this.contextRunner.withPropertyValues(getPropertyValues())
				.withUserConfiguration(RegistrationRepositoryConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(RelyingPartyRegistrationRepository.class);
					assertThat(context).hasBean("testRegistrationRepository");
				});
	}

	@Test
	void samlLoginShouldBeConfigured() {
		this.contextRunner.withPropertyValues(getPropertyValues())
				.run((context) -> assertThat(hasFilter(context, Saml2WebSsoAuthenticationFilter.class)).isTrue());
	}

	private String[] getPropertyValues() {
		return new String[] {
				REGISTRATION_PREFIX
						+ ".foo.signingcredentials[0].private-key-location=classpath:saml/private-key-location",
				REGISTRATION_PREFIX
						+ ".foo.signingcredentials[0].certificate-location=classpath:saml/certificate-location",
				REGISTRATION_PREFIX
						+ ".foo.identityprovider.sso-url=https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/SSOService.php",
				REGISTRATION_PREFIX
						+ ".foo.identityprovider.entity-id=https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/metadata.php",
				REGISTRATION_PREFIX
						+ ".foo.identityprovider.verificationcredentials[0].certificate-location=classpath:saml/certificate-location" };
	}

	private boolean hasFilter(AssertableWebApplicationContext context, Class<? extends Filter> filter) {
		FilterChainProxy filterChain = (FilterChainProxy) context.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN);
		List<SecurityFilterChain> filterChains = filterChain.getFilterChains();
		List<Filter> filters = filterChains.get(0).getFilters();
		return filters.stream().anyMatch(filter::isInstance);
	}

	@Configuration(proxyBeanMethods = false)
	static class RegistrationRepositoryConfiguration {

		@Bean
		RelyingPartyRegistrationRepository testRegistrationRepository() {
			return mock(RelyingPartyRegistrationRepository.class);
		}

	}

}
