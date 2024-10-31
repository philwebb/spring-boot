/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.build.antora;

import org.apache.commons.lang3.StringUtils;

/**
 * A contribution to Antora.
 *
 * @author Andy Wilkinson
 */
abstract class Contribution {

	protected static String toPascalCase(String input) {
		return StringUtils.capitalize(toCamelCase(input));
	}

	protected static String toCamelCase(String input) {
		StringBuilder output = new StringBuilder(input.length());
		boolean capitalize = false;
		for (char c : input.toCharArray()) {
			if (c == '-') {
				capitalize = true;
			}
			else {
				output.append(capitalize ? Character.toUpperCase(c) : c);
				capitalize = false;
			}
		}
		return output.toString();
	}

}
