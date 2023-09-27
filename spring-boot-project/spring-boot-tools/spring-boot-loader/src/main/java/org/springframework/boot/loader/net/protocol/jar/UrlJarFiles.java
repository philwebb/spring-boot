/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader.net.protocol.jar;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * Provides access to {@link UrlJarFile} and {@link UrlNestedJarFile} instances taking
 * care of caching concerns when necessary.
 * <p>
 * This class is thread-safe and designed to be shared by all {@link JarUrlConnection}
 * instances.
 *
 * @author Phillip Webb
 */
class UrlJarFiles {

	private final UrlJarFileFactory factory;

	private final Cache cache = new Cache();

	/**
	 * Create a new {@link UrlJarFiles} instance.
	 */
	UrlJarFiles() {
		this(new UrlJarFileFactory());
	}

	/**
	 * Create a new {@link UrlJarFiles} instance.
	 * @param factory the {@link UrlJarFileFactory} to use.
	 */
	UrlJarFiles(UrlJarFileFactory factory) {
		this.factory = factory;
	}

	/**
	 * Get an existing {@link JarFile} instance from the cache, or create a new
	 * {@link JarFile} instance that can be {@link #cacheIfAbsent(boolean, URL, JarFile)
	 * cached later}.
	 * @param useCaches if caches can be used
	 * @param jarFileUrl the jar file URL
	 * @return a new or existing {@link JarFile} instance
	 * @throws IOException on I/O error
	 */
	JarFile getOrCreate(boolean useCaches, URL jarFileUrl) throws IOException {
		if (useCaches) {
			JarFile cached = this.cache.get(jarFileUrl);
			if (cached != null) {
				return cached;
			}
		}
		return this.factory.createJarFile(jarFileUrl, this::onClose);
	}

	/**
	 * Cache the given {@link JarFile} if caching can be used and there is no existing
	 * entry.
	 * @param useCaches if caches can be used
	 * @param jarFileUrl the jar file URL
	 * @param jarFile the jar file
	 * @return {@code true} if that file was added to the cache
	 */
	boolean cacheIfAbsent(boolean useCaches, URL jarFileUrl, JarFile jarFile) {
		if (!useCaches) {
			return false;
		}
		return this.cache.putIfAbsent(jarFileUrl, jarFile);
	}

	/**
	 * Close the given {@link JarFile} only if it is not contained in the cache.
	 * @param jarFileUrl the jar file URL
	 * @param jarFile the jar file
	 * @throws IOException on I/O error
	 */
	void closeIfNotCached(URL jarFileUrl, JarFile jarFile) throws IOException {
		JarFile cached = this.cache.get(jarFileUrl);
		if (cached != jarFile) {
			jarFile.close();
		}
	}

	/**
	 * Reconnect to the {@link JarFile}, returning a replacement {@link URLConnection}.
	 * @param jarFile the jar file
	 * @param existingConnection the existing connection
	 * @return a newly opened connection inhering the same {@code useCaches} value as the
	 * existing connection
	 * @throws IOException on I/O error
	 */
	URLConnection reconnect(JarFile jarFile, URLConnection existingConnection) throws IOException {
		Boolean useCaches = (existingConnection != null) ? existingConnection.getUseCaches() : null;
		URLConnection connection = openConnection(jarFile);
		if (useCaches != null) {
			connection.setUseCaches(useCaches);
		}
		return connection;
	}

	private URLConnection openConnection(JarFile jarFile) throws IOException {
		URL url = this.cache.get(jarFile);
		return (url != null) ? url.openConnection() : null;
	}

	private void onClose(JarFile jarFile) {
		this.cache.remove(jarFile);
	}

	/**
	 * Internal cache.
	 */
	private static class Cache {

		private final Map<JarFileUrlKey, JarFile> jarFileUrlToJarFileCache = new HashMap<>();

		private final Map<JarFile, URL> jarFileToJarFileUrlCache = new HashMap<>();

		/**
		 * Get a {@link JarFile} from the cache given a jar file URL.
		 * @param jarFileUrl the jar file URL
		 * @return the cached {@link JarFile} or {@code null}
		 */
		JarFile get(URL jarFileUrl) {
			JarFileUrlKey urlKey = JarFileUrlKey.get(jarFileUrl);
			synchronized (this) {
				return this.jarFileUrlToJarFileCache.get(urlKey);
			}
		}

		/**
		 * Get a jar file URL from the cache given a jar file.
		 * @param jarFile the jar file
		 * @return the cached {@link URL} or {@code null}
		 */
		URL get(JarFile jarFile) {
			synchronized (this) {
				return this.jarFileToJarFileUrlCache.get(jarFile);
			}
		}

		/**
		 * Put the given jar file URL and jar file into the cache if they aren't already
		 * there.
		 * @param jarFileUrl the jar file URL
		 * @param jarFile the jar file
		 * @return {@code true} if the items were added to the cache or {@code false} if
		 * they were already there
		 */
		boolean putIfAbsent(URL jarFileUrl, JarFile jarFile) {
			JarFileUrlKey urlKey = JarFileUrlKey.get(jarFileUrl);
			synchronized (this) {
				JarFile cached = this.jarFileUrlToJarFileCache.get(urlKey);
				if (cached == null) {
					this.jarFileUrlToJarFileCache.put(urlKey, jarFile);
					this.jarFileToJarFileUrlCache.put(jarFile, jarFileUrl);
					return true;
				}
				return false;
			}
		}

		/**
		 * Remove the given jar and any related URL file from the cache.
		 * @param jarFile the jar file to remove
		 */
		void remove(JarFile jarFile) {
			synchronized (this) {
				URL removedUrl = this.jarFileToJarFileUrlCache.remove(jarFile);
				if (removedUrl != null) {
					this.jarFileUrlToJarFileCache.remove(JarFileUrlKey.get(removedUrl));
				}
			}
		}

	}

}
