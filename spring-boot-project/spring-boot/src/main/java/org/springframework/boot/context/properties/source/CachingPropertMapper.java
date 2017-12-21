/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties.source;

import java.util.List;

/**
 * {@link PropertyMapper} decorator that applies simple caching.
 *
 * @author Phillip Webb
 */
class CachingPropertMapper implements PropertyMapper {

	private final PropertyMapper delegate;

	private Cache<ConfigurationPropertyName> configurationPropertyNameCache;

	private Cache<String> propertySourceNameCache;

	CachingPropertMapper(PropertyMapper delegate) {
		this.delegate = delegate;
	}

	@Override
	public List<PropertyMapping> map(
			ConfigurationPropertyName configurationPropertyName) {
		Cache<ConfigurationPropertyName> cache = this.configurationPropertyNameCache;
		if (cache != null && cache.isFor(configurationPropertyName)) {
			return cache.getResult();
		}
		List<PropertyMapping> result = this.delegate.map(configurationPropertyName);
		this.configurationPropertyNameCache = new Cache<>(configurationPropertyName,
				result);
		return result;
	}

	@Override
	public List<PropertyMapping> map(String propertySourceName) {
		Cache<String> cache = this.propertySourceNameCache;
		if (cache != null && cache.isFor(propertySourceName)) {
			return cache.getResult();
		}
		List<PropertyMapping> result = this.delegate.map(propertySourceName);
		this.propertySourceNameCache = new Cache<>(propertySourceName, result);
		return result;
	}

	private static class Cache<T> {

		private final T name;

		private final List<PropertyMapping> result;

		public Cache(T name, List<PropertyMapping> result) {
			this.name = name;
			this.result = result;
		}

		public boolean isFor(T name) {
			return this.name.equals(name);
		}

		public List<PropertyMapping> getResult() {
			return this.result;
		}

	}

}
