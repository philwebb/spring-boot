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
 * Strategy interface used to resolve which layer a file should be in.
 *
 * @author Madhura Bhave
 * @since 2.3.0
 */
public interface LayerResolver {

	/**
	 * The default layer resolver.
	 */
	LayerResolver DEFAULT = new SimpleLayerResolver();

	/**
	 * Returns the layer that the file should be added to.
	 * @param destination the original destination for the file
	 * @param file the name of the file
	 * @return the resolved layer
	 */
	String resolveLayer(String destination, String file);

}
