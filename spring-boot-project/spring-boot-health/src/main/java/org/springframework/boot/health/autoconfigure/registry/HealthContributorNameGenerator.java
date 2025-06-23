/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.health.autoconfigure.registry;

import java.util.Locale;

/**
 * Strategy used to create health contributor names from bean names.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@FunctionalInterface
public interface HealthContributorNameGenerator {

	/**
	 * Generate the health contributor name for the given bean name.
	 * @param beanName the bean name
	 * @return the health contributor name
	 */
	String generateContributorName(String beanName);

	/**
	 * Return a {@link HealthContributorNameGenerator} that removes standard suffixes.
	 * @return a new {@link HealthContributorNameGenerator} instance
	 */
	static HealthContributorNameGenerator withoutStandardSuffixes() {
		return withoutSuffixes("healthindicator", "healthcontributor");
	}

	/**
	 * Return a {@link HealthContributorNameGenerator} that removes the given suffixes.
	 * @param suffixes the suffixes to remove (not case sensitive)
	 * @return a new {@link HealthContributorNameGenerator} instance
	 */
	static HealthContributorNameGenerator withoutSuffixes(String... suffixes) {
		return (beanName) -> {
			for (String suffix : suffixes) {
				if (beanName != null && beanName.toLowerCase(Locale.ENGLISH).endsWith(suffix)) {
					return beanName.substring(0, beanName.length() - suffix.length());
				}
			}
			return beanName;
		};
	}

}
