/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.developertools.restart.server;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.developertools.restart.Restarter;
import org.springframework.boot.developertools.restart.classloader.ClassLoaderFiles;
import org.springframework.boot.developertools.restart.classloader.ClassLoaderFiles.SourceFolder;
import org.springframework.util.Assert;

/**
 * Server used to {@link Restarter restart} the current application with updated
 * {@link ClassLoaderFiles}.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class RestartServer {

	private final SourceFolderUrlFilter sourceFolderUrlFilter;

	private final ClassLoader classLoader;

	/**
	 * Create a new {@link RestartServer} instance.
	 * @param sourceFolderUrlFilter the source filter used to link remote folder to the
	 * local classpath
	 */
	public RestartServer(SourceFolderUrlFilter sourceFolderUrlFilter) {
		this(sourceFolderUrlFilter, Thread.currentThread().getContextClassLoader());
	}

	/**
	 * Create a new {@link RestartServer} instance.
	 * @param sourceFolderUrlFilter the source filter used to link remote folder to the
	 * local classpath
	 * @param classLoader the application classloader
	 */
	public RestartServer(SourceFolderUrlFilter sourceFolderUrlFilter,
			ClassLoader classLoader) {
		Assert.notNull(sourceFolderUrlFilter, "SourceFolderUrlFilter must not be null");
		Assert.notNull(classLoader, "ClassLoader must not be null");
		this.sourceFolderUrlFilter = sourceFolderUrlFilter;
		this.classLoader = classLoader;
	}

	/**
	 * Update the current running application with the specified {@link ClassLoaderFiles}
	 * and trigger a reload.
	 * @param files updated class loader files
	 */
	public void updateAndRestart(ClassLoaderFiles files) {
		Set<URL> urls = new LinkedHashSet<URL>();
		for (SourceFolder folder : files.getSourceFolders()) {
			urls.addAll(getClassPathUrls(folder.getName()));
		}
		restart(urls, files);
	}

	private Set<URL> getClassPathUrls(String sourceFolder) {
		Set<URL> urls = new LinkedHashSet<URL>();
		ClassLoader classLoader = this.classLoader;
		while (classLoader != null) {
			if (classLoader instanceof URLClassLoader) {
				for (URL url : ((URLClassLoader) classLoader).getURLs()) {
					if (this.sourceFolderUrlFilter.isMatch(sourceFolder, url)) {
						urls.add(url);
					}
				}
			}
			classLoader = classLoader.getParent();
		}
		return urls;
	}

	protected void restart(Set<URL> urls, ClassLoaderFiles files) {
		Restarter restarter = Restarter.getInstance();
		restarter.addUrls(urls);
		restarter.addClassLoaderFiles(files);
		restarter.restart();
	}

}
