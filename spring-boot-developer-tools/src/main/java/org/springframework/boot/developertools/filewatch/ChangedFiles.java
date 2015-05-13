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

package org.springframework.boot.developertools.filewatch;

import java.io.File;
import java.util.Collections;
import java.util.Set;

/**
 * A collections of files from a specific source folder that have changed.
 *
 * @author Phillip Webb
 * @see FileChangeListener
 */
public final class ChangedFiles {

	private final File sourceFolder;

	private final Set<ChangedFile> files;

	private Set<String> relativeNames;

	public ChangedFiles(File sourceFolder, Set<ChangedFile> files) {
		this.sourceFolder = sourceFolder;
		this.files = Collections.unmodifiableSet(files);
	}

	/**
	 * The source folder being watched.
	 * @return the source folder
	 */
	public File getSourceFolder() {
		return this.sourceFolder;
	}

	/**
	 * The files that have been changed.
	 * @return the changed files
	 */
	public Set<ChangedFile> getFiles() {
		return this.files;
	}

	// FIXME
	// /**
	// * The files names relative to the source folder.
	// * @return the relative names
	// */
	// public Set<String> getRelativeFileNames() {
	// if (this.relativeNames == null) {
	// Set<String> relativeNames = new LinkedHashSet<String>();
	// String sourcePath = this.sourceFolder.getAbsoluteFile().getPath();
	// for (File file : this.files) {
	// relativeNames.add(file.getAbsoluteFile().getPath()
	// .substring(sourcePath.length() + 1));
	// }
	// this.relativeNames = Collections.unmodifiableSet(relativeNames);
	// }
	// return this.relativeNames;
	// }

	@Override
	public int hashCode() {
		return this.files.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj instanceof ChangedFiles) {
			ChangedFiles other = (ChangedFiles) obj;
			return this.sourceFolder.equals(other.sourceFolder)
					&& this.files.equals(other.files);
		}
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return this.sourceFolder + " " + this.files;
	}
}
