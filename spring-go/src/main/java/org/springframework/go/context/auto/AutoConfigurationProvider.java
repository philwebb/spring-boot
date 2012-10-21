
package org.springframework.go.context.auto;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * Allows auto-configuration of an {@link ApplicationContext}.
 *
 * @author Phillip Webb
 */
public interface AutoConfigurationProvider {

	/**
	 * Apply any auto-configuration to the specified bean factory via a
	 * {@link AutoConfigurationRegistry}. Auto configuration can be conditional, for
	 * example based on the presence of a specific class or the absence of a specific
	 * {@link BeanDefinition} in the registry. This method will be called after beans from
	 * any user specific {@link Configuration @Configuration} have been registered but
	 * before the application context is refreshed (specifically before
	 * {@link BeanPostProcessor}s have been instantiated).
	 *
	 * @param context the auto-configuration application context
	 * @param beanFactory the bean factory
	 */
	void apply(AutoConfigurationApplicationContext context,
			ConfigurableListableBeanFactory beanFactory);

}
