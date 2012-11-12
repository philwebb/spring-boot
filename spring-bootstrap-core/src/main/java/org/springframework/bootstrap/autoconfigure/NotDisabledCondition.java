package org.springframework.bootstrap.autoconfigure;

import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.ClassMetadata;

/**
 * {@link Condition} that checks if a user has explicitly {@link DisableAutoConfiguration
 * disabled} one or more auto-configuration classes.
 * @author Phillip Webb
 */
class NotDisabledCondition implements Condition {

	@SuppressWarnings("unchecked")
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		if(metadata instanceof ClassMetadata) {
			ClassMetadata classMetadata = (ClassMetadata) metadata;
			if(context.getRegistry().containsBeanDefinition(AutoConfigurationClassPostProcessor.AUTO_CONFIGURATION_BEAN_NAME)) {
				BeanDefinition beanDefinition = context.getRegistry().getBeanDefinition(AutoConfigurationClassPostProcessor.AUTO_CONFIGURATION_BEAN_NAME);
				Set<String> disabled = (Set<String>) beanDefinition.getAttribute(AutoConfigurationClassPostProcessor.DISABLED_AUTO_CONFIGURATIONS_ATTRIBUTE);
				if(disabled != null && disabled.contains(classMetadata.getClassName())) {
					return false;
				}
			}
		}
		return true;
	}


}
