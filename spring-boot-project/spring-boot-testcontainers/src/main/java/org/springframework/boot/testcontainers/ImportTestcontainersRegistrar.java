/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.testcontainers;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.testcontainers.containers.Container;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} for
 * {@link ImportTestcontainers @ImportTestcontainers}.
 *
 * @author Phillip Webb
 */
class ImportTestcontainersRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry,
			BeanNameGenerator importBeanNameGenerator) {
		MergedAnnotation<ImportTestcontainers> annotation = importingClassMetadata.getAnnotations()
			.get(ImportTestcontainers.class);
		Class<?>[] classes = annotation.getClassArray(MergedAnnotation.VALUE);
		if (ObjectUtils.isEmpty(classes)) {
			Class<?> importingClass = ClassUtils.resolveClassName(importingClassMetadata.getClassName(), null);
			classes = new Class<?>[] { importingClass };
		}
		registerBeanDefinitions(registry, importBeanNameGenerator, classes);
	}

	private void registerBeanDefinitions(BeanDefinitionRegistry registry, BeanNameGenerator importBeanNameGenerator,
			Class<?>[] classes) {
		for (Class<?> containersClass : classes) {
			for (Field field : getContainerFields(containersClass)) {
				Container<?> container = getContainer(field);
				registerBeanDefinition(registry, importBeanNameGenerator, field, container);
			}
		}
	}

	private List<Field> getContainerFields(Class<?> containersClass) {
		List<Field> containerFields = new ArrayList<>();
		ReflectionUtils.doWithFields(containersClass, containerFields::add, this::isContainerField);
		Assert.state(!containerFields.isEmpty(),
				() -> "%s does not container any container fields".formatted(containersClass.getName()));
		List<String> nonStaticFields = containerFields.stream()
			.filter((field) -> !Modifier.isStatic(field.getModifiers()))
			.map(Field::getName)
			.toList();
		Assert.state(nonStaticFields.isEmpty(),
				"%s declares non-static container fields %s".formatted(containersClass.getName(), nonStaticFields));
		return List.copyOf(containerFields);
	}

	private boolean isContainerField(Field candidate) {
		return Container.class.isAssignableFrom(candidate.getType());
	}

	private Container<?> getContainer(Field field) {
		ReflectionUtils.makeAccessible(field);
		Container<?> container = (Container<?>) ReflectionUtils.getField(field, null);
		Assert.state(container != null, "%s declares the null container field [%s]"
			.formatted(field.getDeclaringClass().getName(), field.getName()));
		return container;
	}

	private void registerBeanDefinition(BeanDefinitionRegistry registry, BeanNameGenerator importBeanNameGenerator,
			Field field, Container<?> container) {
		TestcontainerFieldBeanDefinition beanDefinition = new TestcontainerFieldBeanDefinition(field, container);
		String beanName = importBeanNameGenerator.generateBeanName(beanDefinition, registry);
		registry.registerBeanDefinition(beanName, beanDefinition);
	}

	/**
	 * {@link RootBeanDefinition} used for testcontainer bean definitions.
	 */
	private static class TestcontainerFieldBeanDefinition extends RootBeanDefinition
			implements TestcontainerBeanDefinition {

		private final Container<?> container;

		private final MergedAnnotations annotations;

		TestcontainerFieldBeanDefinition(Field field, Container<?> container) {
			this.container = container;
			this.annotations = MergedAnnotations.from(field);
			this.setBeanClass(container.getClass());
			setInstanceSupplier(() -> container);
		}

		@Override
		public String getDockerImageName() {
			return this.container.getDockerImageName();
		}

		@Override
		public MergedAnnotations getAnnotations() {
			return this.annotations;
		}

	}

}
