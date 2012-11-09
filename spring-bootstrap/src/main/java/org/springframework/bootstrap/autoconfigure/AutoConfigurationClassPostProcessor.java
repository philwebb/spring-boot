
package org.springframework.bootstrap.autoconfigure;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;

public class AutoConfigurationClassPostProcessor extends ConfigurationClassPostProcessor {

	private static final String DISABLE_AUTO_CONFIGURATION_ANNOTATION = DisableAutoConfiguration.class.getName();

	public static final String AUTO_CONFIGURATION_BEAN_NAME = "org.springframework.bootstrap.autoconfigure.internalAutoConfiguration";

	public static final String DISABLED_AUTO_CONFIGURATIONS_ATTRIBUTE = "disabledAutoConfigrations";

	@Override
	public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
		registerAutoConfigurationBean(registry);
		super.processConfigBeanDefinitions(registry);
	}

	private void registerAutoConfigurationBean(BeanDefinitionRegistry registry) {
		// Ensure the auto configure bean is always the last bean
		if (registry.containsBeanDefinition(AUTO_CONFIGURATION_BEAN_NAME)) {
			registry.removeBeanDefinition(AUTO_CONFIGURATION_BEAN_NAME);
		}
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(AutoConfigurationBean.class);
		registry.registerBeanDefinition(AUTO_CONFIGURATION_BEAN_NAME,
				beanDefinition);
	}

	@Override
	protected void afterLoadConfigurationClass(BeanDefinitionRegistry registry,
			String beanName, AnnotationMetadata metadata) {
		super.afterLoadConfigurationClass(registry, beanName, metadata);
		if(metadata.isAnnotated(DISABLE_AUTO_CONFIGURATION_ANNOTATION)) {
			Map<String, Object> annotation = metadata.getAnnotationAttributes(DISABLE_AUTO_CONFIGURATION_ANNOTATION);
			Object[] annotationValue = (Object[]) annotation.get("value");
			for (Object configurationClass : annotationValue) {
				addDisabledAutoConfiguration(registry, configurationClass);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void addDisabledAutoConfiguration(BeanDefinitionRegistry registry, Object configurationClass) {
		if(configurationClass instanceof Class<?>) {
			configurationClass = ((Class<?>)configurationClass).getName();
		}
		BeanDefinition definition = registry.getBeanDefinition(AUTO_CONFIGURATION_BEAN_NAME);
		Set<String> disabled = (Set<String>) definition.getAttribute(DISABLED_AUTO_CONFIGURATIONS_ATTRIBUTE);
		if(disabled == null) {
			disabled = new HashSet<String>();
			definition.setAttribute(DISABLED_AUTO_CONFIGURATIONS_ATTRIBUTE, disabled);
		}
		disabled.add((String)configurationClass);
	}

	@Configuration
	@Import(AutoConfigurationImports.class)
	public static class AutoConfigurationBean {
	}

	public static class AutoConfigurationImports implements ImportSelector {
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			List<String> factoryNames = SpringFactoriesLoader.loadFactoryNames(AutoConfiguration.class, getClass().getClassLoader());
			return factoryNames.toArray(new String[factoryNames.size()]);
		}
	}

}
