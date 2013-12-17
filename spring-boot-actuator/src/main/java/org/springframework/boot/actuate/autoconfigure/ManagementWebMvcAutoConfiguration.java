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

package org.springframework.boot.actuate.autoconfigure;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.properties.ManagementServerProperties;
import org.springframework.boot.actuate.web.ManagementHandlerMapping;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

/**
 * {@link EnableAutoConfiguration Auto-configuration} allowing Spring MVC to handle
 * {@link ManagementHandlerMapping}s. If the {@link ManagementServerProperties} specifies
 * a different port to {@link ServerProperties} a new child context is created, otherwise
 * requests will be handled via an already registered {@link DispatcherServlet} .
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class })
@ConditionalOnWebApplication
@AutoConfigureAfter({ PropertyPlaceholderAutoConfiguration.class,
		EmbeddedServletContainerAutoConfiguration.class, WebMvcAutoConfiguration.class,
		ManagementServerPropertiesAutoConfiguration.class })
public class ManagementWebMvcAutoConfiguration implements ApplicationContextAware,
		ApplicationListener<ContextRefreshedEvent> {

	private static final Integer DISABLED_PORT = Integer.valueOf(0);

	private static final HandlerMapping NO_MAPPING = new HandlerMapping() {
		@Override
		public HandlerExecutionChain getHandler(HttpServletRequest request)
				throws Exception {
			return null;
		}
	};

	private ApplicationContext applicationContext;

	@Bean
	public HandlerMapping managementHandlerMappingAdapter() {
		ManagementServerPort port = ManagementServerPort.get(this.applicationContext);
		if (port != ManagementServerPort.SAME) {
			return NO_MAPPING;
		}
		return new ManagementHandlerMappingAdapter();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (event.getApplicationContext() == this.applicationContext) {
			if (ManagementServerPort.get(this.applicationContext) == ManagementServerPort.DIFFERENT
					&& this.applicationContext instanceof WebApplicationContext) {
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
		childContext.register(ManagementWebMvcChildContextConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				EmbeddedServletContainerAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class);

		// Ensure close on the parent also closes the child
		if (this.applicationContext instanceof ConfigurableApplicationContext) {
			((ConfigurableApplicationContext) this.applicationContext)
					.addApplicationListener(new ApplicationListener<ContextClosedEvent>() {
						@Override
						public void onApplicationEvent(ContextClosedEvent event) {
							if (event.getApplicationContext() == ManagementWebMvcAutoConfiguration.this.applicationContext) {
								childContext.close();
							}
						}
					});
		}

		childContext.refresh();
	}

	private enum ManagementServerPort {

		DISABLE, SAME, DIFFERENT;

		public static ManagementServerPort get(ApplicationContext beanFactory) {

			ServerProperties serverProperties;
			try {
				serverProperties = beanFactory.getBean(ServerProperties.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				serverProperties = new ServerProperties();
			}

			ManagementServerProperties managementServerProperties;
			try {
				managementServerProperties = beanFactory
						.getBean(ManagementServerProperties.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				managementServerProperties = new ManagementServerProperties();
			}

			if (DISABLED_PORT.equals(managementServerProperties.getPort())) {
				return DISABLE;
			}
			if (!(beanFactory instanceof WebApplicationContext)) {
				// Current context is no a a webapp
				return DIFFERENT;
			}
			return managementServerProperties.getPort() == null
					|| serverProperties.getPort() == null
					&& managementServerProperties.getPort().equals(8080)
					|| managementServerProperties.getPort().equals(
							serverProperties.getPort()) ? SAME : DIFFERENT;
		}
	};
}
