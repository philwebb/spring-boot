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

import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;

/**
 * {@link PropertySource} that appends a prefix to all properties in the original
 * {@link PropertySource}.
 *
 * @author Madhura Bhave
 */
class PrefixedValuePropertySource extends PropertySource<PropertySource<?>> {

	private final String prefix;

	public PrefixedValuePropertySource(String prefix, PropertySource source) {
		super(source.getName(), source);
		Assert.hasText(prefix, "Prefix must contain at least one character");
		this.prefix = prefix;
	}

	@Override
	public boolean containsProperty(String name) {
		return this.source.containsProperty(getSourceName(name));
	}

	private String getSourceName(String name) {
		return name.substring((this.prefix + ".").length());
	}

	@Override
	public Object getProperty(String name) {
		return this.source.getProperty(getSourceName(name));
	}

}
