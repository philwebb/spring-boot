package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * Auto-configuration for OAuth client support.
 *
 * @author Madhura Bhave
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ EnableWebSecurity.class, ClientRegistration.class })
@EnableConfigurationProperties(OAuth2ClientProperties.class)
@Import(OAuth2WebSecurityConfiguration.class)
@AutoConfigureBefore(SecurityAutoConfiguration.class)
public class OAuth2ClientAutoConfiguration {

	@Configuration
	@Conditional(ClientsConfiguredCondition.class)
	@ConditionalOnMissingBean(ClientRegistrationRepository.class)
	static class ClientRegistrationRepositoryConfiguration {
		@Bean
		public ClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties oauth2ClientProperties) {
			List<ClientRegistration> clientRegistrations = new ArrayList<>();

			//FIXME adapt the client properties to registrations
			return new InMemoryClientRegistrationRepository(clientRegistrations);
		}

	}

	static class ClientsConfiguredCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("OAuth2 Clients Configured Condition");
			Map<String, OAuth2ClientProperties.Registration> clients = this.getConfiguredClients(context.getEnvironment());
			if (!clients.isEmpty()) {
				return ConditionOutcome.match(message.foundExactly("OAuth2 Client(s) -> "
						+ clients.values().stream().map(OAuth2ClientProperties.Registration::getClientId).collect(Collectors.joining(", "))));
			}
			return ConditionOutcome.noMatch(message.notAvailable("OAuth2 Client(s)"));
		}

		private Map<String, OAuth2ClientProperties.Registration> getConfiguredClients(Environment environment) {
			return Binder.get(environment)
					.bind("spring.security.oauth.client.registration",
							Bindable.mapOf(String.class, OAuth2ClientProperties.Registration.class))
					.orElse(new HashMap<>());
		}
	}


}
