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

import java.lang.ref.SoftReference;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * @author Phillip Webb
 * @param <T>
 */
class ConfigurationPropertyCache<T> implements ConfigurationPropertyCaching {

	private static final Duration UNLIMITED = Duration.ZERO;

	private final boolean alwaysEnabled;

	private volatile Duration timeToLive;

	private volatile SoftReference<T> value = new SoftReference<>(null);

	private volatile Instant lastAccessed = now();

	ConfigurationPropertyCache(boolean alwaysEnabled) {
		this.alwaysEnabled = alwaysEnabled;
	}

	@Override
	public void enable() {
		this.timeToLive = UNLIMITED;
	}

	@Override
	public void disable() {
		this.timeToLive = null;
	}

	@Override
	public void setTimeToLive(Duration timeToLive) {
		this.timeToLive = (timeToLive == null || timeToLive.isZero()) ? null : timeToLive;
	}

	@Override
	public void clear() {
		if (!this.alwaysEnabled) {
			this.value = new SoftReference<>(null);
		}
	}

	T get(Supplier<T> factory, UnaryOperator<T> updator) {
		T result = this.value.get();
		if (result == null) {
			result = updator.apply(factory.get());
			this.value = new SoftReference<>(result);
		}
		else if (hasExpired()) {
			result = updator.apply(result);
		}
		if (!this.alwaysEnabled) {
			this.lastAccessed = now();
		}
		return result;
	}

	private boolean hasExpired() {
		Duration timeToLive = this.timeToLive;
		Instant lastAccessed = this.lastAccessed;
		if (timeToLive == null) {
			return true;
		}
		if (this.alwaysEnabled || UNLIMITED.equals(timeToLive)) {
			return false;
		}
		return lastAccessed == null || now().isAfter(lastAccessed.plus(timeToLive));

	}

	protected Instant now() {
		return Instant.now();
	}

}
