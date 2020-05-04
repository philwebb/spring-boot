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

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.env.EnumerablePropertySourceChangeTracker.State;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultEnumerablePropertySourceChangeTracker}.
 *
 * @author Phillip Webb
 */
class DefaultEnumerablePropertySourceChangeTrackerTests {

	private MapPropertySourceChangeTracker tracker = new MapPropertySourceChangeTracker();

	@Test
	void hasChangedWhenNotChangedReturnsFalse() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("a", "A");
		map.put("b", "B");
		MapPropertySource propertySource = new MapPropertySource("test", map);
		State state = this.tracker.getState(propertySource);
		assertThat(state.hasChanged(propertySource)).isFalse();
	}

	@Test
	void hasChangedWhenChangedReturnsTrue() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("a", "A");
		map.put("b", "B");
		MapPropertySource propertySource = new MapPropertySource("test", map);
		State state = this.tracker.getState(propertySource);
		map.put("c", "C");
		assertThat(state.hasChanged(propertySource)).isTrue();
	}

}
