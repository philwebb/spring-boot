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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.util.Assert;

/**
 * Internal base class for contributors backed by a map with values adapted as necessary.
 *
 * @param <V> the value type
 * @param <C> the contributor type
 * @param <E> the entry
 * @author Phillip Webb
 * @author Guirong Hu
 */
abstract class AbstractMapAdapter<V, C, E> {

	private final Map<String, C> map;

	private final BiFunction<String, C, E> entryAdapter;

	AbstractMapAdapter(Map<String, V> map, Function<V, ? extends C> valueAdapter,
			BiFunction<String, C, E> entryAdapter) {
		Assert.notNull(map, "'map' must not be null");
		Assert.notNull(valueAdapter, "'valueAdapter' must not be null");
		map.keySet().forEach(this::validateMapKey);
		this.map = Collections.unmodifiableMap(map.entrySet().stream().collect(LinkedHashMap::new, (result, entry) -> {
			String key = entry.getKey();
			C value = adaptMapValue(entry.getValue(), valueAdapter);
			result.put(key, value);
		}, Map::putAll));
		this.entryAdapter = entryAdapter;
	}

	private void validateMapKey(String value) {
		Assert.notNull(value, "'map' must not contain null keys");
		Assert.isTrue(!value.contains("/"), "'map' keys must not contain a '/'");
	}

	private C adaptMapValue(V value, Function<V, ? extends C> valueAdapter) {
		C contributor = (value != null) ? valueAdapter.apply(value) : null;
		Assert.notNull(contributor, "'map' must not contain null values");
		return contributor;
	}

	C getContributor(String name) {
		return this.map.get(name);
	}

	Iterator<E> iterator() {
		return this.map.entrySet()
			.stream()
			.map((entry) -> this.entryAdapter.apply(entry.getKey(), entry.getValue()))
			.iterator();
	}

}
