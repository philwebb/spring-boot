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

package org.springframework.boot.env;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.StringUtils;

/**
 * An {@link EnvironmentPostProcessor} that configures the
 * {@link org.springframework.core.env.Environment} by loading contents from
 * `spring.environment.import`. An import can specify the locations of the files or
 * folders to import configuration from. For file locations, `.properties` and `.yaml`
 * files are supported by default. If the location is a folder, the resulting
 * {@link PropertySource} will contain a key for each file in the folder with the value
 * being the contents of the file.
 *
 * @author Madhura Bhave
 */
public class ImportedConfigLocationsEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private final List<PropertySourceLoader> propertySourceLoaders;

	private final DefaultResourceLoader resourceLoader;

	public ImportedConfigLocationsEnvironmentPostProcessor() {
		this.propertySourceLoaders = SpringFactoriesLoader.loadFactories(PropertySourceLoader.class,
				getClass().getClassLoader());
		this.resourceLoader = new DefaultResourceLoader();
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Binder binder = new Binder(ConfigurationPropertySources.from(environment.getPropertySources()));
		Map<String, Import> imports = getImports(binder);
		for (Map.Entry<String, Import> entry : imports.entrySet()) {
			String prefix = entry.getKey();
			Import anImport = entry.getValue();
			List<String> locations = anImport.getLocations();
			for (String location : locations) {
				boolean isFolder = location.endsWith("/");
				if (!isFolder) {
					processFile(environment, prefix, anImport, location);
				}
			}
		}
	}

	private Map<String, Import> getImports(Binder binder) {
		return binder.bind("spring.environment.import", Bindable.mapOf(String.class, Import.class))
				.orElse(Collections.emptyMap());
	}

	private void processFile(ConfigurableEnvironment environment, String key, Import value, String location) {
		for (PropertySourceLoader loader : this.propertySourceLoaders) {
			if (canLoadFileExtension(loader, location)) {
				Resource resource = this.resourceLoader.getResource(location);
				try {
					List<PropertySource<?>> propertySources = loader.load("additionalConfig: [" + location + "]",
							resource);
					addPropertySources(environment, key, value, propertySources);
				}
				catch (Exception ex) {
					throw new IllegalStateException("Failed to load property source from " + location, ex);
				}
			}
		}
	}

	private boolean canLoadFileExtension(PropertySourceLoader loader, String name) {
		return Arrays.stream(loader.getFileExtensions())
				.anyMatch((fileExtension) -> StringUtils.endsWithIgnoreCase(name, fileExtension));
	}

	private void addPropertySources(ConfigurableEnvironment environment, String prefix, Import anImport,
			List<PropertySource<?>> propertySources) {
		propertySources.stream().map((source) -> {
			if (anImport.isUsePrefix()) {
				return new PrefixedValuePropertySource(prefix, source);
			}
			return source;
		}).forEach(environment.getPropertySources()::addLast);
	}

	/**
	 * Represents the additional config to import.
	 */
	static class Import {

		private List<String> locations = new ArrayList<>();

		private boolean usePrefix = true;

		public List<String> getLocations() {
			return this.locations;
		}

		public void setLocations(List<String> locations) {
			this.locations = locations;
		}

		public boolean isUsePrefix() {
			return this.usePrefix;
		}

		public void setUsePrefix(boolean usePrefix) {
			this.usePrefix = usePrefix;
		}

	}

}
