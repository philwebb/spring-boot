package org.springframework.boot.autoconfigure.security.oauth2.client;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails;

/**
 * Strategy interface that can be used with OAuth 2.0 auto-configuration to provide the
 * name of the attribute that should be used to obtain the username from
 * {@link ProviderDetails#getUserInfoUri() user info}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 */
@FunctionalInterface
public interface OAuth2UserNameAttributeNameProvider {

	/**
	 * Return the name of the attribute that contains the username.
	 * @param providerDetails the source provider details
	 * @return an attribute name or {@code null}
	 */
	String getUserNameAttributeName(ClientRegistration.ProviderDetails providerDetails);

}
