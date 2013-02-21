/*
 * Copyright 2012 the original author or authors.
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

package org.springframework.bootstrap.autoconfigure;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.util.Assert;

/**
 * {@link ApplicationContextInitializer} to support auto-configuration of the application
 * context.
 *
 * @author Phillip Webb
 */
public class AutoConfigurationApplicationContextInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext> {

	private AutoConfigurationSettings settings;

	public AutoConfigurationApplicationContextInitializer(AutoConfigurationSettings settings) {
		Assert.notNull(settings, "Settings must not be null");
		this.settings = settings;
	}

	public void initialize(ConfigurableApplicationContext applicationContext) {
		// Manually added post-processors will run before beans, this is critical
		// to ensure we replace any existing ConfigurationClassPostProcessor
		applicationContext.addBeanFactoryPostProcessor(new AutoConfigurationRegistrationPostProcessor(settings));
	}

	private static class AutoConfigurationRegistrationPostProcessor implements BeanDefinitionRegistryPostProcessor {

		private AutoConfigurationSettings settings;

		public AutoConfigurationRegistrationPostProcessor(
				AutoConfigurationSettings settings) {
			this.settings = settings;
		}

		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
				throws BeansException {

			Assert.isInstanceOf(SingletonBeanRegistry.class, registry);

			// Register the settings early
			((SingletonBeanRegistry) registry).registerSingleton(
					AutoConfigurationSettings.BEAN_NAME, settings);

			// Register ConfigurationClassPostProcessor if not already present
			AnnotationConfigUtils.registerAnnotationConfigProcessors(registry);

			// Replace ConfigurationClassPostProcessor with an auto-register variant
			BeanDefinition postProcessor = registry.getBeanDefinition(AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME);
			Assert.state(postProcessor != null, "Unable to find configuration class post processor bean");
			Assert.state(ConfigurationClassPostProcessor.class.getName().equals(postProcessor.getBeanClassName()),
					"Unable to auto-configure custom ConfigurationClassPostProcessor");
			postProcessor.setBeanClassName(AutoConfigurationClassPostProcessor.class.getName());
		}

		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
				throws BeansException {
		}
	}
}
