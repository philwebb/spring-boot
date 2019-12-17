/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.loader.tools;

/**
 * Default implementation of {@link LayerResolver}.
 *
 * @author Madhura Bhave
 */
class SimpleLayerResolver implements LayerResolver {

	private static final String CLASSES_LAYER = "layers/0001";

	private static final String SNAPSHOTS_LAYER = "layers/0002";

	private static final String DEPENDENCIES_LAYER = "layers/0003";

	private static final String CATCH_ALL_LAYER = "layers/0004";

	@Override
	public String resolveLayer(String destination, String file) {
		if (destination.endsWith("/classes/")) {
			return CLASSES_LAYER;
		}
		if (file.toUpperCase().contains("SNAPSHOT")) {
			return SNAPSHOTS_LAYER;
		}
		if (destination.endsWith("/lib/")) {
			return DEPENDENCIES_LAYER;
		}
		return CATCH_ALL_LAYER;
	}

}
