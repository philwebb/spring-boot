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
 * A specialization of {@link RepackagingLayout} that supports layers in the repackaged
 * archive.
 *
 * @author Madhura Bhave
 * @since 2.3.0
 */
public interface LayeredLayout extends RepackagingLayout {

	/**
	 * Returns the location of the layers index file that should be written or
	 * {@code null} if not index is required. The result should include the filename and
	 * is relative to the root of the jar.
	 * @return the layers index file location
	 */
	String getLayersIndexFileLocation();

	/**
	 * Returns the location to which classes should be moved within the context of a
	 * layer.
	 * @param layer the destination layer for the content
	 * @return the repackaged classes location
	 */
	String getRepackagedClassesLocation(Layer layer);

	/**
	 * Returns the destination path for a given library within the context of a layer.
	 * @param libraryName the name of the library (excluding any path)
	 * @param scope the scope of the library
	 * @param layer the destination layer for the content
	 * @return the location of the library relative to the root of the archive (should end
	 * with '/') or {@code null} if the library should not be included.
	 */
	String getLibraryLocation(String libraryName, LibraryScope scope, Layer layer);

}
