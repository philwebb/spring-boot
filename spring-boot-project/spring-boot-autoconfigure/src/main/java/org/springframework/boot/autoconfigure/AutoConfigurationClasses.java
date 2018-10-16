/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.Assert;

/**
 * Provides access to relevant details of an auto-configuration classes.
 *
 * @author Phillip Webb
 * @see AutoConfigurationClass
 */
class AutoConfigurationClasses {

	private final Set<String> names = new LinkedHashSet<>();

	private final Map<String, AutoConfigurationClass> classes = new LinkedHashMap<>();

	private final Map<String, AutoConfigurationClass> deprecated = new HashMap<>();

	private final Map<AutoConfigurationClass, Set<String>> before = new HashMap<>();

	private final Map<AutoConfigurationClass, Set<String>> after = new HashMap<>();

	AutoConfigurationClasses(BeanFactory beanFactory, ResourceLoader resourceLoader,
			AutoConfigurationMetadata autoConfigurationMetadata,
			Collection<String> classNames) {
		this(getMetadataReaderFactory(beanFactory, resourceLoader),
				autoConfigurationMetadata, classNames);
	}

	AutoConfigurationClasses(MetadataReaderFactory metadataReaderFactory,
			AutoConfigurationMetadata autoConfigurationMetadata,
			Collection<String> classNames) {
		Assert.notNull(metadataReaderFactory, "MetadataReaderFactory must not be null");
		Assert.notNull(classNames, "ClassNames must not be null");
		addToClasses(metadataReaderFactory, autoConfigurationMetadata, classNames, false);
	}

	private void addToClasses(MetadataReaderFactory metadataReaderFactory,
			AutoConfigurationMetadata autoConfigurationMetadata,
			Collection<String> classNames, boolean inherited) {
		for (String className : classNames) {
			addToClasses(metadataReaderFactory, autoConfigurationMetadata, className,
					inherited);
		}
	}

	private void addToClasses(MetadataReaderFactory metadataReaderFactory,
			AutoConfigurationMetadata autoConfigurationMetadata, String className,
			boolean inherited) {
		AutoConfigurationClass autoConfigurationClass = this.classes.get(className);
		boolean added = autoConfigurationClass != null;
		if (!added) {
			autoConfigurationClass = new AutoConfigurationClass(className,
					metadataReaderFactory, autoConfigurationMetadata);
			Set<String> replacements = getDeprecatedReplacements(autoConfigurationClass);
			if (!replacements.isEmpty()) {
				this.deprecated.put(className, autoConfigurationClass);
				addToClasses(metadataReaderFactory, autoConfigurationMetadata,
						replacements, inherited);
				return;
			}
		}
		if (!inherited) {
			this.names.add(className);
		}
		if (!added) {
			boolean available = autoConfigurationClass.isAvailable();
			if (!inherited || available) {
				this.classes.put(className, autoConfigurationClass);
			}
			if (available) {
				addToClasses(metadataReaderFactory, autoConfigurationMetadata,
						autoConfigurationClass.getBefore(), true);
				addToClasses(metadataReaderFactory, autoConfigurationMetadata,
						autoConfigurationClass.getAfter(), true);
			}
		}
	}

	public Set<String> getNames(boolean includeInherted) {
		return includeInherted ? this.classes.keySet() : this.names;
	}

	public AutoConfigurationClass get(String className) {
		return this.classes.get(className);
	}

	public Set<String> getClassesRequestedAfter(String className) {
		Set<String> result = new LinkedHashSet<>();
		result.addAll(getAfter(get(className)));
		this.classes.forEach((name, autoConfigurationClass) -> {
			if (getBefore(autoConfigurationClass).contains(className)) {
				result.add(name);
			}
		});
		this.deprecated.forEach((name, autoConfigurationClass) -> {
			if (getBefore(autoConfigurationClass).contains(className)) {
				result.add(name);
			}
		});
		return result;
	}

	private Set<String> getBefore(AutoConfigurationClass autoConfigurationClass) {
		return this.before.computeIfAbsent(autoConfigurationClass,
				(key) -> replaceDeprecated(key.getBefore()));
	}

	private Set<String> getAfter(AutoConfigurationClass autoConfigurationClass) {
		return this.after.computeIfAbsent(autoConfigurationClass,
				(key) -> replaceDeprecated(key.getAfter()));
	}

	private Set<String> replaceDeprecated(Set<String> classNames) {
		Set<String> result = new LinkedHashSet<>(classNames.size());
		addReplacingDeprecated(result, classNames);
		return result;
	}

	private void addReplacingDeprecated(Set<String> result, Set<String> classNames) {
		for (String className : classNames) {
			AutoConfigurationClass deprecated = this.deprecated.get(className);
			if (deprecated != null) {
				addReplacingDeprecated(result, deprecated.getDeprecatedReplacements());
			}
			else {
				result.add(className);
			}
		}
	}

	private static MetadataReaderFactory getMetadataReaderFactory(BeanFactory beanFactory,
			ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		try {
			return beanFactory.getBean(
					SharedMetadataReaderFactoryContextInitializer.BEAN_NAME,
					MetadataReaderFactory.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return new CachingMetadataReaderFactory(resourceLoader);
		}
	}

	private Set<String> getDeprecatedReplacements(
			AutoConfigurationClass configurationClass) {
		if (configurationClass != null && configurationClass.isAvailable()) {
			return configurationClass.getDeprecatedReplacements();
		}
		return Collections.emptySet();
	}

}
