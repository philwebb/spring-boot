/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded.jetty;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.util.Assert;

/**
 * Jetty {@link Configuration} that calls {@link ServletContextInitializer}s.
 *
 * @author Phillip Webb
 */
public class ServletContextInitializerConfiguration extends AbstractConfiguration {

	private static final Class<?>[] NO_CLASSES = {};

	private final ContainerInitializer initializer;

	/**
	 * Create a new {@link ServletContextInitializerConfiguration}.
	 * @param contextHandler the Jetty ContextHandler
	 * @param initializers the initializers that should be invoked
	 * @deprecated since 1.2.1 in favor of
	 * {@link #ServletContextInitializerConfiguration(ServletContextInitializer...)}
	 */
	@Deprecated
	public ServletContextInitializerConfiguration(ContextHandler contextHandler,
			ServletContextInitializer... initializers) {
		this(initializers);
	}

	/**
	 * Create a new {@link ServletContextInitializerConfiguration}.
	 * @param initializers the initializers that should be invoked
	 */
	public ServletContextInitializerConfiguration(
			ServletContextInitializer... initializers) {
		Assert.notNull(initializers, "Initializers must not be null");
		this.initializer = createContainerInitializer(initializers);
	}

	private ContainerInitializer createContainerInitializer(
			final ServletContextInitializer[] initializers) {
		return createContainerInitializer(new ServletContainerInitializer() {
			@Override
			public void onStartup(Set<Class<?>> classes, ServletContext servletContext)
					throws ServletException {
				for (ServletContextInitializer initializer : initializers) {
					initializer.onStartup(servletContext);
				}
			}
		});
	}

	private ContainerInitializer createContainerInitializer(
			ServletContainerInitializer target) {
		try {
			return new ContainerInitializer(target, NO_CLASSES);
		}
		catch (NoSuchMethodError ex) {
			return createJetty8ContainerInitializer(target);
		}
	}

	private ContainerInitializer createJetty8ContainerInitializer(
			ServletContainerInitializer target) {
		try {
			BeanWrapper wrapper = new BeanWrapperImpl(ContainerInitializer.class);
			wrapper.setPropertyValue("target", target);
			return (ContainerInitializer) wrapper.getWrappedInstance();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to create ContainerInitializer", ex);
		}
	}

	@Override
	public void configure(WebAppContext context) throws Exception {
		context.addBean(new InitializerListener(context), true);
	}

	private class InitializerListener extends AbstractLifeCycle {

		private final WebAppContext context;

		public InitializerListener(WebAppContext context) {
			this.context = context;
		}

		@Override
		protected void doStart() throws Exception {
			ServletContextInitializerConfiguration.this.initializer
					.callStartup(this.context);
		}
	}

}
