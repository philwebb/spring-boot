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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;

/**
 * Default {@link ConfigurableBootstrapContext} implementation.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public class DefaultBootstrapContext implements ConfigurableBootstrapContext {

	private final Map<Class<?>, InstanceSupplier<?>> instanceSuppliers = new HashMap<>();

	private final Map<Class<?>, Object> instances = new HashMap<>();

	private final Set<ApplicationContextPreparedListener> applicationContextPreparedListeners = new CopyOnWriteArraySet<>();

	@Override
	public <T> void register(Class<T> type, InstanceSupplier<T> instanceSupplier) {
		register(type, instanceSupplier, true);
	}

	@Override
	public <T> void registerIfAbsent(Class<T> type, InstanceSupplier<T> instanceSupplier) {
		register(type, instanceSupplier, false);
	}

	private <T> void register(Class<T> type, InstanceSupplier<T> instanceSupplier, boolean replaceExisting) {
		Assert.notNull(type, "Type must not be null");
		Assert.notNull(instanceSupplier, "InstanceSupplier must not be null");
		synchronized (this.instanceSuppliers) {
			boolean alreadyRegistered = this.instanceSuppliers.containsKey(type);
			if (replaceExisting || !alreadyRegistered) {
				Assert.state(!this.instances.containsKey(type), () -> type.getName() + " has already been created");
				this.instanceSuppliers.put(type, instanceSupplier);
			}
		}
	}

	@Override
	public <T> boolean isRegistered(Class<T> type) {
		synchronized (this.instanceSuppliers) {
			return this.instanceSuppliers.containsKey(type);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> InstanceSupplier<T> getRegisteredInstanceSupplier(Class<T> type) {
		synchronized (this.instanceSuppliers) {
			return (InstanceSupplier<T>) this.instanceSuppliers.get(type);
		}
	}

	@Override
	public void addApplicationContextPreparedListener(ApplicationContextPreparedListener listener) {
		this.applicationContextPreparedListeners.add(listener);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> type) throws IllegalStateException {
		synchronized (this.instanceSuppliers) {
			InstanceSupplier<?> instanceSupplier = this.instanceSuppliers.get(type);
			Assert.state(instanceSupplier != null, () -> type.getName() + " has not been registered");
			return (T) this.instances.computeIfAbsent(type, (k) -> instanceSupplier.get(this));
		}
	}

	/**
	 * Method to be called when the {@link ApplicationContext} is prepared.
	 * @param applicationContext the prepared context
	 */
	public void applicationContextPrepared(ConfigurableApplicationContext applicationContext) {
		for (ApplicationContextPreparedListener listener : this.applicationContextPreparedListeners) {
			listener.onApplicationContextPrepared(this, applicationContext);
		}
	}

}
