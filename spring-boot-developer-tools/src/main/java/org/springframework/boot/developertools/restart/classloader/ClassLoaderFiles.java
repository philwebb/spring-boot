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

package org.springframework.boot.developertools.restart.classloader;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.loading.ClassLoaderRepository;

import org.springframework.util.Assert;

/**
 * {@link ClassLoaderFileRepository} that maintains a collection of
 * {@link ClassLoaderFile} items grouped by source folders.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see ClassLoaderFile
 * @see ClassLoaderRepository
 */
public class ClassLoaderFiles implements ClassLoaderFileRepository, Serializable {

	private static final long serialVersionUID = 1;

	private final Map<String, SourceFolder> sourceFolders = new LinkedHashMap<String, SourceFolder>();

	public void addFile(String name, ClassLoaderFile file) {
		addFile("", name, file);
	}

	public void addFile(String sourceFolder, String name, ClassLoaderFile file) {
		Assert.notNull(sourceFolder, "SourceFolder must not be null");
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(file, "File must not be null");
		removeAll(name);
		getOrCreateSourceFolder(sourceFolder).add(name, file);
	}

	private void removeAll(String name) {
		for (SourceFolder sourceFolder : this.sourceFolders.values()) {
			sourceFolder.remove(name);
		}
	}

	protected final SourceFolder getOrCreateSourceFolder(String name) {
		SourceFolder sourceFolder = this.sourceFolders.get(name);
		if (sourceFolder == null) {
			sourceFolder = new SourceFolder(name);
			this.sourceFolders.put(name, sourceFolder);
		}
		return sourceFolder;
	}

	public Collection<SourceFolder> getSourceFolders() {
		return Collections.unmodifiableCollection(this.sourceFolders.values());
	}

	@Override
	public ClassLoaderFile getFile(String name) {
		for (SourceFolder sourceFolder : this.sourceFolders.values()) {
			ClassLoaderFile file = sourceFolder.get(name);
			if (file != null) {
				return file;
			}
		}
		return null;
	}

	public static class SourceFolder implements Serializable {

		private static final long serialVersionUID = 1;

		private final String name;

		private final Map<String, ClassLoaderFile> files = new LinkedHashMap<String, ClassLoaderFile>();

		SourceFolder(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public void add(String name, ClassLoaderFile file) {
			this.files.put(name, file);
		}

		public ClassLoaderFile get(String name) {
			return this.files.get(name);
		}

		public void remove(String name) {
			this.files.remove(name);
		}

		public Collection<ClassLoaderFile> getFiles() {
			return Collections.unmodifiableCollection(this.files.values());
		}

	}

}
