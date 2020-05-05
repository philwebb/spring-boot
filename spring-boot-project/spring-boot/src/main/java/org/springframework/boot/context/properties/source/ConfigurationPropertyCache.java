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
import java.util.function.Supplier;

import org.springframework.core.env.PropertySource;

/**
 * Utility that to control the caches used by {@link ConfigurationPropertySource}
 * implementations. Can be used to configure a global time-to-live based cache or clear
 * existing caches if a property source is known to have changed.
 * <p>
 * Can also be used to start thread-local caching for a specific period of time.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public final class ConfigurationPropertyCache {

	private static final ConfigurationPropertyCacheDelegate delegate = new ConfigurationPropertyCacheDelegate(
			Clock.systemUTC());

	private ConfigurationPropertyCache() {
	}

	/**
	 * Update the global cache time-to-live.
	 * @param timeToLive the time to live value or {@code null} if the global cache is
	 * disabled.
	 */
	public static void setTimeToLive(Duration timeToLive) {
		delegate.setTimeToLive(timeToLive);
	}

	/**
	 * Run the given action with thread local caching enabled for the duration.
	 * @param action the action to run
	 */
	public static void withThreadLocalCache(Runnable action) {
		delegate.withThreadLocalCache(action);
	}

	/**
	 * Run with thread local caching, enabled until the last
	 * {@link ThreadLocalCaching#close()} is called.
	 * @return a {@link ThreadLocalCaching} instance that <em>must</em> be closed when
	 * thread local caching has finished
	 */
	public static ThreadLocalCaching withThreadLocalCache() {
		return delegate.withThreadLocalCache();
	}

	public static void clear() {
		delegate.clear();
	}

	public static void clear(PropertySource<?> propertySource) {
		delegate.clear(propertySource);
	}

	static <T> T get(ConfigurationPropertySource source, Class<T> type, Supplier<T> factory) {
		return delegate.get(source, type, factory);
	}

	/**
	 * Interface used to control thread local caching.
	 */
	public interface ThreadLocalCaching extends AutoCloseable {

		@Override
		void close();

	}

}
