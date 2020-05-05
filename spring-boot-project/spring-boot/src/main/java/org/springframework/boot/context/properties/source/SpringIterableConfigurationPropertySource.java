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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.PropertySourceOrigin;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link ConfigurationPropertySource} backed by an {@link EnumerablePropertySource}.
 * Extends {@link SpringConfigurationPropertySource} with full "relaxed" mapping support.
 * In order to use this adapter the underlying {@link PropertySource} must be fully
 * enumerable. A security restricted {@link SystemEnvironmentPropertySource} cannot be
 * adapted.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see PropertyMapper
 */
class SpringIterableConfigurationPropertySource extends SpringConfigurationPropertySource
		implements IterableConfigurationPropertySource {

	private volatile Collection<ConfigurationPropertyName> configurationPropertyNames;

	private volatile SoftReference<Mappings> mappings = new SoftReference<>(null);

	SpringIterableConfigurationPropertySource(EnumerablePropertySource<?> propertySource, PropertyMapper... mappers) {
		super(propertySource, mappers);
		assertEnumerablePropertySource();
	}

	private void assertEnumerablePropertySource() {
		if (getPropertySource() instanceof MapPropertySource) {
			try {
				((MapPropertySource) getPropertySource()).getSource().size();
			}
			catch (UnsupportedOperationException ex) {
				throw new IllegalArgumentException("PropertySource must be fully enumerable");
			}
		}
	}

	@Override
	public ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
		if (name == null) {
			return null;
		}
		ConfigurationProperty configurationProperty = super.getConfigurationProperty(name);
		if (configurationProperty != null) {
			return configurationProperty;
		}
		for (String candidate : getMappings().getMapped(name)) {
			Object value = getPropertySource().getProperty(candidate);
			if (value != null) {
				Origin origin = PropertySourceOrigin.get(getPropertySource(), candidate);
				return ConfigurationProperty.of(name, value, origin);
			}
		}
		return null;
	}

	@Override
	public Stream<ConfigurationPropertyName> stream() {
		return getConfigurationPropertyNames().stream();
	}

	@Override
	public Iterator<ConfigurationPropertyName> iterator() {
		return getConfigurationPropertyNames().iterator();
	}

	private Collection<ConfigurationPropertyName> getConfigurationPropertyNames() {
		if (!isImmutablePropertySource()) {
			return getMappings().getConfigurationPropertyNames(getPropertySource().getPropertyNames());
		}
		Collection<ConfigurationPropertyName> configurationPropertyNames = this.configurationPropertyNames;
		if (configurationPropertyNames == null) {
			configurationPropertyNames = getMappings()
					.getConfigurationPropertyNames(getPropertySource().getPropertyNames());
			this.configurationPropertyNames = configurationPropertyNames;
		}
		return configurationPropertyNames;
	}

	@Override
	public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
		ConfigurationPropertyState result = super.containsDescendantOf(name);
		if (result == ConfigurationPropertyState.UNKNOWN) {
			result = ConfigurationPropertyState.search(this, name::isAncestorOf);
		}
		return result;
	}

	private Mappings getMappings() {
		return ConfigurationPropertyCache.get(this, Mappings.class, () -> {
			Mappings mappings = this.mappings.get();
			if (mappings == null) {
				mappings = new Mappings(getMappers());
				this.mappings = new SoftReference<>(mappings);
			}
			if (!mappings.hasMappings() || !isImmutablePropertySource()) {
				mappings.updateMappings(getPropertySource()::getPropertyNames);
			}
			return mappings;
		});
	}

	private boolean isImmutablePropertySource() {
		EnumerablePropertySource<?> source = getPropertySource();
		if (source instanceof OriginLookup) {
			return ((OriginLookup<?>) source).isImmutable();
		}
		if (StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME.equals(source.getName())) {
			return source.getSource() == System.getenv();
		}
		return false;
	}

	@Override
	protected EnumerablePropertySource<?> getPropertySource() {
		return (EnumerablePropertySource<?>) super.getPropertySource();
	}

	private static class Mappings {

		private final PropertyMapper[] mappers;

		private volatile MultiValueMap<ConfigurationPropertyName, String> mappings;

		private volatile Map<String, ConfigurationPropertyName> names;

		private volatile String[] lastUpdated;

		Mappings(PropertyMapper[] mappers) {
			this.mappers = mappers;
		}

		boolean hasMappings() {
			return this.mappings != null;
		}

		void updateMappings(Supplier<String[]> propertyNames) {
			int count = 0;
			while (true) {
				try {
					updateMappings(propertyNames.get());
					return;
				}
				catch (ConcurrentModificationException ex) {
					if (count++ > 10) {
						throw ex;
					}
				}
			}
		}

		private void updateMappings(String[] propertyNames) {
			String[] lastUpdated = this.lastUpdated;
			if (lastUpdated != null && Arrays.equals(lastUpdated, propertyNames)) {
				return;
			}
			MultiValueMap<ConfigurationPropertyName, String> currentMappings = this.mappings;
			MultiValueMap<ConfigurationPropertyName, String> mappings = (currentMappings != null)
					? new LinkedMultiValueMap<>(currentMappings) : new LinkedMultiValueMap<>(propertyNames.length);
			Map<String, ConfigurationPropertyName> currentNames = this.names;
			Map<String, ConfigurationPropertyName> names = (currentNames != null) ? new HashMap<>(currentNames)
					: new HashMap<>(propertyNames.length);
			for (PropertyMapper propertyMapper : this.mappers) {
				for (String propertyName : propertyNames) {
					if (!names.containsKey(propertyName)) {
						ConfigurationPropertyName configurationPropertyName = propertyMapper.map(propertyName);
						if (configurationPropertyName != null && !configurationPropertyName.isEmpty()) {
							mappings.add(configurationPropertyName, propertyName);
							names.put(propertyName, configurationPropertyName);
						}
					}
				}
			}
			this.mappings = mappings;
			this.names = names;
			this.lastUpdated = propertyNames;
		}

		List<String> getMapped(ConfigurationPropertyName configurationPropertyName) {
			return this.mappings.getOrDefault(configurationPropertyName, Collections.emptyList());
		}

		Collection<ConfigurationPropertyName> getConfigurationPropertyNames(String[] propertyNames) {
			Map<String, ConfigurationPropertyName> names = this.names;
			if (names == null || names.isEmpty()) {
				return Collections.emptySet();
			}
			List<ConfigurationPropertyName> result = new ArrayList<>(names.size());
			for (String propertyName : propertyNames) {
				ConfigurationPropertyName configurationPropertyName = names.get(propertyName);
				if (configurationPropertyName != null) {
					result.add(configurationPropertyName);
				}
			}
			return result;
		}

	}

}
