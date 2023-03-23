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

import java.util.HashMap;
import java.util.LinkedHashMap;

import org.springframework.util.Assert;

/**
 * {@link SslBundles} backed by a mutable collection that allows bundles to be registered.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
public class SslBundleRegistry implements SslBundles {

	private final HashMap<String, SslBundle> bundles = new LinkedHashMap<>();

	/**
	 * Register a named {@link SslBundle}.
	 * @param name the bundle name
	 * @param bundle the bundle
	 */
	public void registerBundle(String name, SslBundle bundle) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(bundle, "Bundle must not be null");
		this.bundles.put(name, bundle);
	}

	/**
	 * Return the named {@link SslBundle}.
	 * @param name the bundle name
	 * @return the bundle
	 * @throws NoSuchSslBundleException if a bundle with the provided name does not exist
	 */
	@Override
	public SslBundle getBundle(String name) {
		Assert.notNull(name, "Name must not be null");
		SslBundle sslBundle = this.bundles.get(name);
		if (sslBundle == null) {
			throw new NoSuchSslBundleException("SSL bundle name '" + name + "' is not valid");
		}
		return sslBundle;
	}

}
