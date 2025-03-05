/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.interfaceclients.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.web.client.support.RestClientHttpServiceGroup;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.client.support.RestClientHttpServiceProxyRegistry;

// TODO - Boot: add separate packages for RestClient and WebClient based implementations?
// TODO: handle AOT
// TODO: fix packaging

/**
 * @author Olga Maciaszek-Sharma
 */
public class HttpInterfaceClientsRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		Assert.isInstanceOf(ListableBeanFactory.class, registry,
				"Registry must be an instance of " + ListableBeanFactory.class.getSimpleName());
		ListableBeanFactory beanFactory = (ListableBeanFactory) registry;

		// TODO: support both RestClient and WebClient registries at different levels
		RestClientHttpServiceProxyRegistry interfaceClientRegistry = beanFactory
			.getBean(RestClientHttpServiceProxyRegistry.class);

		// TODO: also support configuring proxy factory
		beanFactory.getBeansOfType(RestClientHttpServiceGroupConfigurer.class)
			.values()
			.forEach(interfaceClientRegistry::apply);

		Map<String, Set<MergedAnnotation<InterfaceClientGroup>>> annotationsMap = getAnnotations(beanFactory, registry);

		registerBeanDefinitions(registry, annotationsMap, interfaceClientRegistry);
	}

	private void registerBeanDefinitions(BeanDefinitionRegistry registry,
			Map<String, Set<MergedAnnotation<InterfaceClientGroup>>> annotationsMap,
			RestClientHttpServiceProxyRegistry interfaceClientRegistry) {

		addClientGroups(annotationsMap, interfaceClientRegistry);

		for (Map.Entry<String, RestClientHttpServiceGroup> entry : interfaceClientRegistry.getGroups().entrySet()) {
			String name = entry.getKey();
			RestClientHttpServiceGroup group = entry.getValue();
			for (Class<?> httpServiceType : group.httpServiceTypes()) {
				// TODO: improve bean naming:
				// - better handle missing group names (set to null and check
				// for that while constructing group lookup in registry) and
				// name clashes (use just simple name to begin with, but proactively use
				// a more advanced naming strategy: groupName + FQN if required)
				String beanName = name + httpServiceType.getSimpleName();
				registerBeanDefinitions(registry, beanName, httpServiceType,
						() -> group.getClientProxy(httpServiceType));
			}
		}
	}

	private void addClientGroups(Map<String, Set<MergedAnnotation<InterfaceClientGroup>>> annotationsMap,
			RestClientHttpServiceProxyRegistry interfaceClientRegistry) {
		for (String key : annotationsMap.keySet()) {
			Set<MergedAnnotation<InterfaceClientGroup>> annotations = annotationsMap.get(key);
			for (MergedAnnotation<InterfaceClientGroup> annotation : annotations) {
				Class<?>[] serviceTypes = annotation.getClassArray("httpServiceTypes");

				interfaceClientRegistry.registerGroup(annotation.getString(MergedAnnotation.VALUE), group -> {
					group.addHttpServiceTypes(serviceTypes);
					group.detectHttpServiceTypes(annotation.getStringArray("basePackages"));
					group.detectHttpServiceTypes(annotation.getClassArray("basePackageClasses"));
				});
			}
		}
	}

	private <T> void registerBeanDefinitions(BeanDefinitionRegistry registry, String beanName, Class<?> beanClass,
			Supplier<T> instanceSupplier) {
		BeanDefinition definition = BeanDefinitionBuilder
			.rootBeanDefinition(ResolvableType.forClass(beanClass), instanceSupplier)
			.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE)
			.getBeanDefinition();
		BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, beanName);
		BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
	}

	private static Map<String, Set<MergedAnnotation<InterfaceClientGroup>>> getAnnotations(
			ListableBeanFactory beanFactory, BeanDefinitionRegistry registry) {
		String[] annotatedBeanNames = beanFactory.getBeanNamesForAnnotation(InterfaceClientGroup.class);
		Map<String, Set<MergedAnnotation<InterfaceClientGroup>>> annotations = new HashMap<>();
		for (String beanName : annotatedBeanNames) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
			Assert.isInstanceOf(AnnotatedBeanDefinition.class, beanDefinition);
			AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) beanDefinition;
			AnnotationMetadata metadata = annotatedBeanDefinition.getMetadata();
			Set<MergedAnnotation<InterfaceClientGroup>> annotationSet = new HashSet<>();
			MergedAnnotation<EnableInterfaceClients> containerAnnotation = annotatedBeanDefinition.getMetadata()
				.getAnnotations()
				.get(EnableInterfaceClients.class);
			if (containerAnnotation.isPresent()) {
				Collections.addAll(annotationSet,
						containerAnnotation.getAnnotationArray(MergedAnnotation.VALUE, InterfaceClientGroup.class));
			}
			MergedAnnotation<InterfaceClientGroup> annotation = annotatedBeanDefinition.getMetadata()
				.getAnnotations()
				.get(InterfaceClientGroup.class);
			if (annotation.isPresent()) {
				annotationSet.add(annotation);
			}
			annotations.put(metadata.getClassName(), annotationSet);
		}
		return annotations;
	}

}
