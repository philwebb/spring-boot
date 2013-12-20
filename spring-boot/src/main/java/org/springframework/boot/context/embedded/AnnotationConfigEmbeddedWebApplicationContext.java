/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * {@link EmbeddedWebApplicationContext} that accepts annotated classes as input - in
 * particular {@link org.springframework.context.annotation.Configuration
 * <code>@Configuration</code>}-annotated classes, but also plain
 * {@link org.springframework.stereotype.Component <code>@Component</code>} classes and
 * JSR-330 compliant classes using {@code javax.inject} annotations. Allows for
 * registering classes one by one (specifying class names as config location) as well as
 * for classpath scanning (specifying base packages as config location).
 * 
 * <p>
 * Note: In case of multiple {@code @Configuration} classes, later {@code @Bean}
 * definitions will override ones defined in earlier loaded files. This can be leveraged
 * to deliberately override certain bean definitions via an extra Configuration class.
 * 
 * @author Phillip Webb
 * @see #register(Class...)
 * @see #scan(String...)
 * @see EmbeddedWebApplicationContext
 * @see AnnotationConfigWebApplicationContext
 */
public class AnnotationConfigEmbeddedWebApplicationContext extends
		EmbeddedWebApplicationContext {

	private BeanNameGenerator beanNameGenerator;

	private ScopeMetadataResolver scopeMetadataResolver;

	private final Set<Class<?>> annotatedClasses = new LinkedHashSet<Class<?>>();

	private final Set<String> basePackages = new LinkedHashSet<String>();

	/**
	 * Set a custom {@link BeanNameGenerator} for use with
	 * {@link AnnotatedBeanDefinitionReader} and/or {@link ClassPathBeanDefinitionScanner}
	 * .
	 * <p>
	 * Default is
	 * {@link org.springframework.context.annotation.AnnotationBeanNameGenerator}.
	 * @see AnnotatedBeanDefinitionReader#setBeanNameGenerator
	 * @see ClassPathBeanDefinitionScanner#setBeanNameGenerator
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = beanNameGenerator;
	}

	/**
	 * Return the custom {@link BeanNameGenerator} for use with
	 * {@link AnnotatedBeanDefinitionReader} and/or {@link ClassPathBeanDefinitionScanner}
	 * , if any.
	 */
	protected BeanNameGenerator getBeanNameGenerator() {
		return this.beanNameGenerator;
	}

	/**
	 * Set a custom {@link ScopeMetadataResolver} for use with
	 * {@link AnnotatedBeanDefinitionReader} and/or {@link ClassPathBeanDefinitionScanner}
	 * .
	 * <p>
	 * Default is an
	 * {@link org.springframework.context.annotation.AnnotationScopeMetadataResolver}.
	 * @see AnnotatedBeanDefinitionReader#setScopeMetadataResolver
	 * @see ClassPathBeanDefinitionScanner#setScopeMetadataResolver
	 */
	public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver = scopeMetadataResolver;
	}

	/**
	 * Return the custom {@link ScopeMetadataResolver} for use with
	 * {@link AnnotatedBeanDefinitionReader} and/or {@link ClassPathBeanDefinitionScanner}
	 * , if any.
	 */
	protected ScopeMetadataResolver getScopeMetadataResolver() {
		return this.scopeMetadataResolver;
	}

	/**
	 * Register one or more annotated classes to be processed. Note that
	 * {@link #refresh()} must be called in order for the context to fully process the new
	 * class.
	 * <p>
	 * Calls to {@code register} are idempotent; adding the same annotated class more than
	 * once has no additional effect.
	 * @param annotatedClasses one or more annotated classes, e.g.
	 * {@link org.springframework.context.annotation.Configuration @Configuration} classes
	 * @see #scan(String...)
	 * @see #loadBeanDefinitions(DefaultListableBeanFactory)
	 * @see #setConfigLocation(String)
	 * @see #refresh()
	 */
	public void register(Class<?>... annotatedClasses) {
		Assert.notEmpty(annotatedClasses,
				"At least one annotated class must be specified");
		this.annotatedClasses.addAll(Arrays.asList(annotatedClasses));
	}

	/**
	 * Perform a scan within the specified base packages. Note that {@link #refresh()}
	 * must be called in order for the context to fully process the new class.
	 * @param basePackages the packages to check for annotated classes
	 * @see #loadBeanDefinitions(DefaultListableBeanFactory)
	 * @see #register(Class...)
	 * @see #setConfigLocation(String)
	 * @see #refresh()
	 */
	public void scan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		this.basePackages.addAll(Arrays.asList(basePackages));
	}

	/**
	 * Register a {@link org.springframework.beans.factory.config.BeanDefinition} for any
	 * classes specified by {@link #register(Class...)} and scan any packages specified by
	 * {@link #scan(String...)}.
	 * <p>
	 * For any values specified by {@link #setConfigLocation(String)} or
	 * {@link #setConfigLocations(String[])}, attempt first to load each location as a
	 * class, registering a {@code BeanDefinition} if class loading is successful, and if
	 * class loading fails (i.e. a {@code ClassNotFoundException} is raised), assume the
	 * value is a package and attempt to scan it for annotated classes.
	 * <p>
	 * Enables the default set of annotation configuration post processors, such that
	 * {@code @Autowired}, {@code @Required}, and associated annotations can be used.
	 * <p>
	 * Configuration class bean definitions are registered with generated bean definition
	 * names unless the {@code value} attribute is provided to the stereotype annotation.
	 * @see #register(Class...)
	 * @see #scan(String...)
	 * @see #setConfigLocation(String)
	 * @see #setConfigLocations(String[])
	 * @see AnnotatedBeanDefinitionReader
	 * @see ClassPathBeanDefinitionScanner
	 */
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

		if (!this.annotatedClasses.isEmpty()) {
			if (this.logger.isInfoEnabled()) {
				this.logger.info("Registering annotated classes: ["
						+ StringUtils
								.collectionToCommaDelimitedString(this.annotatedClasses)
						+ "]");
			}
			reader.register(this.annotatedClasses
					.toArray(new Class<?>[this.annotatedClasses.size()]));
		}

		if (!this.basePackages.isEmpty()) {
			if (this.logger.isInfoEnabled()) {
				this.logger.info("Scanning base packages: ["
						+ StringUtils.collectionToCommaDelimitedString(this.basePackages)
						+ "]");
			}
			scanner.scan(this.basePackages.toArray(new String[this.basePackages.size()]));
		}

		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			for (String configLocation : configLocations) {
				try {
					Class<?> clazz = getClassLoader().loadClass(configLocation);
					if (this.logger.isInfoEnabled()) {
						this.logger.info("Successfully resolved class for ["
								+ configLocation + "]");
					}
					reader.register(clazz);
				}
				catch (ClassNotFoundException ex) {
					if (this.logger.isDebugEnabled()) {
						this.logger.debug("Could not load class for config location ["
								+ configLocation + "] - trying package scan. " + ex);
					}
					int count = scanner.scan(configLocation);
					if (this.logger.isInfoEnabled()) {
						if (count == 0) {
							this.logger
									.info("No annotated classes found for specified class/package ["
											+ configLocation + "]");
						}
						else {
							this.logger.info("Found " + count
									+ " annotated classes in package [" + configLocation
									+ "]");
						}
					}
				}
			}
		}
	}

}
