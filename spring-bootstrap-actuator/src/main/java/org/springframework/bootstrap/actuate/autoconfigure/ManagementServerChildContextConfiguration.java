/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.bootstrap.actuate.autoconfigure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.bootstrap.actuate.autoconfigure.ManagementServerChildContextConfiguration.SpringMvcAliasRegistrar;
import org.springframework.bootstrap.actuate.properties.ManagementServerProperties;
import org.springframework.bootstrap.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.bootstrap.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.bootstrap.context.embedded.ConfigurableEmbeddedServletContainerFactory;
import org.springframework.bootstrap.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.bootstrap.properties.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Configuration for the child actuator embedded servlet container,
 * {@link DispatcherServlet} and lite Spring MVC configuration.
 * 
 * @see ManagementServerAutoConfiguration
 */
@Configuration
@Import({ PropertyPlaceholderAutoConfiguration.class,
		EmbeddedServletContainerAutoConfiguration.class, SpringMvcAliasRegistrar.class })
public class ManagementServerChildContextConfiguration implements
		EmbeddedServletContainerCustomizer {

	@Autowired
	private ServerProperties serverProperties;

	@Autowired
	private ManagementServerProperties managementServerProperties;

	@Override
	public void customize(ConfigurableEmbeddedServletContainerFactory factory) {
		factory.setPort(this.managementServerProperties.getPort() == null ? this.serverProperties
				.getPort() : this.managementServerProperties.getPort());
		factory.setAddress(this.managementServerProperties.getAddress());
		factory.setContextPath(this.managementServerProperties.getContextPath());
	}

	@Bean
	public DispatcherServlet actuatorDispatcherServlet() {
		DispatcherServlet dispatcherServlet = new DispatcherServlet();

		// Ensure the parent configuration does not leak down to us
		dispatcherServlet.setDetectAllHandlerAdapters(false);
		dispatcherServlet.setDetectAllHandlerExceptionResolvers(false);
		dispatcherServlet.setDetectAllHandlerMappings(false);
		dispatcherServlet.setDetectAllViewResolvers(false);

		return dispatcherServlet;
	}

	public static class SpringMvcAliasRegistrar implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			registry.registerAlias("actuatorEndpointsHandlerMapping", "handlerMapping");
			registry.registerAlias("actuatorEndpointHandlerAdapter", "handlerAdapter");
		}

	}
}
