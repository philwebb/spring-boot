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
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragmentsCustomizer;
import org.springframework.beans.factory.aot.BeanRegistrationExcludeFilter;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextFactory;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.web.context.ConfigurableWebServerApplicationContext;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.javapoet.ClassName;
import org.springframework.util.Assert;

/**
 * {@link ApplicationListener} used to initialize the management context when it's running
 * on a different port.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ChildManagementContextInitializer implements ApplicationListener<WebServerInitializedEvent>,
		BeanRegistrationAotProcessor, BeanRegistrationExcludeFilter, BeanRegistrationCodeFragmentsCustomizer {

	private final ManagementContextFactory managementContextFactory;

	private final ApplicationContext parentContext;

	ChildManagementContextInitializer(ManagementContextFactory managementContextFactory,
			ApplicationContext parentContext) {
		this.managementContextFactory = managementContextFactory;
		this.parentContext = parentContext;
	}

	@Override
	public void onApplicationEvent(WebServerInitializedEvent event) {
		if (event.getApplicationContext().equals(this.parentContext)) {
			ConfigurableApplicationContext managementContext = createManagementContext();
			registerBeans(managementContext);
			managementContext.refresh();
		}
	}

	@Override
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		Assert.isInstanceOf(ConfigurableApplicationContext.class, this.parentContext);
		BeanFactory parentBeanFactory = ((ConfigurableApplicationContext) this.parentContext).getBeanFactory();
		if (registeredBean.getBeanClass().equals(getClass())
				&& registeredBean.getBeanFactory().equals(parentBeanFactory)) {
			ConfigurableApplicationContext managementContext = createManagementContext();
			registerBeans(managementContext);
			return new AotContribution(managementContext);
		}
		return null;
	}

	@Override
	public boolean isExcluded(RegisteredBean registeredBean) {
		return false; // Don't filter ourself
	}

	@Override
	public BeanRegistrationCodeFragments customizeBeanRegistrationCodeFragments(RegisteredBean registeredBean,
			BeanRegistrationCodeFragments codeFragments) {
		return codeFragments;
	}

	protected void registerBeans(ConfigurableApplicationContext managementContext) {
		Assert.isInstanceOf(AnnotationConfigRegistry.class, managementContext);
		AnnotationConfigRegistry registry = (AnnotationConfigRegistry) managementContext;
		this.managementContextFactory.registerWebServerFactoryBeans(this.parentContext, managementContext, registry);
		registry.register(EnableChildManagementContextConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
		if (isLazyInitialization()) {
			managementContext.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor());
		}
	}

	protected final ConfigurableApplicationContext createManagementContext() {
		ConfigurableApplicationContext managementContext = this.managementContextFactory
				.createManagementContext(this.parentContext);
		managementContext.setId(this.parentContext.getId() + ":management");
		if (managementContext instanceof ConfigurableWebServerApplicationContext webServerApplicationContext) {
			webServerApplicationContext.setServerNamespace("management");
		}
		if (managementContext instanceof DefaultResourceLoader resourceLoader) {
			resourceLoader.setClassLoader(this.parentContext.getClassLoader());
		}
		CloseManagementContextListener.addIfPossible(this.parentContext, managementContext);
		return managementContext;
	}

	private boolean isLazyInitialization() {
		AbstractApplicationContext context = (AbstractApplicationContext) this.parentContext;
		List<BeanFactoryPostProcessor> postProcessors = context.getBeanFactoryPostProcessors();
		return postProcessors.stream().anyMatch(LazyInitializationBeanFactoryPostProcessor.class::isInstance);
	}

	/**
	 * {@link BeanRegistrationAotContribution} for
	 * {@link ChildManagementContextInitializer}.
	 */
	private static class AotContribution implements BeanRegistrationAotContribution {

		private final GenericApplicationContext managementContext;

		AotContribution(ConfigurableApplicationContext managementContext) {
			Assert.isInstanceOf(GenericApplicationContext.class, managementContext);
			this.managementContext = (GenericApplicationContext) managementContext;
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
			ClassName generatedInitializerClassName = generationContext.getClassNameGenerator().generateClassName("",
					"test");
			new ApplicationContextAotGenerator().generateApplicationContext(this.managementContext, generationContext,
					generatedInitializerClassName);
		}

	}

	/**
	 * {@link ChildManagementContextInitializer} used for AOT processed applications.
	 */
	static class AotChildManagementContextInitializer extends ChildManagementContextInitializer {

		private final ApplicationContextInitializer<ConfigurableApplicationContext> initializer;

		AotChildManagementContextInitializer(ManagementContextFactory managementContextFactory,
				ApplicationContext parentContext,
				ApplicationContextInitializer<ConfigurableApplicationContext> initializer) {
			super(managementContextFactory, parentContext);
			this.initializer = initializer;
		}

		@Override
		protected void registerBeans(ConfigurableApplicationContext managementContext) {
			this.initializer.initialize(managementContext);
		}

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

}
