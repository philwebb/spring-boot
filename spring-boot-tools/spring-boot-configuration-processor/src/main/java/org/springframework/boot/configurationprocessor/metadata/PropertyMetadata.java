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

/**
 * Meta-data for a single configuration property.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.2.0
 * @see ConfigurationMetadata
 */
public final class PropertyMetadata extends ItemMetadata {

	private final String dataType;

	public PropertyMetadata(String name, String dataType, String sourceType,
			String sourceMethod, String description) {
		super(name, sourceType, sourceMethod, description);
		this.dataType = dataType;
	}

	public PropertyMetadata(String prefix, String name, String dataType,
			String sourceType, String sourceMethod, String description) {
		super(prefix, name, sourceType, sourceMethod, description);
		this.dataType = dataType;
	}

	public String getDataType() {
		return this.dataType;
	}

	@Override
	protected void buildToString(StringBuilder string) {
		buildToStringProperty(string, "dataType", this.dataType);
		super.buildToString(string);
	}

}
