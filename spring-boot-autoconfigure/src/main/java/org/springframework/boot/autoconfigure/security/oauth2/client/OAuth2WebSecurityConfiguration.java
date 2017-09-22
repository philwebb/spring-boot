package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.net.URI;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.StringUtils;

/**
 * {@link WebSecurityConfigurerAdapter} to add OAuth client support.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 * @see OAuth2UserNameAttributeNameProvider
 */
@Configuration
@ConditionalOnMissingBean(WebSecurityConfigurerAdapter.class)
@ConditionalOnBean(ClientRegistrationRepository.class)
public class OAuth2WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

	private final ClientRegistrationRepository clientRegistrationRepository;

	private final OAuth2UserNameAttributeNameProvider userNameAttributeNameProvider;

	public OAuth2WebSecurityConfiguration(
			ClientRegistrationRepository clientRegistrationRepository,
			ObjectProvider<OAuth2UserNameAttributeNameProvider> userNameAttributeNameProvider) {
		this.clientRegistrationRepository = clientRegistrationRepository;
		this.userNameAttributeNameProvider = userNameAttributeNameProvider
				.getIfAvailable();
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		// @formatter:off
		http
			.authorizeRequests()
				.anyRequest()
					.authenticated().and()
			.oauth2Login()
				.clients(this.clientRegistrationRepository);
		// @formatter:on
		this.registerUserNameAttributeNames(http.oauth2Login());
	}

	private void registerUserNameAttributeNames(
			OAuth2LoginConfigurer<HttpSecurity> configurer) throws Exception {
		if (this.userNameAttributeNameProvider == null) {
			return;
		}
		this.clientRegistrationRepository.getRegistrations().stream()
				.map(ClientRegistration::getProviderDetails)
				.filter((providerDetails) -> StringUtils
						.hasText(providerDetails.getUserInfoUri()))
				.forEach((providerDetails) -> lookupUserNameAttributeName(configurer,
						providerDetails));
	}

	private void lookupUserNameAttributeName(
			OAuth2LoginConfigurer<HttpSecurity> configurer,
			ProviderDetails providerDetails) {
		String name = this.userNameAttributeNameProvider
				.getUserNameAttributeName(providerDetails);
		if (name != null) {
			configurer.userInfoEndpoint().userNameAttributeName(name,
					URI.create(providerDetails.getAuthorizationUri()));
		}
	}

}
