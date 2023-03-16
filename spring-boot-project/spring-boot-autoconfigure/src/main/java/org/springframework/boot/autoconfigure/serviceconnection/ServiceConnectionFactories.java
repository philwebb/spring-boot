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

package org.springframework.boot.autoconfigure.serviceconnection;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * A registry of {@link ServiceConnectionFactory} instances.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public class ServiceConnectionFactories {

	private List<RegisteredFactory> registeredFactories = new ArrayList<>();

	public ServiceConnectionFactories() {
		this(SpringFactoriesLoader.forDefaultResourceLocation(ServiceConnectionFactories.class.getClassLoader()));
	}

	@SuppressWarnings("rawtypes")
	ServiceConnectionFactories(SpringFactoriesLoader loader) {
		List<ServiceConnectionFactory> factories = loader.load(ServiceConnectionFactory.class);
		for (ServiceConnectionFactory<?, ?> factory : factories) {
			ResolvableType type = ResolvableType.forClass(factory.getClass());
			try {
				ResolvableType[] interfaces = type.getInterfaces();
				for (ResolvableType iface : interfaces) {
					if (iface.getRawClass().equals(ServiceConnectionFactory.class)) {
						ResolvableType input = iface.getGeneric(0);
						ResolvableType output = iface.getGeneric(1);
						registerFactory(input.getRawClass(), output.getRawClass(), factory);
					}
				}
			}
			catch (TypeNotPresentException ex) {
				// A type referenced by the factory is not present. Skip it.
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <I, SC extends ServiceConnection> void registerFactory(Class<?> input, Class<?> connectionType,
			ServiceConnectionFactory<?, ?> factory) {
		addFactory((Class<I>) input, (Class<SC>) connectionType, (ServiceConnectionFactory<I, SC>) factory);
	}

	private <I, SC extends ServiceConnection> void addFactory(Class<I> input, Class<SC> output,
			ServiceConnectionFactory<I, SC> factory) {
		this.registeredFactories.add(new RegisteredFactory(input, output, factory));
	}

	@SuppressWarnings("unchecked")
	public <I, SC extends ServiceConnection> ServiceConnectionFactory<I, SC> getFactory(
			ServiceConnectionSource<I, SC> source) {
		Class<I> input = (Class<I>) source.input().getClass();
		Class<SC> connectionType = source.connectionType();
		for (RegisteredFactory factory : this.registeredFactories) {
			if (factory.input.isAssignableFrom(input) && factory.output.isAssignableFrom(connectionType)) {
				return (ServiceConnectionFactory<I, SC>) factory.factory();
			}
		}
		throw new ServiceConnectionFactoryNotFoundException(source);
	}

	private record RegisteredFactory(Class<?> input, Class<? extends ServiceConnection> output,
			ServiceConnectionFactory<?, ?> factory) {
	}

}
