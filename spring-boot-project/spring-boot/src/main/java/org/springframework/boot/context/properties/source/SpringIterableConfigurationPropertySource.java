/*
 * Copyright 2012-2019 the original author or authors.
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
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.boot.env.EnumerablePropertySourceChangeTracker.State;
import org.springframework.boot.env.EnumerablePropertySourceChangeTrackers;
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

	private final EnumerablePropertySourceChangeTrackers changeTrackers;

	private SoftReference<Mappings> mappings = new SoftReference<>(null);

	private volatile Collection<ConfigurationPropertyName> configurationPropertyNames;

	SpringIterableConfigurationPropertySource(EnumerablePropertySource<?> propertySource, PropertyMapper... mappers) {
		super(propertySource, mappers);
		assertEnumerablePropertySource();
		this.changeTrackers = new EnumerablePropertySourceChangeTrackers(propertySource.getClass().getClassLoader());
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
		EnumerablePropertySource<?> propertySource = getPropertySource();
		boolean immutable = isImmutablePropertySource();
		Mappings mappings = this.mappings.get();
		if (mappings == null) {
			mappings = new Mappings(getMappers());
			State state = (immutable) ? null : this.changeTrackers.getState(propertySource);
			mappings.updateMappings(state, propertySource.getPropertyNames());
			this.mappings = new SoftReference<>(mappings);
			return mappings;
		}
		if (isImmutablePropertySource()) {
			return mappings;
		}
		int attempts = 0;
		while (true) {
			try {
				if (mappings.hasSourceChanged(getPropertySource())) {
					mappings.updateMappings(this.changeTrackers.getState(propertySource),
							propertySource.getPropertyNames());
				}
				return mappings;
			}
			catch (ConcurrentModificationException ex) {
				attempts++;
				if (attempts > 10) {
					throw ex;
				}
			}
		}
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

		private volatile State state;

		private volatile MultiValueMap<ConfigurationPropertyName, String> mappings;

		private volatile Map<String, ConfigurationPropertyName> names;

		Mappings(PropertyMapper[] mappers) {
			this.mappers = mappers;
		}

		public boolean hasSourceChanged(EnumerablePropertySource<?> propertySource) {
			return this.state.hasChanged(propertySource);
		}

		void updateMappings(State state, String[] propertyNames) {
			MultiValueMap<ConfigurationPropertyName, String> previousMappings = this.mappings;
			Map<String, ConfigurationPropertyName> previousNames = this.names;
			MultiValueMap<ConfigurationPropertyName, String> updatedMappings = (previousMappings != null)
					? new LinkedMultiValueMap<>(previousMappings) : new LinkedMultiValueMap<>(propertyNames.length);
			Map<String, ConfigurationPropertyName> updatedNames = (previousNames != null) ? new HashMap<>(previousNames)
					: new HashMap<>(propertyNames.length);
			for (PropertyMapper propertyMapper : this.mappers) {
				for (String propertyName : propertyNames) {
					if (!updatedNames.containsKey(propertyName)) {
						ConfigurationPropertyName configurationPropertyName = propertyMapper.map(propertyName);
						if (configurationPropertyName != null && !configurationPropertyName.isEmpty()) {
							updatedMappings.add(configurationPropertyName, propertyName);
							updatedNames.putIfAbsent(propertyName, configurationPropertyName);
						}
					}
				}
			}
			this.state = state;
			this.mappings = updatedMappings;
			this.names = updatedNames;
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
