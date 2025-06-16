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

package org.springframework.boot.health.contributor;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.util.Assert;

/**
 * A collection of named {@link HealthContributor health contributors}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface HealthContributors extends Iterable<HealthContributors.Entry> {

	/**
	 * Return the contributor with the given name.
	 * @param name the name of the contributor
	 * @return a contributor instance or {@code null}
	 */
	HealthContributor getContributor(String name);

	/**
	 * Return a stream of the contributor entries.
	 * @return the stream of named contributors
	 */
	default Stream<HealthContributors.Entry> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	/**
	 * A health contributor entry.
	 *
	 * @param name the name of the contributor
	 * @param contributor the contributor
	 */
	record Entry(String name, HealthContributor contributor) {

		public Entry {
			Assert.hasText(name, "'name' must not be empty");
			Assert.notNull(contributor, "'contributor' must not be null");
		}

	}

}
