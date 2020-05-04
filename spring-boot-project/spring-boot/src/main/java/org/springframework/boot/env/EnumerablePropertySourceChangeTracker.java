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

import org.springframework.core.Ordered;
import org.springframework.core.env.EnumerablePropertySource;

/**
 * Interface used to provide change tracking of a {@link EnumerablePropertySource}. Allows
 * the state of a property source to be obtained in an opaque way, and then later used to
 * determine if the property source has had properties added or removed.
 * <p>
 * Property source implementors may register an instance of this interface in
 * {@code spring.factories} in order to provide a performant way of saving and checking
 * state.
 * <p>
 * If no custom tracking is provided, the default implementation will use the
 * {@link EnumerablePropertySource#getPropertyNames() property source names} as state.
 * <p>
 * Implementations may use the {@link Ordered @Ordered} annotation if a specific priority
 * is required.
 *
 * @author Phillip Webb
 * @since 2.3.0
 * @see EnumerablePropertySourceChangeTrackers
 */
public interface EnumerablePropertySourceChangeTracker {

	/**
	 * Return state information for the given property source if possible.
	 * @param propertySource the property source to track
	 * @return a {@link State} instance or {@code null}
	 */
	State getState(EnumerablePropertySource<?> propertySource);

	/**
	 * State information obtained from a property source at a given point in time.
	 */
	interface State {

		/**
		 * Determine if the given property source has changed since this state was
		 * obtained.
		 * @param propertySource the property source to check
		 * @return {@code true} if the property source has changed
		 */
		boolean hasChanged(EnumerablePropertySource<?> propertySource);

	}

}
