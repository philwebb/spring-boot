
package org.springframework.go.context.auto;

import java.util.Iterator;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import sun.misc.Service;

/**
 * Extension to {@link AnnotationConfigWebApplicationContext} that supports
 * auto-configuration.
 *
 * @author Phillip Webb
 */
public class AutoAnnotationConfigWebApplicationContext extends
		AnnotationConfigWebApplicationContext implements
		AutoConfigurationApplicationContext {

	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
		super.loadBeanDefinitions(beanFactory);
		AutoConfigurationAwarePostProcessor.register(beanFactory);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<AutoConfigurationProvider> getAutoConfigurationProviders() {
		return Service.providers(AutoConfigurationProvider.class,
				getBeanFactory().getBeanClassLoader());
	}
}
