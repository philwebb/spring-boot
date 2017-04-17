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

package org.springframework.boot.context.properties.bind;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;

/**
 * Binder that can be used by {@link AggregateBinder} implementations to recursively bind
 * elements.
 *
 * @author Phillip Webb
 */
@FunctionalInterface
interface AggregateElementBinder {

	default Object bind(ConfigurationPropertyName name, Bindable<?> target) {
		return bind(name, target, null);
	}

	Object bind(ConfigurationPropertyName name, Bindable<?> target,
			ConfigurationPropertySource source);

}
