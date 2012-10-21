
package org.springframework.go.context.auto;

import java.util.Iterator;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import sun.misc.Service;

/**
 * Extension to {@link AnnotationConfigApplicationContext} that supports
 * auto-configuration.
 * 
 * @author Phillip Webb
 */
public class AutoAnnotationConfigApplicationContext extends
		AnnotationConfigApplicationContext implements AutoConfigurationApplicationContext {

	public AutoAnnotationConfigApplicationContext() {
		super();
		AutoConfigurationAwarePostProcessor.register(this);
	}

	public AutoAnnotationConfigApplicationContext(Class<?>... annotatedClasses) {
		this();
		register(annotatedClasses);
		refresh();
	}

	public AutoAnnotationConfigApplicationContext(String... basePackages) {
		this();
		scan(basePackages);
		refresh();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<AutoConfigurationProvider> getAutoConfigurationProviders() {
		return Service.providers(AutoConfigurationProvider.class,
				getBeanFactory().getBeanClassLoader());
	}
}
