
package org.springframework.bootstrap.autoconfigure;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

public class AutoConfigurationApplicationContextInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	public void initialize(ConfigurableApplicationContext applicationContext) {
		Assert.isInstanceOf(BeanDefinitionRegistry.class, applicationContext,
				"Unable to auto configure this type of ApplicationContext");
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) applicationContext;
		initialize(applicationContext, registry);
	}

	private void initialize(ConfigurableApplicationContext applicationContext,
			BeanDefinitionRegistry registry) {
		AnnotationConfigUtils.registerAnnotationConfigProcessors(registry);
		BeanDefinition postProcessor = registry.getBeanDefinition(AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME);
		Assert.state(postProcessor != null,
				"Unable to find configuration class post processor bean");
		Assert.state(
				ConfigurationClassPostProcessor.class.getName().equals(
						postProcessor.getBeanClassName()),
				"Unable to auto-configure custom ConfigurationClassPostProcessor");
		postProcessor.setBeanClassName(AutoConfigurationClassPostProcessor.class.getName());
	}

	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}
}
