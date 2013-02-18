
package org.springframework.bootstrap.autoconfigure;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
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
 * @author Phillip Webb
 */
public class AutoConfigurationApplicationContextInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext> {

	public void initialize(ConfigurableApplicationContext applicationContext) {
		// Manually added post-processors will run before beans, this is critical
		// to ensure we replace any existing ConfigurationClassPostProcessor
		applicationContext.addBeanFactoryPostProcessor(new AutoConfigurationRegistrationPostProcessor());
	}

	private static class AutoConfigurationRegistrationPostProcessor implements BeanDefinitionRegistryPostProcessor {

		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
				throws BeansException {
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
