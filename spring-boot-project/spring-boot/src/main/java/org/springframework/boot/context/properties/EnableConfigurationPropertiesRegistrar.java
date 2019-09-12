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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author Phillip Webb
 * @see EnableConfigurationProperties
 */
class EnableConfigurationPropertiesRegistrar implements ImportBeanDefinitionRegistrar {

	public void xregisterBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
		ConfigurationPropertiesBeanRegistrar registrar = new ConfigurationPropertiesBeanRegistrar(registry);
		getTypes(metadata).forEach(registrar::register);
	}

	private Set<Class<?>> getTypes(AnnotationMetadata metadata) {
		return metadata.getAnnotations().stream(EnableConfigurationProperties.class)
				.flatMap((annotation) -> Arrays.stream(annotation.getClassArray(MergedAnnotation.VALUE)))
				.filter((type) -> void.class != type).collect(Collectors.toSet());
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(ConfigurationPropertiesBinder.BEAN_NAME)) {
			registerConfigurationPropertiesBinder(registry);
		}
		if (!registry.containsBeanDefinition(ConfigurationPropertiesBindingPostProcessor.BEAN_NAME)) {
			registerConfigurationPropertiesBindingPostProcessor(registry);
			registerConfigurationBeanFactoryMetadata(registry);
		}
	}

	private void registerConfigurationPropertiesBinder(BeanDefinitionRegistry registry) {
		GenericBeanDefinition definition = new GenericBeanDefinition();
		definition.setBeanClass(ConfigurationPropertiesBinder.class);
		definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		definition.getConstructorArgumentValues().addIndexedArgumentValue(0,
				ConfigurationPropertiesBindingPostProcessor.VALIDATOR_BEAN_NAME);
		registry.registerBeanDefinition(ConfigurationPropertiesBinder.BEAN_NAME, definition);
	}

	private void registerConfigurationPropertiesBindingPostProcessor(BeanDefinitionRegistry registry) {
		GenericBeanDefinition definition = new GenericBeanDefinition();
		definition.setBeanClass(ConfigurationPropertiesBindingPostProcessor.class);
		definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(ConfigurationPropertiesBindingPostProcessor.BEAN_NAME, definition);
	}

	@SuppressWarnings("deprecation")
	private void registerConfigurationBeanFactoryMetadata(BeanDefinitionRegistry registry) {
		GenericBeanDefinition definition = new GenericBeanDefinition();
		definition.setBeanClass(ConfigurationBeanFactoryMetadata.class);
		definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(ConfigurationBeanFactoryMetadata.BEAN_NAME, definition);
	}

}
