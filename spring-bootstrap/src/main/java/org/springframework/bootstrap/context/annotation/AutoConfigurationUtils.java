
package org.springframework.bootstrap.context.annotation;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

public abstract class AutoConfigurationUtils {

	static String BASE_PACKAGES_BEAN = AutoConfigurationUtils.class.getName()
			+ ".basePackages";

	@SuppressWarnings("unchecked")
	public static List<String> getBasePackages(BeanFactory beanFactory) {
		try {
			return beanFactory.getBean(BASE_PACKAGES_BEAN, List.class);
		}
		catch (NoSuchBeanDefinitionException e) {
			return Collections.emptyList();
		}
	}
}
