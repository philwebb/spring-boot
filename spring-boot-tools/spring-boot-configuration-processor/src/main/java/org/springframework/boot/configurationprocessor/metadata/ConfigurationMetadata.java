/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.configurationprocessor.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration meta-data.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.2.0
 * @see PropertyMetadata
 */
public class ConfigurationMetadata {

	private final List<PropertyMetadata> properties;

	public ConfigurationMetadata() {
		this.properties = new ArrayList<PropertyMetadata>();
	}

	public ConfigurationMetadata(ConfigurationMetadata metadata) {
		this.properties = new ArrayList<PropertyMetadata>(metadata.getProperties());
	}

	/**
	 * Add property meta-data.
	 * @param propertyMetadata the meta-data to add
	 */
	public void add(PropertyMetadata propertyMetadata) {
		this.properties.add(propertyMetadata);
		Collections.sort(this.properties);
	}

	/**
	 * Add all properties from another {@link ConfigurationMetadata}.
	 * @param metadata the {@link ConfigurationMetadata} instance to merge
	 */
	public void addAll(ConfigurationMetadata metadata) {
		this.properties.addAll(metadata.getProperties());
		Collections.sort(this.properties);
	}

	/**
	 * @return the meta-data properties.
	 */
	public List<PropertyMetadata> getProperties() {
		return Collections.unmodifiableList(this.properties);
	}

	public static String nestedPrefix(String prefix, String name) {
		String nestedPrefix = (prefix == null ? "" : prefix);
		String dashedName = toDashedCase(name);
		nestedPrefix += ("".equals(nestedPrefix) ? dashedName : "." + dashedName);
		return nestedPrefix;
	}

	static String toDashedCase(String name) {
		StringBuilder dashed = new StringBuilder();
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isUpperCase(c) && dashed.length() > 0) {
				dashed.append("-");
			}
			dashed.append(Character.toLowerCase(c));
		}
		return dashed.toString();
	}

}
