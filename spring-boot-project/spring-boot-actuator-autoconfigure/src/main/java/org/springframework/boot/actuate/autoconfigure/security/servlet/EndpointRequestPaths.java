/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.security.servlet;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Strategy interface used by {@link EndpointRequest} to find the root paths that actuator
 * endpoints are mapped to.
 *
 * @author Phillip Webb
 * @since 2.0.4
 */
public interface EndpointRequestPaths {

	/**
	 * {@link EndpointRequestPaths} that contains a single root mapping.
	 */
	EndpointRequestPaths SINGLE_ROOT = of("/");

	/**
	 * Return a set of all paths that provide access to actuator endpoints.
	 * @return the endpoint paths
	 */
	Set<String> getEndpointRequestPaths();

	static EndpointRequestPaths of(String... paths) {
		Set<String> pathSet = Collections
				.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(paths)));
		return () -> pathSet;
	}

}
