/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.buildpack.platform.build;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.buildpack.platform.docker.type.Layer;

/**
 * A buildpack that should be invoked by the builder during image building.
 *
 * @author Scott Frederick
 */
abstract class Buildpack {

	protected BuildpackDescriptor descriptor;

	/**
	 * Return the buildpack descriptor.
	 * @return the descriptor
	 */
	BuildpackDescriptor getDescriptor() {
		return this.descriptor;
	}

	/**
	 * Return a collection of layers, if any, that contain buildpack content to be added
	 * to the builder.
	 * @return the layers
	 */
	List<Layer> getLayers() throws IOException {
		return Collections.emptyList();
	}

}
