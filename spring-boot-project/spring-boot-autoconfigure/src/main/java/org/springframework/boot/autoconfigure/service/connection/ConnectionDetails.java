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

package org.springframework.boot.autoconfigure.service.connection;

import java.util.function.Supplier;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.origin.OriginProvider;

/**
 * Base interface for types that provide the details required to establish a connection to
 * a remote service.
 * <p>
 * Implementation classes can also implement {@link OriginProvider} in order to provide
 * origin information.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public interface ConnectionDetails {

	/**
	 * Register this instance as a bean in the given {@link BeanDefinitionRegistry}.
	 * @param <T> the connection details type
	 * @param registry the bean definition registry
	 * @param beanName the name of the bean instance to register
	 */
	@SuppressWarnings("unchecked")
	default <T> void register(BeanDefinitionRegistry registry, String beanName) {
		Class<T> beanType = (Class<T>) getClass();
		Supplier<T> beanSupplier = () -> (T) this;
		BeanDefinition beanDefinition = new RootBeanDefinition(beanType, beanSupplier);
		registry.registerBeanDefinition(beanName, beanDefinition);
	}

}
