/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.security.oauth2.client.autoconfigure;

/**
 * @author pwebb
 */

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.web.client.support.OAuth2RestClientHttpServiceGroupConfigurer;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;

@AutoConfiguration(
		afterName = { "org.springframework.boot.restclient.autoconfigure.service.HttpServiceClientAutoConfiguration",
				"org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration",
				"org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration" })
@ConditionalOnClass({ RestClientAdapter.class, ClientRegistration.class })
@ConditionalOnBean({ HttpServiceProxyRegistry.class, OAuth2AuthorizedClientManager.class })
public class OAuth2RestClientAutoConfiguration {

	@Bean
	OAuth2RestClientHttpServiceGroupConfigurer oauth2RestClientHttpServiceGroupConfigurer(
			OAuth2AuthorizedClientManager oauth2AuthorizedClientManager) {
		return OAuth2RestClientHttpServiceGroupConfigurer.from(oauth2AuthorizedClientManager);
	}

}
