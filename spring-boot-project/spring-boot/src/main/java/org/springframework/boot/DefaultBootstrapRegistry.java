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
import java.util.function.Supplier;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link BootstrapRegistry}.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public class DefaultBootstrapRegistry implements BootstrapRegistry {

	private final Map<Class<?>, Registration<?>> registrations = new HashMap<>();

	private final Map<Class<?>, Object> instances = new HashMap<>();

	private final Set<ApplicationContextPreparedListener> applicationContextPreparedListeners = new CopyOnWriteArraySet<>();

	@Override
	public <T> Supplier<T> register(Class<T> type, Registration<T> registration) {
		return register(type, registration, true);
	}

	@Override
	public <T> Supplier<T> registerIfAbsent(Class<T> type, Registration<T> registration) {
		return register(type, registration, false);
	}

	private <T> Supplier<T> register(Class<T> type, Registration<T> registration, boolean replaceExisting) {
		Assert.notNull(type, "Type must not be null");
		Assert.notNull(registration, "Registration must not be null");
		synchronized (this.registrations) {
			boolean alreadyRegistered = this.registrations.containsKey(type);
			if (replaceExisting || !alreadyRegistered) {
				Assert.state(!this.instances.containsKey(type), () -> type.getName() + " has already been created");
				this.registrations.put(type, registration);
			}
		}
		return () -> get(type);
	}

	@Override
	public <T> boolean isRegistered(Class<T> type) {
		synchronized (this.registrations) {
			return this.registrations.containsKey(type);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Registration<T> getRegistration(Class<T> type) {
		synchronized (this.registrations) {
			return (Registration<T>) this.registrations.get(type);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> type) {
		synchronized (this.registrations) {
			Registration<?> registration = this.registrations.get(type);
			Assert.state(registration != null, () -> type.getName() + " has not been registered");
			return (T) this.instances.computeIfAbsent(type, (k) -> registration.createInstance(this));
		}
	}

	@Override
	public void addApplicationContextPreparedListener(ApplicationContextPreparedListener listener) {
		this.applicationContextPreparedListeners.add(listener);
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
