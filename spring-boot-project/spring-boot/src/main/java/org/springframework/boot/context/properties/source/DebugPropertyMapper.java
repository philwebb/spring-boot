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
 * Temp mapper for debug.
 *
 * @author Phillip Webb
 */
class DebugPropertyMapper implements PropertyMapper {

	private final PropertyMapper delegate;

	public DebugPropertyMapper(PropertyMapper delegate) {
		this.delegate = delegate;
	}

	@Override
	public List<PropertyMapping> map(
			ConfigurationPropertyName configurationPropertyName) {
		System.err.println(
				"Map from configurationPropertyName " + configurationPropertyName);
		return this.delegate.map(configurationPropertyName);
	}

	@Override
	public List<PropertyMapping> map(String propertySourceName) {
		System.err.println("Map from propertySourceName " + propertySourceName);
		return this.delegate.map(propertySourceName);
	}

}
