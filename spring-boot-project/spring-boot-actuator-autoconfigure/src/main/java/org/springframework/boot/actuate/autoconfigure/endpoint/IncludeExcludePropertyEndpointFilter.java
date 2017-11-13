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

package org.springframework.boot.actuate.autoconfigure.endpoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * {@link EndpointFilter} that will filter endpoints based on {@code include} and
 * {@code exclude} properties.
 *
 * @param <T> The operation type
 * @author Phillip Webb
 * @since 2.0.0
 */
public class IncludeExcludePropertyEndpointFilter<T extends Operation>
		implements EndpointFilter<T> {

	private final Set<String> include;

	private final Set<String> exclude;

	public IncludeExcludePropertyEndpointFilter(Environment environment, String prefix) {
		Assert.notNull(environment, "Environment must not be null");
		Assert.hasText(prefix, "Prefix must not be empty");
		Binder binder = Binder.get(environment);
		this.include = bind(binder, prefix + ".include");
		this.exclude = bind(binder, prefix + ".exclude");
	}

	private Set<String> bind(Binder binder, String name) {
		return binder.bind(name, Bindable.listOf(String.class)).orElseGet(ArrayList::new)
				.stream().map(String::toLowerCase)
				.collect(Collectors.toCollection(HashSet::new));
	}

	@Override
	public boolean match(EndpointInfo<T> info, EndpointDiscoverer<T> discoverer) {
		return isIncluded(info) && !isExcluded(info);
	}

	private boolean isIncluded(EndpointInfo<T> info) {
		if (this.include.isEmpty()) {
			return true;
		}
		return contains(this.include, info);
	}

	private boolean isExcluded(EndpointInfo<T> info) {
		if (this.exclude.isEmpty()) {
			return false;
		}
		return contains(this.exclude, info);
	}

	private boolean contains(Set<String> items, EndpointInfo<T> info) {
		return items.contains(info.getId().toLowerCase());
	}

}
