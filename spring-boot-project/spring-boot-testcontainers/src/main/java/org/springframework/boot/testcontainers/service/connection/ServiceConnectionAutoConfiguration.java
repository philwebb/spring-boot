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

package org.springframework.boot.testcontainers.service.connection;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.testcontainers.containers.Container;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactories;
import org.springframework.boot.origin.Origin;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for {@link ServiceConnection @ServiceConnection} annotated
 * {@link Container} beans.
 *
 * @author Phillip Webb
 * @since 3.1.0
 */
@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Import(ServiceConnectionAutoConfiguration.Registrar.class)
public class ServiceConnectionAutoConfiguration {

	ServiceConnectionAutoConfiguration() {
	}

	static class Registrar implements ImportBeanDefinitionRegistrar {

		private final Environment environment;

		private final BeanFactory beanFactory;

		Registrar(Environment environment, BeanFactory beanFactory) {
			this.environment = environment;
			this.beanFactory = beanFactory;
		}

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			if (this.beanFactory instanceof ConfigurableListableBeanFactory listableBeanFactory) {
				registerBeanDefinitions(listableBeanFactory, registry);
			}
		}

		private void registerBeanDefinitions(ConfigurableListableBeanFactory beanFactory,
				BeanDefinitionRegistry registry) {
			ContainerConnectionSourceRegistrar connectionSourceRegistrar = new ContainerConnectionSourceRegistrar(
					beanFactory, new ConnectionDetailsFactories());
			for (String beanName : beanFactory.getBeanNamesForType(Container.class)) {
				for (ServiceConnection annotation : beanFactory.findAllAnnotationsOnBean(beanName,
						ServiceConnection.class, false)) {
					connectionSourceRegistrar.registerBeanDefinitions(registry,
							createContainerConnectionSource(beanFactory, beanName, annotation));
				}
			}
		}

		@SuppressWarnings("unchecked")
		private <C extends Container<?>> ContainerConnectionSource<C> createContainerConnectionSource(
				ConfigurableListableBeanFactory beanFactory, String beanName, ServiceConnection annotation) {
			RegisteredBean registeredBean = RegisteredBean.of(beanFactory, beanName);
			Origin origin = new BeanOrigin(beanName, registeredBean.getMergedBeanDefinition());
			OptionalContainer container = getContainerIfPossible(registeredBean);
			Class<C> containerType = getContainerType(registeredBean, container);
			return new ContainerConnectionSource<>(beanName, origin, containerType, container::getContainerName,
					annotation, () -> (C) beanFactory.getBean(beanName));
		}

		@SuppressWarnings("unchecked")
		private <C extends Container<?>> Class<C> getContainerType(RegisteredBean registeredBean,
				OptionalContainer container) {
			Class<?> type = (!container.isAvailable()) ? registeredBean.getBeanType().resolve()
					: container.get().getClass();
			Assert.isAssignable(Container.class, type);
			return (Class<C>) type;
		}

		private OptionalContainer getContainerIfPossible(RegisteredBean registeredBean) {
			try {
				Method beanFactoryMethod = getBeanFactoryMethod(registeredBean);
				Assert.state(beanFactoryMethod != null, () -> "Unable to find factory method");
				Assert.state(Modifier.isStatic(beanFactoryMethod.getModifiers()),
						() -> "Factory method must be static");
				ReflectionUtils.makeAccessible(beanFactoryMethod);
				Object[] args = resolveArgs(beanFactoryMethod.getParameterTypes());
				Object result = ReflectionUtils.invokeMethod(beanFactoryMethod, null, args);
				Assert.state(Container.class.isInstance(result), () -> "Factory method did not return a container");
				return OptionalContainer.of(registeredBean, (Container<?>) result);
			}
			catch (Exception ex) {
				return OptionalContainer.unavailable(registeredBean, ex);
			}
		}

		private Object[] resolveArgs(Class<?>[] parameterTypes) {
			if (parameterTypes.length == 0) {
				return new Object[] {};
			}
			if (parameterTypes.length == 1 && parameterTypes[0].isInstance(this.environment)) {
				return new Object[] { this.environment };
			}
			throw new IllegalStateException("Factory method has unsupported parameter types");
		}

		private Method getBeanFactoryMethod(RegisteredBean registeredBean) {
			Method resolvedFactoryMethod = registeredBean.getMergedBeanDefinition().getResolvedFactoryMethod();
			if (resolvedFactoryMethod != null) {
				return resolvedFactoryMethod;
			}
			Executable resolveConstructorOrFactoryMethod = registeredBean.resolveConstructorOrFactoryMethod();
			if (resolveConstructorOrFactoryMethod instanceof Method method) {
				return method;
			}
			return null;
		}

	}

	private static class OptionalContainer {

		private final RegisteredBean registeredBean;

		private final Container<?> container;

		private final Exception cause;

		private OptionalContainer(RegisteredBean registeredBean, Container<?> container, Exception cause) {
			this.registeredBean = registeredBean;
			this.container = container;
			this.cause = cause;
		}

		boolean isAvailable() {
			return this.container != null;
		}

		public Container<?> get() {
			if (this.container == null) {
				String message = "Unable to get container for bean '" + this.registeredBean.getBeanName() + "'";
				throw new IllegalStateException(message, this.cause);
			}
			return this.container;
		}

		public String getContainerName() {
			if (this.container == null) {
				String message = "Unable to get container name for bean '" + this.registeredBean.getBeanName()
						+ "'. Please update the @Bean method or use the 'name' attribute of @ServiceConnection";
				throw new IllegalStateException(message, this.cause);
			}
			return this.container.getDockerImageName();
		}

		static OptionalContainer of(RegisteredBean registeredBean, Container<?> container) {
			return new OptionalContainer(registeredBean, container, null);
		}

		static OptionalContainer unavailable(RegisteredBean registeredBean, Exception cause) {
			return new OptionalContainer(registeredBean, null, cause);
		}

	}

}
