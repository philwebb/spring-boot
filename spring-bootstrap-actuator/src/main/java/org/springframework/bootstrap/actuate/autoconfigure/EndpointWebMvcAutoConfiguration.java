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
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrap.actuate.endpoint.Endpoint;
import org.springframework.bootstrap.actuate.endpoint.mvc.EndpointHandlerAdapter;
import org.springframework.bootstrap.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.bootstrap.actuate.properties.ManagementServerProperties;
import org.springframework.bootstrap.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.bootstrap.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.bootstrap.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.bootstrap.context.annotation.ConditionalOnClass;
import org.springframework.bootstrap.context.annotation.ConditionalOnMissingBean;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.bootstrap.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.bootstrap.properties.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to enable Spring MVC to handle
 * {@link Endpoint} requests. If the {@link ManagementServerProperties} specifies a
 * different port to {@link ServerProperties} a new child context is created, otherwise it
 * is assumed that endpoint requests will be mapped and handled via an already registered
 * {@link DispatcherServlet}.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
@Configuration
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class })
@Import({ PropertyPlaceholderAutoConfiguration.class,
		EmbeddedServletContainerAutoConfiguration.class, WebMvcAutoConfiguration.class })
public class EndpointWebMvcAutoConfiguration implements ApplicationContextAware,
		ApplicationListener<ContextRefreshedEvent> {

	private static final Integer DISABLED_PORT = Integer.valueOf(0);

	private ApplicationContext applicationContext;

	@Autowired(required = false)
	private ServerProperties serverProperties = new ServerProperties();

	@Autowired(required = false)
	private ManagementServerProperties managementServerProperties = new ManagementServerProperties();

	@Bean
	@ConditionalOnMissingBean(EndpointHandlerMapping.class)
	@Conditional(EndpointCondition.class)
	public EndpointHandlerMapping endpointHandlerMapping() {
		return new EndpointHandlerMapping();
	}

	@Bean
	@ConditionalOnMissingBean(EndpointHandlerAdapter.class)
	@Conditional(EndpointCondition.class)
	public EndpointHandlerAdapter endpointHandlerAdapter() {
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
			if (ManagementServerPort.get(this.applicationContext) == ManagementServerPort.DIFFERENT) {
				createChildManagementContext();
			}
		}
	}

	private void createChildManagementContext() {

		final AnnotationConfigEmbeddedWebApplicationContext childContext = new AnnotationConfigEmbeddedWebApplicationContext();
		childContext.setParent(this.applicationContext);
		childContext.setId(this.applicationContext.getId() + ":management");

		// Register the ManagementServerChildContextConfiguration first followed
		// by various specific AutoConfiguration classes. NOTE: The child context
		// is intentionally not completely auto-configured.
		childContext.register(EndpointWebMvcChildContextConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				EmbeddedServletContainerAutoConfiguration.class);

		// Ensure close on the parent also closes the child
		if (this.applicationContext instanceof ConfigurableApplicationContext) {
			((ConfigurableApplicationContext) this.applicationContext)
					.addApplicationListener(new ApplicationListener<ContextClosedEvent>() {
						@Override
						public void onApplicationEvent(ContextClosedEvent event) {
							if (event.getApplicationContext() == EndpointWebMvcAutoConfiguration.this.applicationContext) {
								childContext.close();
							}
						}
					});
		}
		childContext.refresh();
	}

	private static class EndpointCondition implements ConfigurationCondition {

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.REGISTER_BEAN;
		}

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			if (context.getBeanFactory() == null) {
				// FIXME log a warning
				return false;
			}
			return ManagementServerPort.get(context.getBeanFactory()) == ManagementServerPort.SAME;
		}

	}

	private enum ManagementServerPort {

		DISABLE, SAME, DIFFERENT;

		public static ManagementServerPort get(BeanFactory beanFactory) {

			ServerProperties serverProperties;
			try {
				serverProperties = beanFactory.getBean(ServerProperties.class);
			} catch (NoSuchBeanDefinitionException ex) {
				serverProperties = new ServerProperties();
			}

			ManagementServerProperties managementServerProperties;
			try {
				managementServerProperties = beanFactory
						.getBean(ManagementServerProperties.class);
			} catch (NoSuchBeanDefinitionException ex) {
				managementServerProperties = new ManagementServerProperties();
			}

			if (DISABLED_PORT.equals(managementServerProperties.getPort())) {
				return DISABLE;
			}
			return managementServerProperties.getPort() == null
					|| serverProperties.getPort() == managementServerProperties.getPort() ? SAME
					: DIFFERENT;
			// FIXME check the inet address etc are not different since they will be
			// ignored
		}
	};
}
