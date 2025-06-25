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

import java.util.Collections;
import java.util.stream.Stream;

import org.springframework.boot.health.contributor.ReactiveHealthContributors.Entry;

/**
 * Adapts {@link HealthContributors} to {@link ReactiveHealthContributors} so that they
 * can be safely invoked in a reactive environment.
 *
 * @author Phillip Webb
 */
class HealthContributorsReactiveAdapter
		extends Adapter<HealthContributors, HealthContributors.Entry, ReactiveHealthContributor, Entry>
		implements ReactiveHealthContributors {

	HealthContributorsReactiveAdapter(HealthContributors healthContributors) {
		super(Collections.singleton(healthContributors), HealthContributorsReactiveAdapter::getAdapted,
				HealthContributors::stream, HealthContributors.Entry::name,
				HealthContributorsReactiveAdapter::adaptEntry);
	}

	private static ReactiveHealthContributor getAdapted(HealthContributors healthContributors, String name) {
		return adaptContributor(healthContributors.getContributor(name));
	}

	private static Entry adaptEntry(HealthContributors.Entry entry) {
		return new Entry(entry.name(), adaptContributor(entry.contributor()));
	}

	private static ReactiveHealthContributor adaptContributor(HealthContributor contributor) {
		return (contributor != null) ? ReactiveHealthContributor.adapt(contributor) : null;
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
