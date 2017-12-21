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

/**
 * Default {@link PropertyMapper} implementation. Names are mapped by removing invalid
 * characters and converting to lower case. For example "{@code my.server_name.PORT}" is
 * mapped to "{@code my.servername.port}".
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see PropertyMapper
 * @see SpringConfigurationPropertySource
 */
final class DefaultPropertyMapper implements PropertyMapper {

	public static final PropertyMapper INSTANCE = new DefaultPropertyMapper();

	@Override
	public PropertyMapping[] map(ConfigurationPropertyName configurationPropertyName) {
		String convertedName = configurationPropertyName.toString();
		return new PropertyMapping[] {
				new PropertyMapping(convertedName, configurationPropertyName) };
	}

	@Override
	public PropertyMapping[] map(String propertySourceName) {
		return tryMap(propertySourceName);
	}

	private PropertyMapping[] tryMap(String propertySourceName) {
		try {
			ConfigurationPropertyName convertedName = ConfigurationPropertyName
					.adapt(propertySourceName, '.');
			if (!convertedName.isEmpty()) {
				return new PropertyMapping[] {
						new PropertyMapping(propertySourceName, convertedName) };
			}
		}
		catch (Exception ex) {
		}
		return NO_MAPPINGS;
	}

}
