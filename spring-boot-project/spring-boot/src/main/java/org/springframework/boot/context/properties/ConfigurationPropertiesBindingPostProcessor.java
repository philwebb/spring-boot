/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.bind.handler.IgnoreErrorsBindHandler;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.bind.validation.ValidationBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.UnboundElementsSourceFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySources;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

/**
 * {@link BeanPostProcessor} to bind {@link PropertySources} to beans annotated with
 * {@link ConfigurationProperties}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Madhura Bhave
 */
public class ConfigurationPropertiesBindingPostProcessor
		implements BeanPostProcessor, PriorityOrdered {

	/**
	 * The bean name that this post-processor is registered with.
	 */
	public static final String BEAN_NAME = ConfigurationPropertiesBindingPostProcessor.class
			.getName();

	/**
	 * The bean name of the configuration properties validator.
	 */
	public static final String VALIDATOR_BEAN_NAME = "configurationPropertiesValidator";

	private final ApplicationContext applicationContext;

	private final ConfigurationBeanFactoryMetadata beanFactoryMetadata;

	private final PropertySources propertySources;

	private final Validator configurationPropertiesValidator;

	private final Validator jsr303Validator;

	private volatile Binder binder;

	public ConfigurationPropertiesBindingPostProcessor(Environment environment,
			ApplicationContext applicationContext, BeanFactory beanFactory,
			ConfigurationBeanFactoryMetadata beanFactoryMetadata) {
		this.beanFactoryMetadata = beanFactoryMetadata;
		this.applicationContext = applicationContext;
		this.propertySources = new PropertySourcesDeducer(environment, beanFactory)
				.getPropertySources();
		this.configurationPropertiesValidator = getConfigurationPropertiesValidator(
				applicationContext);
		this.jsr303Validator = ConfigurationPropertiesJsr303Validator
				.getIfJsr303Present(applicationContext);
	}

	private Validator getConfigurationPropertiesValidator(
			ApplicationContext applicationContext) {
		if (applicationContext.containsBean(VALIDATOR_BEAN_NAME)) {
			return applicationContext.getBean(VALIDATOR_BEAN_NAME, Validator.class);
		}
		return null;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 1;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		ConfigurationProperties annotation = getAnnotation(bean, beanName,
				ConfigurationProperties.class);
		if (annotation != null) {
			bind(bean, beanName, annotation);
		}
		return bean;
	}

	private void bind(Object bean, String beanName, ConfigurationProperties annotation) {
		ResolvableType type = getBeanType(bean, beanName);
		Bindable<?> target = Bindable.of(type).withExistingValue(bean);
		List<Validator> validators = getValidators(bean, beanName);
		BindHandler bindHandler = getBindHandler(annotation, validators);
		getBinder().bind(annotation.prefix(), target, bindHandler);
		// FIXME exception details
	}

	private ResolvableType getBeanType(Object bean, String beanName) {
		Method factoryMethod = this.beanFactoryMetadata.findFactoryMethod(beanName);
		if (factoryMethod != null) {
			return ResolvableType.forMethodReturnType(factoryMethod);
		}
		return ResolvableType.forClass(bean.getClass());
	}

	private List<Validator> getValidators(Object bean, String beanName) {
		List<Validator> validators = new ArrayList<>(3);
		if (this.configurationPropertiesValidator != null) {
			validators.add(this.configurationPropertiesValidator);
		}
		if (this.jsr303Validator != null
				&& getAnnotation(bean, beanName, Validated.class) != null) {
			validators.add(this.jsr303Validator);
		}
		if (bean instanceof Validator) {
			validators.add((Validator) bean);
		}
		return validators;
	}

	private BindHandler getBindHandler(ConfigurationProperties annotation,
			List<Validator> validators) {
		BindHandler handler = BindHandler.DEFAULT;
		if (annotation.ignoreInvalidFields()) {
			handler = new IgnoreErrorsBindHandler(handler);
		}
		if (!annotation.ignoreUnknownFields()) {
			UnboundElementsSourceFilter filter = new UnboundElementsSourceFilter();
			handler = new NoUnboundElementsBindHandler(handler, filter);
		}
		if (!validators.isEmpty()) {
			handler = new ValidationBindHandler(handler,
					validators.toArray(new Validator[validators.size()]));
		}
		return handler;
	}

	/*
	 * VALIDATOR_BEAN_NAME / Jsr303 (if @Valid) / The bean itself
	 */

	private <A extends Annotation> A getAnnotation(Object bean, String beanName,
			Class<A> type) {
		A annotation = this.beanFactoryMetadata.findFactoryAnnotation(beanName, type);
		if (annotation == null) {
			annotation = AnnotationUtils.findAnnotation(bean.getClass(), type);
		}
		return annotation;
	}

	private Binder getBinder() {
		if (this.binder == null) {
			this.binder = new Binder(
					ConfigurationPropertySources.from(this.propertySources),
					new PropertySourcesPlaceholdersResolver(this.propertySources),
					new ConversionServiceDeducer(this.applicationContext)
							.getConversionService());
		}
		return this.binder;
	}

}
