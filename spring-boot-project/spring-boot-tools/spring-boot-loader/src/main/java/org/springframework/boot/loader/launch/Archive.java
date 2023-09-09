/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.loader.launch;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.jar.Manifest;

/**
 * An archive that can be launched by the {@link Launcher}.
 *
 * @author Phillip Webb
 * @since 3.2.0
 * @see JarFileArchive
 * @see ExplodedArchive
 */
public interface Archive extends AutoCloseable {

	/**
	 * Returns a URL that can be used to load the archive.
	 * @return the archive URL
	 */
	URL getUrl();

	/**
	 * Returns the manifest of the archive.
	 * @return the manifest
	 * @throws IOException if the manifest cannot be read
	 */
	Manifest getManifest() throws IOException;

	/**
	 * Returns nested {@link Archive} instances that match the specified filters.
	 * @param searchFilter filter used to limit when additional sub-entry searching is
	 * required or {@code null} if all entries should be considered.
	 * @param includeFilter filter used to determine which entries should be included in
	 * the result or {@code null} if all entries should be included
	 * @return the nested archives
	 * @throws IOException on IO error
	 */
	Iterator<Archive> getNestedArchives(Predicate<Entry> searchFilter, Predicate<Entry> includeFilter)
			throws IOException;

	/**
	 * Return if the archive is exploded (already unpacked).
	 * @return if the archive is exploded
	 */
	default boolean isExploded() {
		return false;
	}

	/**
	 * Closes the {@code Archive}, releasing any open resources.
	 * @throws Exception if an error occurs during close processing
	 */
	@Override
	default void close() throws Exception {
	}

	/**
	 * Represents a single entry in the archive.
	 */
	interface Entry {

		/**
		 * Returns the name of the entry.
		 * @return the name of the entry
		 */
		String getName();

		/**
		 * Returns {@code true} if the entry represents a directory.
		 * @return if the entry is a directory
		 */
		boolean isDirectory();

	}

}
