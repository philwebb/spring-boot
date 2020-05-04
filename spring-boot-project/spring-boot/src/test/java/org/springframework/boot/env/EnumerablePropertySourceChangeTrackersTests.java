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

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EnumerablePropertySourceChangeTrackers}.
 *
 * @author Phillip Webb
 */
class EnumerablePropertySourceChangeTrackersTests {

	private EnumerablePropertySourceChangeTrackers trackers = new EnumerablePropertySourceChangeTrackers(
			getClass().getClassLoader());

	@Test
	void getStateForMapSource() {
		MapPropertySource propertySource = new MapPropertySource("test", Collections.emptyMap());
		assertThat(this.trackers.getState(propertySource)).isInstanceOf(MapPropertySourceChangeTracker.KeysState.class);
	}

	@Test
	void getStateForOtherSource() {
		EnumerablePropertySource<?> propertySource = mock(EnumerablePropertySource.class);
		assertThat(this.trackers.getState(propertySource))
				.isInstanceOf(DefaultEnumerablePropertySourceChangeTracker.PropertyNamesState.class);
	}

}
