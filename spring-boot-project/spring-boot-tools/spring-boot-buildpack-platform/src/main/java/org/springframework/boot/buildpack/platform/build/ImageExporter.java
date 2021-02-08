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
import java.io.InputStream;

import org.springframework.boot.buildpack.platform.docker.type.ImageReference;

/**
 * Exports an image archive from the Docker daemon.
 *
 * @author Scott Frederick
 */
@FunctionalInterface
interface ImageExporter {

	/**
	 * Export an image.
	 * @param reference the image reference
	 * @return an input stream to read the contents of the image
	 */
	InputStream exportImage(ImageReference reference) throws IOException;

}
