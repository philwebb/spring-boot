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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.Manifest;

/**
 * An archive that can be launched by the {@link Launcher}.
 *
 * @author Phillip Webb
 * @since 3.2.0
 */
public interface Archive extends AutoCloseable {

	// FIXME review close()

	/**
	 * Returns the manifest of the archive.
	 * @return the manifest or {@code null}
	 * @throws IOException if the manifest cannot be read
	 */
	Manifest getManifest() throws IOException;

	/**
	 * Returns classpath URLs for the archive that match the specified filters.
	 * @param searchFilter filter used to limit when additional sub-entry searching is
	 * required or {@code null} if all entries should be considered.
	 * @param includeFilter filter used to determine which entries should be included in
	 * the result or {@code null} if all entries should be included
	 * @return the classpath URLs
	 * @throws IOException on IO error
	 */
	Set<URL> getClassPathUrls(Predicate<Entry> searchFilter, Predicate<Entry> includeFilter) throws IOException;

	/**
	 * Returns if this archive is backed by an exploded archive directory.
	 * @return if the archive is exploded
	 */
	default boolean isExploded() {
		return getRootDirectory() != null;
	}

	/**
	 * Returns the root directory of this archive or {@code null} if the archive is not
	 * backed by a directory.
	 * @return the root directory
	 */
	default File getRootDirectory() {
		return null;
	}

	/**
	 * Closes the {@code Archive}, releasing any open resources.
	 * @throws Exception if an error occurs during close processing
	 */
	@Override
	default void close() throws Exception {
	}

	/**
	 * Factory method to create an appropriate {@link Archive} from the given
	 * {@link Class} target.
	 * @param target a target class that will be used to find the archive code source
	 * @return an new {@link Archive} instance
	 * @throws Exception if the archive cannot be created
	 */
	static Archive create(Class<?> target) throws Exception {
		ProtectionDomain protectionDomain = target.getProtectionDomain();
		CodeSource codeSource = protectionDomain.getCodeSource();
		URI location = (codeSource != null) ? codeSource.getLocation().toURI() : null;
		String path = (location != null) ? location.getSchemeSpecificPart() : null;
		if (path == null) {
			throw new IllegalStateException("Unable to determine code source archive");
		}
		return create(new File(path));
	}

	/**
	 * Factory method to create an {@link Archive} from the given {@link File} target.
	 * @param target a target {@link File} used to create the archive. May be a directory
	 * or a jar file.
	 * @return a new {@link Archive} instance.
	 * @throws Exception if the archive cannot be created
	 */
	static Archive create(File target) throws Exception {
		if (!target.exists()) {
			throw new IllegalStateException("Unable to determine code source archive from " + target);
		}
		return (target.isDirectory() ? new ExplodedArchive(target) : new JarFileArchive(target));
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
