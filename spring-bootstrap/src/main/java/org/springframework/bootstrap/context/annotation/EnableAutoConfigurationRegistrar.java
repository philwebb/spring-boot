
package org.springframework.bootstrap.context.annotation;

import java.util.List;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;

@Order(Ordered.LOWEST_PRECEDENCE)
class EnableAutoConfigurationRegistrar implements DeferredImportSelector, BeanClassLoaderAware {

	private ClassLoader beanClassLoader;

	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		importingClassMetadata.getAnnotationAttributes(EnableAutoConfiguration.class.getName(), true);
		List<String> factoryNames = SpringFactoriesLoader.loadFactoryNames(AutoConfiguration.class, this.beanClassLoader);
		return factoryNames.toArray(new String[factoryNames.size()]);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

}
