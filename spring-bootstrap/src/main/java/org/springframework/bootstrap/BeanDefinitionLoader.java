
package org.springframework.bootstrap;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AbstractTypeHierarchyTraversingFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

class BeanDefinitionLoader {

	private static final ResourceLoader DEFAULT_RESOURCE_LOADER = new DefaultResourceLoader();

	private Object[] sources;

	private AnnotatedBeanDefinitionReader annotatedReader;

	private XmlBeanDefinitionReader xmlReader;

	private ClassPathBeanDefinitionScanner scanner;

	private ResourceLoader resourceLoader;


	public BeanDefinitionLoader(BeanDefinitionRegistry registry, Object... sources) {
		Assert.notNull(registry, "Registry must not be null");
		Assert.notEmpty(sources, "Sources must not be empty");
		this.sources = sources;
		this.annotatedReader = new AnnotatedBeanDefinitionReader(registry);
		this.xmlReader = new XmlBeanDefinitionReader(registry);
		this.scanner = new ClassPathBeanDefinitionScanner(registry);
		this.scanner.addExcludeFilter(new ClassExcludeFilter(sources));
	}

	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.annotatedReader.setBeanNameGenerator(beanNameGenerator);
		this.xmlReader.setBeanNameGenerator(beanNameGenerator);
		this.scanner.setBeanNameGenerator(beanNameGenerator);
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
		this.xmlReader.setResourceLoader(resourceLoader);
		if(resourceLoader != null) {
			this.scanner.setResourceLoader(resourceLoader);
		}
	}

	public void setEnvironment(ConfigurableEnvironment environment) {
		this.annotatedReader.setEnvironment(environment);
		this.xmlReader.setEnvironment(environment);
		this.scanner.setEnvironment(environment);
	}

	public int load() {
		int count = 0;
		for (Object source : this.sources) {
			count += load(source);
		}
		return count;
	}

	private int load(Object source) {
		Assert.notNull(source, "Source must not be null");
		if (source instanceof Class<?>) {
			this.annotatedReader.register((Class<?>) source);
			return 1;
		}

		if (source instanceof Resource) {
			return this.xmlReader.loadBeanDefinitions((Resource) source);
		}

		if (source instanceof Package) {
			// FIXME register the scanned package
			return this.scanner.scan(((Package) source).getName());
		}

		if (source instanceof CharSequence) {
			try {
				return load(Class.forName(source.toString()));
			}
			catch (ClassNotFoundException e) {
			}

			Resource loadedResource = (this.resourceLoader != null ? this.resourceLoader
					: DEFAULT_RESOURCE_LOADER).getResource(source.toString());
			if (loadedResource != null && loadedResource.exists()) {
				return load(loadedResource);
			}
			Package packageResource = Package.getPackage(source.toString());
			if (packageResource != null) {
				return load(packageResource);
			}
		}

		throw new IllegalArgumentException("Invalid source '" + source + "'");
	}


	private static class ClassExcludeFilter extends AbstractTypeHierarchyTraversingFilter {

		private Set<String> classNames = new HashSet<String>();

		public ClassExcludeFilter(Object... sources) {
			super(false, false);
			for (Object source : sources) {
				if(source instanceof Class<?>) {
					this.classNames.add(((Class<?>)source).getName());
				}
			}
		}

		@Override
		protected boolean matchClassName(String className) {
			return this.classNames.contains(className);
		}
	}


}
