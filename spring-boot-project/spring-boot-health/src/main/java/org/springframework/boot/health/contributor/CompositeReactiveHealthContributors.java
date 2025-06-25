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

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.boot.health.contributor.ReactiveHealthContributors.Entry;

/**
 * {@link ReactiveHealthContributors} composed of other
 * {@link ReactiveHealthContributors}.
 *
 * @author Phillip Webb
 */
class CompositeReactiveHealthContributors
		extends Adapter<ReactiveHealthContributors, Entry, ReactiveHealthContributor, Entry>
		implements ReactiveHealthContributors {

	CompositeReactiveHealthContributors(ReactiveHealthContributors... contributors) {
		super(Arrays.asList(contributors), ReactiveHealthContributors::getContributor,
				ReactiveHealthContributors::stream, Entry::name, Function.identity());
	}

	@Override
	public ReactiveHealthContributor getContributor(String name) {
		return super.getContributor(name);
	}

	@Override
	public Stream<Entry> stream() {
		return super.stream();
	}

}
