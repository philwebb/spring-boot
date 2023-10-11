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

package org.springframework.boot.ssl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;

/**
 * Default {@link SslBundleRegistry} implementation.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @since 3.1.0
 */
public class DefaultSslBundleRegistry implements SslBundleRegistry, SslBundles {

	private static final Log logger = LogFactory.getLog(DefaultSslBundleRegistry.class);

	private final Map<String, SslBundle> bundles = new ConcurrentHashMap<>();

	private final Map<String, List<Consumer<SslBundle>>> listeners = new ConcurrentHashMap<>();

	private final Set<String> bundlesWithoutListeners = ConcurrentHashMap.newKeySet();

	public DefaultSslBundleRegistry() {
	}

	public DefaultSslBundleRegistry(String name, SslBundle bundle) {
		registerBundle(name, bundle);
	}

	@Override
	public void registerBundle(String name, SslBundle bundle) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(bundle, "Bundle must not be null");
		SslBundle previous = this.bundles.putIfAbsent(name, bundle);
		Assert.state(previous == null, () -> "Cannot replace existing SSL bundle '%s'".formatted(name));
	}

	@Override
	public void updateBundle(String name, SslBundle sslBundle) {
		Assert.notNull(name, "Name must not be null");
		// FIXME I think this isn't thread safe since someone could do a put after we do
		// the get
		SslBundle bundle = this.bundles.get(name);
		if (bundle == null) {
			throw new NoSuchSslBundleException(name, "SSL bundle name '%s' cannot be found".formatted(name));
		}
		this.bundles.put(name, sslBundle);
		notifyListeners(name, sslBundle);
		logMissingListeners(name);
	}

	private void notifyListeners(String name, SslBundle sslBundle) {
		List<Consumer<SslBundle>> listeners = this.listeners.getOrDefault(name, Collections.emptyList());
		for (Consumer<SslBundle> listener : listeners) {
			listener.accept(sslBundle);
		}
	}

	private void logMissingListeners(String name) {
		if (logger.isWarnEnabled()) {
			if (this.bundlesWithoutListeners.contains(name)) {
				logger.warn(LogMessage.format("SSL bundle '%s' has been updated, but not all consumers are updatable",
						name));
			}
		}
	}

	@Override
	public SslBundle getBundle(String name) {
		return getBundle(name, null);
	}

	private SslBundle getBundle(String name, Consumer<SslBundle> onUpdate) throws NoSuchSslBundleException {
		Assert.notNull(name, "Name must not be null");
		SslBundle bundle = this.bundles.get(name);
		if (bundle == null) {
			throw new NoSuchSslBundleException(name, "SSL bundle name '%s' cannot be found".formatted(name));
		}
		addListener(name, onUpdate);
		return bundle;
	}

	@Override
	public void addBundleUpdateHandler(String bundleName, Consumer<SslBundle> bundleUpdateHandler)
			throws NoSuchSslBundleException {
		getBundle(bundleName, bundleUpdateHandler);
	}

	private void addListener(String name, Consumer<SslBundle> onUpdate) {
		if (onUpdate == null) {
			this.bundlesWithoutListeners.add(name);
		}
		else {
			this.listeners.computeIfAbsent(name, (ignore) -> new CopyOnWriteArrayList<>()).add(onUpdate);
		}
	}

}
