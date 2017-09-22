package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.net.URI;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.config.oauth2.client.OAuth2ClientTemplatePropertiesLoader;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Adds a {@link WebSecurityConfigurerAdapter} for OAuth.,
 *
 * @author Madhura Bhave
 */
@Configuration
@ConditionalOnMissingBean(WebSecurityConfigurerAdapter.class)
@ConditionalOnBean(ClientRegistrationRepository.class)
public class OAuth2WebSecurityConfiguration {

	private static final String USER_NAME_ATTR_NAME_PROPERTY = "user-name-attribute-name";

	@Configuration
	protected static class OAuth2LoginConfiguration extends WebSecurityConfigurerAdapter {
		private final ClientRegistrationRepository clientRegistrationRepository;

		private final OAuth2ClientProperties oauth2ClientProperties;

		private final Map<String, Object> clientTypesPropertySource;

		protected OAuth2LoginConfiguration(
				ClientRegistrationRepository clientRegistrationRepository,
				OAuth2ClientProperties oauth2ClientProperties) {
			this.clientRegistrationRepository = clientRegistrationRepository;
			this.oauth2ClientProperties = oauth2ClientProperties;
			this.clientTypesPropertySource = OAuth2ClientTemplatePropertiesLoader.loadClientTemplates();
		}

		// @formatter:off
		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http
				.authorizeRequests()
					.anyRequest().authenticated()
					.and()
				.oauth2Login()
				.clients(this.clientRegistrationRepository);

			this.registerUserNameAttributeNames(http.oauth2Login());
		}
		// @formatter:on

		private void registerUserNameAttributeNames(OAuth2LoginConfigurer<HttpSecurity> oauth2LoginConfigurer) throws Exception {
			this.oauth2ClientProperties.getProvider().forEach((key, details) -> {
				String userInfoUriValue = details.getUserInfoUri();
				String userNameAttributeNameValue = (String) this.clientTypesPropertySource.get(
						OAuth2ClientTemplatePropertiesLoader.CLIENT_TEMPLATES_PROPERTY_PREFIX + "." +
								key.toLowerCase() + "." +
								USER_NAME_ATTR_NAME_PROPERTY);
				if (userInfoUriValue != null && userNameAttributeNameValue != null) {
					// @formatter:off
					oauth2LoginConfigurer
							.userInfoEndpoint()
							.userNameAttributeName(userNameAttributeNameValue, URI.create(userInfoUriValue));
					// @formatter:on
				}
			});
		}
	}

}
