package org.springframework.bootstrap.autoconfigure;

import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Condition;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.ClassMetadata;

class NotDisabledCondition implements Condition, BeanFactoryAware {

	private BeanFactory beanFactory;

	@SuppressWarnings("unchecked")
	public boolean matches(AnnotatedTypeMetadata metadata) {
		if(metadata instanceof ClassMetadata && beanFactory != null && beanFactory instanceof BeanDefinitionRegistry) {
			ClassMetadata classMetadata = (ClassMetadata) metadata;
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			if(registry.containsBeanDefinition(AutoConfigurationClassPostProcessor.AUTO_CONFIGURATION_BEAN_NAME)) {
				BeanDefinition beanDefinition = registry.getBeanDefinition(AutoConfigurationClassPostProcessor.AUTO_CONFIGURATION_BEAN_NAME);
				Set<String> disabled = (Set<String>) beanDefinition.getAttribute(AutoConfigurationClassPostProcessor.DISABLED_AUTO_CONFIGURATIONS_ATTRIBUTE);
				if(disabled != null && disabled.contains(classMetadata.getClassName())) {
					return false;
				}
			}
		}
		return true;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

}
