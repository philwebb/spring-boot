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

package org.springframework.boot.context.properties.source;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.boot.context.properties.source.ConfigurationPropertyCache.ThreadLocalCaching;
import org.springframework.core.env.PropertySource;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Delegate used to implement {@link ConfigurationPropertyCache} methods.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertyCacheDelegate {

	private volatile Clock clock;

	private volatile Duration timeToLive;

	private final Map<ConfigurationPropertySource, Values> globalCache = new ConcurrentReferenceHashMap<>();

	private final ThreadLocal<ThreadLocalCache> threadLocalCache = new ThreadLocal<>();

	ConfigurationPropertyCacheDelegate(Clock clock) {
		this.clock = clock;
	}

	void offsetClock(Duration tickDuration) {
		this.clock = Clock.offset(this.clock, tickDuration);
	}

	/**
	 * Update the global cache time-to-live.
	 * @param timeToLive the time to live value or {@code null} if the global cache is
	 * disabled.
	 */
	void setTimeToLive(Duration timeToLive) {
		this.timeToLive = timeToLive;
	}

	/**
	 * Run the given action with thread local caching enabled for the duration.
	 * @param action the action to run
	 */
	void withThreadLocalCache(Runnable action) {
		try (ThreadLocalCaching caching = withThreadLocalCache()) {
			action.run();
		}
	}

	/**
	 * Start thread local caching and keep it enabled until
	 * {@link ThreadLocalCaching#close()} is called.
	 * @return a {@link ThreadLocalCaching} instance that <em>must</em> be closed when
	 * thread local caching has finished
	 */
	ThreadLocalCaching withThreadLocalCache() {
		synchronized (this.threadLocalCache) {
			ThreadLocalCache cache = this.threadLocalCache.get();
			if (cache == null) {
				cache = new ThreadLocalCache();
				this.threadLocalCache.set(cache);
			}
			cache.aquire();
			return cache;
		}
	}

	void clear() {
		this.globalCache.clear();
	}

	void clear(PropertySource<?> propertySource) {
		for (ConfigurationPropertySource source : this.globalCache.keySet()) {
			if (isFromPropertySource(source, propertySource)) {
				this.globalCache.remove(source);
			}
		}
	}

	private boolean isFromPropertySource(ConfigurationPropertySource source, PropertySource<?> propertySource) {
		if (source instanceof SpringConfigurationPropertySource) {
			return ((SpringConfigurationPropertySource) source).getPropertySource() == propertySource;
		}
		return false;
	}

	<T> T get(ConfigurationPropertySource source, Class<T> type, Supplier<T> factory) {
		T value = getFromThreadLocalCache(source, type);
		if (value != null) {
			updateGlobalCache(source, type, value);
			return value;
		}
		value = getFromGlobalCache(source, type);
		if (value != null) {
			updateThreadLocalCache(source, type, value);
			return value;
		}
		value = factory.get();
		updateGlobalCache(source, type, value);
		updateThreadLocalCache(source, type, value);
		return value;
	}

	private <T> T getFromThreadLocalCache(ConfigurationPropertySource source, Class<T> type) {
		ThreadLocalCache threadLocalCache = this.threadLocalCache.get();
		Values values = (threadLocalCache != null) ? threadLocalCache.get(source) : null;
		return (values != null) ? values.get(type) : null;
	}

	private <T> void updateThreadLocalCache(ConfigurationPropertySource source, Class<T> type, T value) {
		ThreadLocalCache threadLocalCache = this.threadLocalCache.get();
		if (threadLocalCache != null) {
			Values values = threadLocalCache.getOrCreate(source);
			values.update(type, value);
		}
	}

	private <T> T getFromGlobalCache(ConfigurationPropertySource source, Class<T> type) {
		Duration timeToLive = this.timeToLive;
		Values values = (timeToLive != null) ? this.globalCache.get(source) : null;
		return (values != null) ? values.get(type, timeToLive) : null;
	}

	private <T> void updateGlobalCache(ConfigurationPropertySource source, Class<T> type, T value) {
		Duration timeToLive = this.timeToLive;
		if (timeToLive != null) {
			Values values = this.globalCache.computeIfAbsent(source, (key) -> new Values());
			values.update(type, value);
		}
	}

	/**
	 * A thread local cache.
	 */
	private class ThreadLocalCache implements ThreadLocalCaching {

		private int count;

		private final Map<ConfigurationPropertySource, Values> values = new HashMap<>();

		void aquire() {
			this.count++;
		}

		@Override
		public void close() {
			synchronized (ConfigurationPropertyCacheDelegate.this.threadLocalCache) {
				this.count--;
				if (this.count == 0) {
					ConfigurationPropertyCacheDelegate.this.threadLocalCache.set(null);
				}
			}
		}

		Values getOrCreate(ConfigurationPropertySource source) {
			return this.values.computeIfAbsent(source, (key) -> new Values());
		}

		Values get(ConfigurationPropertySource source) {
			return this.values.get(source);
		}

	}

	/**
	 * Values contained in the global or thread local cache.
	 */
	private class Values {

		private final Map<Class<?>, Object> values = new ConcurrentHashMap<>();

		private Instant lastAccessed = now();

		<T> T get(Class<T> type) {
			return get(type, null);
		}

		@SuppressWarnings("unchecked")
		<T> T get(Class<T> type, Duration timeToLive) {
			if (timeToLive != null) {
				Instant now = now();
				if (now.isAfter(this.lastAccessed.plus(timeToLive))) {
					this.values.remove(type);
					return null;
				}
			}
			return (T) this.values.get(type);
		}

		<T> void update(Class<T> type, T value) {
			this.values.put(type, value);
			this.lastAccessed = now();
		}

		private Instant now() {
			return ConfigurationPropertyCacheDelegate.this.clock.instant();
		}

	}

}
