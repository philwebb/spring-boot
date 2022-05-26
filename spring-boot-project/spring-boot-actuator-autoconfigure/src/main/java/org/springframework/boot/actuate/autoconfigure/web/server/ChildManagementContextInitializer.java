/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.server;

import java.util.List;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextFactory;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.web.context.ConfigurableWebServerApplicationContext;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.Assert;

/**
 * {@link ApplicationListener} used to initialize the management context when it's running
 * on a different port.
 *
 * @param <C> the context type
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ChildManagementContextInitializer<C extends ConfigurableWebServerApplicationContext & AnnotationConfigRegistry>
		implements ApplicationListener<WebServerInitializedEvent>, BeanRegistrationAotProcessor {

	private final ApplicationContext applicationContext;

	private final ManagementContextFactory<C> managementContextFactory;

	ChildManagementContextInitializer(ApplicationContext applicationContext,
			ManagementContextFactory<C> managementContextFactory) {
		this.applicationContext = applicationContext;
		this.managementContextFactory = managementContextFactory;
	}

	@Override
	public void onApplicationEvent(WebServerInitializedEvent event) {
		if (event.getApplicationContext().equals(this.applicationContext)) {
			createAndInitializeManagementContext();
		}
	}

	protected void createAndInitializeManagementContext() {
		createManagementContext(true).refresh();
	}

	@Override
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		Assert.isInstanceOf(ConfigurableApplicationContext.class, this.applicationContext);
		if (registeredBean.getBeanClass().equals(getClass()) && registeredBean.getBeanFactory()
				.equals(((ConfigurableApplicationContext) this.applicationContext).getBeanFactory())) {
			return new AotContribution(createManagementContext(true));
		}
		return null;
	}

	protected final C createManagementContext(boolean registerBeans) {
		ApplicationContext parent = this.applicationContext;
		C child = this.managementContextFactory.createManagementContext(parent, registerBeans);
		if (registerBeans) {
			child.register(EnableChildManagementContextConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
			if (isLazyInitialization()) {
				child.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor());
			}
		}
		child.setServerNamespace("management");
		child.setId(parent.getId() + ":management");
		if (child instanceof DefaultResourceLoader) {
			((DefaultResourceLoader) child).setClassLoader(parent.getClassLoader());
		}
		CloseManagementContextListener.addIfPossible(parent, child);
		return child;
	}

	private boolean isLazyInitialization() {
		AbstractApplicationContext context = (AbstractApplicationContext) this.applicationContext;
		List<BeanFactoryPostProcessor> postProcessors = context.getBeanFactoryPostProcessors();
		return postProcessors.stream().anyMatch(LazyInitializationBeanFactoryPostProcessor.class::isInstance);
	}

	/**
	 * {@link ApplicationListener} to propagate the {@link ContextClosedEvent} and
	 * {@link ApplicationFailedEvent} from a parent to a child.
	 */
	private static class CloseManagementContextListener implements ApplicationListener<ApplicationEvent> {

		private final ApplicationContext parentContext;

		private final ConfigurableApplicationContext childContext;

		CloseManagementContextListener(ApplicationContext parentContext, ConfigurableApplicationContext childContext) {
			this.parentContext = parentContext;
			this.childContext = childContext;
		}

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			if (event instanceof ContextClosedEvent) {
				onContextClosedEvent((ContextClosedEvent) event);
			}
			if (event instanceof ApplicationFailedEvent) {
				onApplicationFailedEvent((ApplicationFailedEvent) event);
			}
		}

		private void onContextClosedEvent(ContextClosedEvent event) {
			propagateCloseIfNecessary(event.getApplicationContext());
		}

		private void onApplicationFailedEvent(ApplicationFailedEvent event) {
			propagateCloseIfNecessary(event.getApplicationContext());
		}

		private void propagateCloseIfNecessary(ApplicationContext applicationContext) {
			if (applicationContext == this.parentContext) {
				this.childContext.close();
			}
		}

		static void addIfPossible(ApplicationContext parentContext, ConfigurableApplicationContext childContext) {
			if (parentContext instanceof ConfigurableApplicationContext) {
				add((ConfigurableApplicationContext) parentContext, childContext);
			}
		}

		private static void add(ConfigurableApplicationContext parentContext,
				ConfigurableApplicationContext childContext) {
			parentContext.addApplicationListener(new CloseManagementContextListener(parentContext, childContext));
		}

	}

	private static class AotContribution implements BeanRegistrationAotContribution {

		AotContribution(ConfigurableWebServerApplicationContext managementContext) {
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
		}

	}

}
