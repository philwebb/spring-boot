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

import org.springframework.core.convert.Property;

/**
 * Internal utility to help when dealing with Java Bean property names.
 *
 * @author Phillip Webb
 */
abstract class BeanPropertyName {

	private BeanPropertyName() {
	}

	/**
	 * Return the name of the specified {@link Property} in dashed form.
	 * @param property the source property
	 * @return the dashed from
	 */
	public static String toDashedForm(Property property) {
		return toDashedForm(property.getName());
	}

	/**
	 * Return the specified Java Bean property name in dashed form.
	 * @param name the source name
	 * @return the dashed from
	 */
	public static String toDashedForm(String name) {
		StringBuilder result = new StringBuilder();
		for (char c : name.replace("_", "-").toCharArray()) {
			if (Character.isUpperCase(c) && result.length() > 0
					&& result.charAt(result.length() - 1) != '-') {
				result.append("-");
			}
			result.append(Character.toLowerCase(c));
		}
		return result.toString();
	}

}
