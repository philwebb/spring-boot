/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot;

import java.util.function.Supplier;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * A simple object registry that is available during startup and {@link Environment}
 * post-processing up to the point that the {@link ApplicationContext} is prepared. The
 * registry can be used to store instances that may be expensive to create, or need to be
 * shared before the {@link ApplicationContext} is available.
 * <p>
 * The registry uses the object type as a key, meaning that only a single instance of a
 * given type can be stored.
 * <p>
 * The {@link #addApplicationContextPreparedListener(ApplicationContextPreparedListener)}
 * method can be used to add a listener that can perform actions when the
 * {@link ApplicationContext} is fully prepared. For example, an instance may choose to
 * register itself as a regular Spring bean so that it is available for the application to
 * use.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public interface BootstrapRegistry {

	/**
	 * Register a specific type with the registry. If the specified type has already been
	 * registered, but not get obtained, it will be replaced.
	 * @param <T> the instance type
	 * @param type the instance type
	 * @param registration the registration
	 * @return a supplier equivalent to {@link #get(Class) get(type)}
	 */
	<T> Supplier<T> register(Class<T> type, Registration<T> registration);

	/**
	 * Register a specific type with the registry if one is not already present.
	 * @param <T> the instance type
	 * @param type the instance type
	 * @param registration the registration
	 * @return a supplier equivalent to {@link #get(Class) get(type)}
	 */
	<T> Supplier<T> registerIfAbsent(Class<T> type, Registration<T> registration);

	/**
	 * Return if a registration exists for the given type.
	 * @param <T> the instance type
	 * @param type the instance type
	 * @return {@code true} if the type has already been registered
	 */
	<T> boolean isRegistered(Class<T> type);

	/**
	 * Return any existing {@link Registration} for the given type.
	 * @param <T> the instance type
	 * @param type the instance type
	 * @return the existing registration or {@code null}
	 */
	<T> Registration<T> getRegistration(Class<T> type);

	/**
	 * Get an in instance from the registry or throw an {@link IllegalStateException} if
	 * no instance has not been registered.
	 * @param <T> the instance type
	 * @param type the instance type
	 * @return the registered instance
	 */
	<T> T get(Class<T> type);

	/**
	 * Add a listener that will be called when the {@link ApplicationContext} is prepared.
	 * @param listener the listener to add
	 */
	void addApplicationContextPreparedListener(ApplicationContextPreparedListener listener);

	/**
	 * A single registration contained in the registry.
	 *
	 * @param <T> the instance type
	 */
	@FunctionalInterface
	interface Registration<T> {

		/**
		 * Factory method used to create the instance when needed.
		 * @param registry the source registry which may be used to obtain other
		 * registered instances.
		 * @return the instance
		 */
		T createInstance(BootstrapRegistry registry);

		/**
		 * Factory method that can be used to create a {@link Registration} for a given
		 * instance.
		 * @param <T> the instance type
		 * @param instance the instance
		 * @return a new {@link Registration}
		 */
		static <T> Registration<T> of(T instance) {
			return (registry) -> instance;
		}

		/**
		 * Factory method that can be used to create a {@link Registration} for a supplied
		 * instance.
		 * @param <T> the instance type
		 * @param supplier the supplier that will provide the instance
		 * @return a new {@link Registration}
		 */
		static <T> Registration<T> suppliedBy(Supplier<T> supplier) {
			return (registry) -> (supplier != null) ? supplier.get() : null;
		}

	}

	/**
	 * Listener to be called when the {@link ApplicationContext} has been prepared.
	 */
	@FunctionalInterface
	interface ApplicationContextPreparedListener {

		/**
		 * Called when the {@link ApplicationContext} has been prepared.
		 * @param bootstrapRegistry the bootstrap registry
		 * @param applicationContext the application context
		 */
		void onApplicationContextPrepared(BootstrapRegistry bootstrapRegistry,
				ConfigurableApplicationContext applicationContext);

	}

}
