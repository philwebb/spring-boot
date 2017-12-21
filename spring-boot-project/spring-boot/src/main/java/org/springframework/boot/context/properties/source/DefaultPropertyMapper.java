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

import java.util.Collections;
import java.util.List;

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

	private DefaultPropertyMapper() {
	}

	@Override
	public List<PropertyMapping> map(
			ConfigurationPropertyName configurationPropertyName) {
		// Use a local copy in case another thread changes things
		String convertedName = configurationPropertyName.toString();
		return Collections.singletonList(
				new PropertyMapping(convertedName, configurationPropertyName));
	}

	@Override
	public List<PropertyMapping> map(String propertySourceName) {
		return tryMap(propertySourceName);
	}

	private List<PropertyMapping> tryMap(String propertySourceName) {
		try {
			ConfigurationPropertyName convertedName = ConfigurationPropertyName
					.adapt(propertySourceName, '.');
			if (!convertedName.isEmpty()) {
				PropertyMapping o = new PropertyMapping(propertySourceName,
						convertedName);
				return Collections.singletonList(o);
			}
		}
		catch (Exception ex) {
		}
		return Collections.emptyList();
	}

}
