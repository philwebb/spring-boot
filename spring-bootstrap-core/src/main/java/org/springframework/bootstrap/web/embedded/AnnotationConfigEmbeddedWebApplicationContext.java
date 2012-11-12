
package org.springframework.bootstrap.web.embedded;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.util.Assert;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * Variant of {@link AnnotationConfigWebApplicationContext} that support
 * {@link EmbeddedServletContainer embedded} servlet containers.
 * @author Phillip Webb
 * @see EmbeddedWebApplicationContext
 */
public class AnnotationConfigEmbeddedWebApplicationContext extends
		EmbeddedWebApplicationContext {

	private Delegate delegate = new Delegate();

	public void register(Class<?>... annotatedClasses) {
		Assert.notEmpty(annotatedClasses,
				"At least one annotated class must be specified");
		this.delegate.register(annotatedClasses);
	}

	public void scan(String... basePackages) {
		this.delegate.scan(basePackages);
	}

	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
		this.delegate.loadBeanDefinitions(beanFactory);
	}

	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.delegate.setBeanNameGenerator(beanNameGenerator);
	}

	public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
		this.delegate.setScopeMetadataResolver(scopeMetadataResolver);
	}

	private class Delegate extends AnnotationConfigWebApplicationContext {

		@Override
		protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
			super.loadBeanDefinitions(beanFactory);
		}

		@Override
		public ConfigurableWebEnvironment getEnvironment() {
			return AnnotationConfigEmbeddedWebApplicationContext.this.getEnvironment();
		}

		@Override
		public String[] getConfigLocations() {
			return AnnotationConfigEmbeddedWebApplicationContext.this.getConfigLocations();
		}

		@Override
		public ClassLoader getClassLoader() {
			return AnnotationConfigEmbeddedWebApplicationContext.this.getClassLoader();
		}
	}

}
