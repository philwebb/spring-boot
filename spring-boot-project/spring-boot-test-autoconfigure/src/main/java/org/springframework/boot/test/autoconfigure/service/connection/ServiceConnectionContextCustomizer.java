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

package org.springframework.boot.test.autoconfigure.service.connection;

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactories;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * {@link ContextCustomizer} to support registering {@link ConnectionDetails}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class ServiceConnectionContextCustomizer implements ContextCustomizer {

	private final ConnectionDetailsFactories factories = new ConnectionDetailsFactories();

	private final List<?> sources;

	ServiceConnectionContextCustomizer(List<?> sources) {
		this.sources = sources;
	}

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (beanFactory instanceof BeanDefinitionRegistry registry) {
			registerServiceConnections(registry);
		}
	}

	private void registerServiceConnections(BeanDefinitionRegistry registry) {
		for (Object source : this.sources) {
			registerServiceConnection(registry, source);
		}
	}

	private <S> void registerServiceConnection(BeanDefinitionRegistry registry, S source) {
		ConnectionDetails connectionDetails = this.factories.getConnectionDetailsFactory(source)
			.getConnectionDetails(source);
		String beanName = "";
		registry.registerBeanDefinition(beanName, createBeanDefinition(connectionDetails));
	}

	@SuppressWarnings("unchecked")
	private <T> BeanDefinition createBeanDefinition(T instance) {
		return new RootBeanDefinition((Class<T>) instance.getClass(), () -> instance);
	}

}
