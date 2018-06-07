/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.servlet;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.SecurityConfigurer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a Spring Security in-memory
 * {@link AuthenticationManager}. Adds an {@link InMemoryUserDetailsManager} with a
 * default user and generated password. This can be disabled by providing a bean of type
 * {@link AuthenticationManager}, {@link AuthenticationProvider} or
 * {@link UserDetailsService}.
 *
 * @author Dave Syer
 * @author Rob Winch
 * @author Madhura Bhave
 */
@Configuration
@ConditionalOnClass(AuthenticationManager.class)
@ConditionalOnBean(ObjectPostProcessor.class)
@ConditionalOnMissingBean({ AuthenticationManager.class, AuthenticationProvider.class,
		UserDetailsService.class })
public class UserDetailsServiceAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(type = "org.springframework.security.oauth2.client.registration.ClientRegistrationRepository")
	public PropertiesUserDetailsManager propertiesUserDetailsManager(
			SecurityProperties properties,
			ObjectProvider<PasswordEncoder> passwordEncoder) {
		return new PropertiesUserDetailsManager(properties.getUser(), passwordEncoder);
	}

	@Bean
	public PropertiesUserDetailsManagerLoggingInitializer propertiesUserDetailsManagerLoggingInitializer(
			ApplicationContext applicationContext) {
		return new PropertiesUserDetailsManagerLoggingInitializer(applicationContext);
	}

	/**
	 * {@link SecurityConfigurer} to trigger early logging from the
	 * {@link PropertiesUserDetailsManager} if the user hasn't configured their own auth.
	 */
	@Order(PropertiesUserDetailsManagerLoggingInitializer.ORDER)
	static class PropertiesUserDetailsManagerLoggingInitializer
			extends GlobalAuthenticationConfigurerAdapter {

		static final int BEAN_MANAGER_CONFIGURER_ORDER = Ordered.LOWEST_PRECEDENCE - 5000;

		static final int ORDER = BEAN_MANAGER_CONFIGURER_ORDER - 1;

		private final ApplicationContext applicationContext;

		PropertiesUserDetailsManagerLoggingInitializer(
				ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Override
		public void init(AuthenticationManagerBuilder auth) throws Exception {
			auth.apply(new Configurer());
		}

		class Configurer extends GlobalAuthenticationConfigurerAdapter {

			@Override
			public void configure(AuthenticationManagerBuilder auth) throws Exception {
				if (!auth.isConfigured()) {
					PropertiesUserDetailsManagerLoggingInitializer.this.applicationContext
							.getBeansOfType(PropertiesUserDetailsManager.class).values()
							.forEach(PropertiesUserDetailsManager::logGeneratedPassword);
				}
			}

		}

	}

	/**
	 * {@link InMemoryUserDetailsManager} backed by {@link SecurityProperties}.
	 */
	static class PropertiesUserDetailsManager extends InMemoryUserDetailsManager {

		private static final String NOOP_PASSWORD_PREFIX = "{noop}";

		private static final Pattern PASSWORD_ALGORITHM_PATTERN = Pattern
				.compile("^\\{.+}.*$");

		private static final Log logger = LogFactory
				.getLog(PropertiesUserDetailsManager.class);

		private final SecurityProperties.User userProperties;

		private final AtomicBoolean loggedGeneratedPassword = new AtomicBoolean();

		PropertiesUserDetailsManager(SecurityProperties.User userProperties,
				ObjectProvider<PasswordEncoder> passwordEncoder) {
			this.userProperties = userProperties;
			String name = userProperties.getName();
			String password = getOrDeducePassword(userProperties,
					passwordEncoder.getIfAvailable());
			String[] roles = StringUtils.toStringArray(userProperties.getRoles());
			createUser(User.withUsername(name).password(password).roles(roles).build());
		}

		private String getOrDeducePassword(SecurityProperties.User user,
				PasswordEncoder encoder) {
			String password = user.getPassword();
			if (encoder != null
					|| PASSWORD_ALGORITHM_PATTERN.matcher(password).matches()) {
				return password;
			}
			return NOOP_PASSWORD_PREFIX + password;
		}

		@Override
		public UserDetails loadUserByUsername(String username)
				throws UsernameNotFoundException {
			logGeneratedPassword();
			return super.loadUserByUsername(username);
		}

		public void logGeneratedPassword() {
			if (logger.isInfoEnabled() && this.userProperties.isPasswordGenerated()
					&& this.loggedGeneratedPassword.compareAndSet(false, true)) {
				logger.info(String.format("%n%nUsing generated security password: %s%n",
						this.userProperties.getPassword()));
			}
		}

	}

}
