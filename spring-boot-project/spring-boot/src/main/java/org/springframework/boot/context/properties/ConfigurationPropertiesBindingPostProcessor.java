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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.PropertySources;
import org.springframework.validation.annotation.Validated;

/**
 * {@link BeanPostProcessor} to bind {@link PropertySources} to beans annotated with
 * {@link ConfigurationProperties @ConfigurationProperties}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @since 1.0.0
 */
public class ConfigurationPropertiesBindingPostProcessor
		implements BeanPostProcessor, PriorityOrdered, ApplicationContextAware, InitializingBean {

	// FIXME revisit

	/**
	 * The bean name that this post-processor is registered with.
	 */
	public static final String BEAN_NAME = ConfigurationPropertiesBindingPostProcessor.class.getName();

	/**
	 * The bean name of the configuration properties validator.
	 */
	public static final String VALIDATOR_BEAN_NAME = "configurationPropertiesValidator";

	private ApplicationContext applicationContext;

	private Delegate delegate;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// We can't use constructor injection of the application context because
		// it causes eager factory bean initialization
		// FIXME double check this is still the case, at lease try to push into the setAC
		this.delegate = new Delegate(this.applicationContext);
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 1;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		this.delegate.bindIfNecessary(bean, beanName);
		return bean;
	}

	private static class Delegate {

		private final ApplicationContext applicationContext;

		private final ConfigurationPropertiesBinder configurationPropertiesBinder;

		private final BeanDefinitionRegistry registry;

		Delegate(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
			this.configurationPropertiesBinder = ConfigurationPropertiesBinder.get(applicationContext);
			this.registry = (BeanDefinitionRegistry) applicationContext.getAutowireCapableBeanFactory();
		}

		void bindIfNecessary(Object bean, String beanName) throws BeansException {
			ConfigurationProperties annotation = getAnnotation(bean, beanName, ConfigurationProperties.class);
			if (annotation != null && !hasBeenBoundAsValueObject(beanName)) {
				bind(bean, beanName, annotation);
			}
		}

		private boolean hasBeenBoundAsValueObject(String beanName) {
			if (this.registry.containsBeanDefinition(beanName)) {
				BeanDefinition beanDefinition = this.registry.getBeanDefinition(beanName);
				return beanDefinition instanceof ConfigurationPropertiesValueObjectBeanDefinition;
			}
			return false;
		}

		private void bind(Object bean, String beanName, ConfigurationProperties annotation) {
			// FIXME this needs to use ConfigurationPropertiesBean
			ResolvableType type = getBeanType(bean, beanName);
			Validated validated = getAnnotation(bean, beanName, Validated.class);
			Annotation[] annotations = (validated != null) ? new Annotation[] { annotation, validated }
					: new Annotation[] { annotation };
			Bindable<?> target = Bindable.of(type).withExistingValue(bean).withAnnotations(annotations);
			try {
				this.configurationPropertiesBinder.bind(target);
			}
			catch (Exception ex) {
				throw new ConfigurationPropertiesBindException(beanName, bean.getClass(), annotation, ex);
			}
		}

		private ResolvableType getBeanType(Object bean, String beanName) {
			Method factoryMethod = this.beanFactoryMetadata.findFactoryMethod(beanName);
			if (factoryMethod != null) {
				return ResolvableType.forMethodReturnType(factoryMethod);
			}
			return ResolvableType.forClass(bean.getClass());
		}

		private <A extends Annotation> A getAnnotation(Object bean, String beanName, Class<A> type) {
			A annotation = this.beanFactoryMetadata.findFactoryAnnotation(beanName, type);
			if (annotation == null) {
				annotation = AnnotationUtils.findAnnotation(bean.getClass(), type);
			}
			return annotation;
		}

	}

}
