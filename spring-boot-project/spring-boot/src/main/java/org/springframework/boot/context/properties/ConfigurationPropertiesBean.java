/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.validation.annotation.Validated;

/**
 * Provides access to {@link ConfigurationProperties} beans from an
 * {@link ApplicationContext}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @see #get(ApplicationContext, Object, String)
 * @see #getAll(ApplicationContext)
 */
public final class ConfigurationPropertiesBean {

	private final String name;

	private final Object instance;

	private final ConfigurationProperties annotation;

	private final Bindable<?> bindTarget;

	private ConfigurationPropertiesBean(String name, Object instance, ConfigurationProperties annotation,
			Bindable<?> bindTarget) {
		this.name = name;
		this.instance = instance;
		this.annotation = annotation;
		this.bindTarget = bindTarget;
	}

	/**
	 * Return the name of the Spring bean.
	 * @return the bean name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the actual Spring bean instance.
	 * @return the bean instance
	 */
	public Object getInstance() {
		return this.instance;
	}

	/**
	 * Return the {@link ConfigurationProperties} annotation for the bean. The annotation
	 * may be defined on the bean itself or from the factory method that create the bean
	 * (usually a {@link Bean @Bean} method).
	 * @return the configuration properties annotation
	 */
	public ConfigurationProperties getAnnotation() {
		return this.annotation;
	}

	/**
	 * Return a {@link Bindable} instance suitable that can be used as a target for the
	 * {@link Binder}.
	 * @return a bind target for use with the {@link Binder}
	 */
	public Bindable<?> asBindTarget() {
		return this.bindTarget;
	}

	/**
	 * Return a {@link ConfigurationPropertiesBean @ConfigurationPropertiesBean} instance
	 * for the given bean details or {@code null} if the bean is not a
	 * {@link ConfigurationProperties @ConfigurationProperties} object. Annotations are
	 * considered both on the bean itself, as well as any factory method (for example a
	 * {@link Bean @Bean} method).
	 * @param applicationContext the source application context
	 * @param bean the bean to consider
	 * @param beanName the bean name
	 * @return a configuration properties bean or {@code null} if the neither the bean or
	 * factory method are annotated with
	 * {@link ConfigurationProperties @ConfigurationProperties}
	 */
	static ConfigurationPropertiesBean get(ApplicationContext applicationContext, Object bean, String beanName) {
		Method factoryMethod = findFactoryMethod(applicationContext, beanName);
		ConfigurationProperties annotation = getAnnotation(factoryMethod, bean.getClass(),
				ConfigurationProperties.class);
		if (annotation == null) {
			return null;
		}
		ResolvableType type = (factoryMethod != null) ? ResolvableType.forMethodReturnType(factoryMethod)
				: ResolvableType.forClass(bean.getClass());
		Validated validated = getAnnotation(factoryMethod, bean.getClass(), Validated.class);
		Annotation[] annotations = (validated != null) ? new Annotation[] { annotation, validated }
				: new Annotation[] { annotation };
		Bindable<?> bindTarget = Bindable.of(type).withAnnotations(annotations).withExistingValue(bean);
		return new ConfigurationPropertiesBean(beanName, bean, annotation, bindTarget);
	}

	/**
	 * Return all {@link ConfigurationProperties @ConfigurationProperties} beans contained
	 * in the given application context. Both directly annotated beans, as well as beans
	 * that have {@link ConfigurationProperties @ConfigurationProperties} annotated
	 * factory methods are included.
	 * @param applicationContext the source application context
	 * @return a map of all configuration properties beans keyed by the bean name
	 */
	public static Map<String, ConfigurationPropertiesBean> getAll(ApplicationContext applicationContext) {

		// private Map<String, Object> getConfigurationPropertiesBeans(ApplicationContext
		// context,
		// ConfigurationBeanFactoryMetadata beanFactoryMetadata) {
		// Map<String, Object> beans = new
		// HashMap<>(context.getBeansWithAnnotation(ConfigurationProperties.class));
		// if (beanFactoryMetadata != null) {
		// beans.putAll(beanFactoryMetadata.getBeansWithFactoryAnnotation(ConfigurationProperties.class));
		// }
		// return beans;
		// }

		throw new IllegalStateException();

	}

	private static Method findFactoryMethod(ApplicationContext applicationContext, String beanName) {
		if (applicationContext instanceof ConfigurableApplicationContext) {
			return findFactoryMethod((ConfigurableApplicationContext) applicationContext, beanName);
		}
		return null;
	}

	private static Method findFactoryMethod(ConfigurableApplicationContext applicationContext, String beanName) {
		ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
		if (beanFactory.containsBeanDefinition(beanName)) {
			BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
			if (beanDefinition instanceof RootBeanDefinition) {
				return ((RootBeanDefinition) beanDefinition).getResolvedFactoryMethod();
			}
		}
		return null;
	}

	private static <A extends Annotation> A getAnnotation(Method factoryMethod, Class<?> beanType,
			Class<A> annotationType) {
		A annotation = (factoryMethod != null) ? AnnotationUtils.findAnnotation(factoryMethod, annotationType) : null;
		if (annotation == null) {
			annotation = AnnotationUtils.findAnnotation(beanType, annotationType);
		}
		return annotation;
	}

}
