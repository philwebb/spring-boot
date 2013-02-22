
package org.springframework.bootstrap.context.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AbstractTypeHierarchyTraversingFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

@Order(Ordered.LOWEST_PRECEDENCE)
class EnableAutoConfigurationImportSelector implements DeferredImportSelector, BeanClassLoaderAware, BeanFactoryAware {

	private ClassLoader beanClassLoader;

	private BeanFactory beanFactory;

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		String declaringClass = metadata.getClassName();
		AnnotationAttributes attributes = getAttributes(metadata, EnableAutoConfiguration.class);
		AnnotationAttributes componentScanAttributes = getAttributes(metadata, ComponentScan.class);
		boolean componentScan = attributes.getBoolean("componentScan");

		// If there is no @ComponentScan annotation and the user has not disabled it we scan
		if(componentScanAttributes == null && componentScan) {
			scan(declaringClass);
		}
		if(componentScanAttributes != null || componentScan) {
			saveBasePackages(declaringClass, componentScanAttributes);
		}

		List<String> factoryNames = SpringFactoriesLoader.loadFactoryNames(AutoConfiguration.class, this.beanClassLoader);
		factoryNames.removeAll(Arrays.asList(attributes.getStringArray("excluding")));
		return factoryNames.toArray(new String[factoryNames.size()]);
	}

	private AnnotationAttributes getAttributes(AnnotationMetadata metadata, Class<?> annotation) {
		return AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(annotation.getName(), true));
	}

	private void scan(final String declaringClass) {
		Assert.isInstanceOf(BeanDefinitionRegistry.class, this.beanFactory, "Unable to perform component scanning");
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner((BeanDefinitionRegistry) this.beanFactory);
		scanner.addExcludeFilter(new AbstractTypeHierarchyTraversingFilter(false, false){
			@Override
			protected boolean matchClassName(String className) {
				return declaringClass.equals(className);
			}
		});
		scanner.scan(ClassUtils.getPackageName(declaringClass));
	}

	private void saveBasePackages(String declaringClass, AnnotationAttributes componentScanAttributes) {
		List<String> basePackages = new ArrayList<String>();
		if(componentScanAttributes != null) {
			addAllHavingText(basePackages, componentScanAttributes.getStringArray("value"));
			addAllHavingText(basePackages, componentScanAttributes.getStringArray("basePackages"));
			for (String packageClass : componentScanAttributes.getStringArray("basePackageClasses")) {
				basePackages.add(ClassUtils.getPackageName(packageClass));
			}
		}
		if (basePackages.isEmpty()) {
			basePackages.add(ClassUtils.getPackageName(declaringClass));
		}
		//FIXME SAVE
		System.out.println(basePackages);
	}

	private void addAllHavingText(List<String> list, String[] strings) {
		for (String s : strings) {
			if (StringUtils.hasText(s)) {
				list.add(s);
			}
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

}
