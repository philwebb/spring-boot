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

import javax.servlet.Servlet;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.bootstrap.actuate.Endpoints;
import org.springframework.bootstrap.actuate.properties.ManagementServerProperties;
import org.springframework.bootstrap.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.bootstrap.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.bootstrap.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.bootstrap.context.annotation.ConditionalOnClass;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.bootstrap.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.bootstrap.properties.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to create and configure a management
 * server to handle actuator requests. If the {@link ManagementServerProperties} specifies
 * a different port to {@link ServerProperties} a new child context is created, otherwise
 * it is assumed that management requests will be handled via an already registered
 * {@link DispatcherServlet}.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
@Configuration
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class })
@Import({ PropertyPlaceholderAutoConfiguration.class,
		EmbeddedServletContainerAutoConfiguration.class, WebMvcAutoConfiguration.class })
public class ManagementServerAutoConfiguration implements ApplicationContextAware,
		ApplicationListener<ContextRefreshedEvent> {

	private static final Integer DISABLED_PORT = Integer.valueOf(0);

	private ApplicationContext applicationContext;

	@Autowired(required = false)
	private ServerProperties serverProperties = new ServerProperties();

	@Autowired(required = false)
	private ManagementServerProperties managementServerProperties = new ManagementServerProperties();

	@Bean
	public Endpoints actuatorEndpoints() {
		// FIXME move out then @Autowire in
		return new Endpoints();
	}

	@Bean
	public EndpointsHandlerMapping actuatorEndpointsHandlerMapping() {
		return new EndpointsHandlerMapping(actuatorEndpoints());
	}

	@Bean
	public EndpointHandlerAdapter actuatorEndpointHandlerAdapter() {
		return new EndpointHandlerAdapter();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (event.getApplicationContext() == this.applicationContext) {
			if (isChildActuatorContextRequired()) {
				createChildActuatorContext();
			}
		}
	}

	private boolean isChildActuatorContextRequired() {
		if (DISABLED_PORT.equals(this.managementServerProperties.getPort())) {
			return false;
		}
		return this.serverProperties.getPort() != this.managementServerProperties
				.getPort();
		// FIXME check the inet address etc are not different since they will be ignored
	}

	private void createChildActuatorContext() {
		AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();
		context.setParent(this.applicationContext);
		context.setId("actuator");
		// Register using the class name as a String so not to load annotations
		context.registerBeanDefinition(
				"managementServerChildContextConfiguration",
				new RootBeanDefinition(ManagementServerChildContextConfiguration.class
						.getName()));
		context.refresh();
	}
}
