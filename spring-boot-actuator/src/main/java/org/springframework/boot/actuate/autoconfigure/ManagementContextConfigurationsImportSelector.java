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

package org.springframework.boot.actuate.autoconfigure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

/**
 * Selects configuration classes for the management context configuration. Entries are
 * loaded from {@code /META-INF/spring.factories} under the
 * {@code org.springframework.boot.actuate.autoconfigure.ManagementContextConfiguration}
 * key.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @see ManagementContextConfiguration
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class ManagementContextConfigurationsImportSelector
		implements DeferredImportSelector, BeanClassLoaderAware {

	private ClassLoader classLoader;

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		// Find all possible management context configuration classes, filtering
		// duplicates
		List<String> names = loadFactoryNames();
		SimpleMetadataReaderFactory readerFactory = new SimpleMetadataReaderFactory(
				this.classLoader);
		List<ManagementContextConfigurationClass> configurationClasses = createConfigurationClasses(
				names, readerFactory);
		OrderComparator.sort(configurationClasses);
		List<String> sortedNames = new ArrayList<String>();
		for (ManagementContextConfigurationClass configurationClass : configurationClasses) {
			sortedNames.add(configurationClass.className);
		}
		return sortedNames.toArray(new String[names.size()]);
	}

	private List<ManagementContextConfigurationClass> createConfigurationClasses(
			List<String> names, SimpleMetadataReaderFactory readerFactory) {
		List<ManagementContextConfigurationClass> configurationClasses = new ArrayList<ManagementContextConfigurationClass>();
		for (String name : names) {
			try {
				MetadataReader metadataReader = readerFactory.getMetadataReader(name);
				Map<String, Object> orderAttributes = metadataReader
						.getAnnotationMetadata()
						.getAnnotationAttributes(Order.class.getName());
				int order = (orderAttributes != null
						&& orderAttributes.get("value") != null)
								? (int) orderAttributes.get("value")
								: Ordered.LOWEST_PRECEDENCE;
				configurationClasses
						.add(new ManagementContextConfigurationClass(name, order));
			}
			catch (IOException ex) {
				throw new RuntimeException(
						"Failed to read annotation metadata for '" + name + "'", ex);
			}
		}
		return configurationClasses;
	}

	protected List<String> loadFactoryNames() {
		return SpringFactoriesLoader
				.loadFactoryNames(ManagementContextConfiguration.class, this.classLoader);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	private static class ManagementContextConfigurationClass implements Ordered {

		private final String className;

		private final int order;

		public ManagementContextConfigurationClass(String className, int order) {
			this.className = className;
			this.order = order;
		}

		@Override
		public int getOrder() {
			return this.order;
		}

	}

}
