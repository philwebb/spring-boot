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
 * @author Phillip Webb
 */
class UrlJarFiles {

	private final JarFileFactory jarFileFactory;

	private Object lock = new Object();

	private final Map<JarFileUrlKey, JarFile> jarFileUrlToJarFileCache = new HashMap<>();

	private final Map<JarFile, URL> jarFileToJarFileUrlCache = new HashMap<>();

	UrlJarFiles() {
		this(new DefaultJarFileFactory());
	}

	UrlJarFiles(JarFileFactory jarFileFactory) {
		this.jarFileFactory = jarFileFactory;
	}

	JarFile getOrCreate(boolean useCaches, URL jarFileUrl) throws IOException {
		if (useCaches) {
			JarFileUrlKey key = JarFileUrlKey.get(jarFileUrl);
			synchronized (this.lock) {
				JarFile cached = this.jarFileUrlToJarFileCache.get(key);
				if (cached != null) {
					return cached;
				}
			}
		}
		return this.jarFileFactory.createJarFile(jarFileUrl, this::onClose);
	}

	boolean cacheIfAbsent(boolean useCaches, URL jarFileUrl, JarFile jarFile) {
		if (!useCaches) {
			return false;
		}
		JarFileUrlKey key = JarFileUrlKey.get(jarFileUrl);
		synchronized (this.lock) {
			JarFile cached = this.jarFileUrlToJarFileCache.get(key);
			if (cached == null) {
				this.jarFileUrlToJarFileCache.put(key, jarFile);
				this.jarFileToJarFileUrlCache.put(jarFile, jarFileUrl);
				return true;
			}
			return false;
		}
	}

	void closeIfNotCached(URL jarFileUrl, JarFile jarFile) throws IOException {
		JarFileUrlKey key = JarFileUrlKey.get(jarFileUrl);
		JarFile cached;
		synchronized (this.lock) {
			cached = this.jarFileUrlToJarFileCache.get(key);
		}
		if (cached != jarFile) {
			jarFile.close();
		}
	}

	URLConnection reconnect(JarFile jarFile, URLConnection existingConnection) throws IOException {
		Boolean useCaches = (existingConnection != null) ? existingConnection.getUseCaches() : null;
		URLConnection connection = openConnection(jarFile);
		if (useCaches != null) {
			connection.setUseCaches(useCaches);
		}
		return connection;
	}

	private URLConnection openConnection(JarFile jarFile) throws IOException {
		URL url;
		synchronized (this.lock) {
			url = this.jarFileToJarFileUrlCache.get(jarFile);
		}
		return (url != null) ? url.openConnection() : null;
	}

	private void onClose(JarFile jarFile) {
		synchronized (this.lock) {
			URL removed = this.jarFileToJarFileUrlCache.remove(jarFile);
			if (removed != null) {
				JarFileUrlKey key = JarFileUrlKey.get(removed);
				this.jarFileUrlToJarFileCache.remove(key);
			}
		}
	}

}
