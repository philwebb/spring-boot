
package org.springframework.bootstrap.web.context;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

public class AnnotationConfigEmbeddedWebApplicationContext extends
		EmbeddedWebApplicationContext {

	private Class<?>[] annotatedClasses;

	private String[] basePackages;

	private BeanNameGenerator beanNameGenerator;

	private ScopeMetadataResolver scopeMetadataResolver;

	public void register(Class<?>... annotatedClasses) {
		Assert.notEmpty(annotatedClasses,
				"At least one annotated class must be specified");
		this.annotatedClasses = annotatedClasses;
	}

	public void scan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		this.basePackages = basePackages;
	}

	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
		AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(
				beanFactory);
		reader.setEnvironment(this.getEnvironment());

		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(
				beanFactory);
		scanner.setEnvironment(this.getEnvironment());

		BeanNameGenerator beanNameGenerator = getBeanNameGenerator();
		ScopeMetadataResolver scopeMetadataResolver = getScopeMetadataResolver();
		if (beanNameGenerator != null) {
			reader.setBeanNameGenerator(beanNameGenerator);
			scanner.setBeanNameGenerator(beanNameGenerator);
			beanFactory.registerSingleton(
					AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR,
					beanNameGenerator);
		}
		if (scopeMetadataResolver != null) {
			reader.setScopeMetadataResolver(scopeMetadataResolver);
			scanner.setScopeMetadataResolver(scopeMetadataResolver);
		}

		if (!ObjectUtils.isEmpty(this.annotatedClasses)) {
			if (logger.isInfoEnabled()) {
				logger.info("Registering annotated classes: ["
						+ StringUtils.arrayToCommaDelimitedString(this.annotatedClasses)
						+ "]");
			}
			reader.register(this.annotatedClasses);
		}

		if (!ObjectUtils.isEmpty(this.basePackages)) {
			if (logger.isInfoEnabled()) {
				logger.info("Scanning base packages: ["
						+ StringUtils.arrayToCommaDelimitedString(this.basePackages)
						+ "]");
			}
			scanner.scan(this.basePackages);
		}

		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			for (String configLocation : configLocations) {
				try {
					Class<?> clazz = getClassLoader().loadClass(configLocation);
					if (logger.isInfoEnabled()) {
						logger.info("Successfully resolved class for [" + configLocation
								+ "]");
					}
					reader.register(clazz);
				} catch (ClassNotFoundException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Could not load class for config location ["
								+ configLocation + "] - trying package scan. " + ex);
					}
					int count = scanner.scan(configLocation);
					if (logger.isInfoEnabled()) {
						if (count == 0) {
							logger.info("No annotated classes found for specified class/package ["
									+ configLocation + "]");
						}
						else {
							logger.info("Found " + count
									+ " annotated classes in package [" + configLocation
									+ "]");
						}
					}
				}
			}
		}
	}

	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = beanNameGenerator;
	}

	protected BeanNameGenerator getBeanNameGenerator() {
		return this.beanNameGenerator;
	}

	public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver = scopeMetadataResolver;
	}

	protected ScopeMetadataResolver getScopeMetadataResolver() {
		return this.scopeMetadataResolver;
	}
}
