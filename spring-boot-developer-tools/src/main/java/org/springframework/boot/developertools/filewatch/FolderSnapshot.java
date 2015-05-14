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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.developertools.filewatch.ChangedFile.Type;
import org.springframework.util.Assert;

/**
 * A snapshot of a folder at a given point in time.
 *
 * @author Phillip Webb
 */
class FolderSnapshot {

	private static final Set<String> DOT_FOLDERS = Collections
			.unmodifiableSet(new HashSet<String>(Arrays.asList(".", "..")));

	private final File folder;

	private final Date time;

	private Set<FileSnapshot> files;

	/**
	 * Create a new {@link FolderSnapshot} for the given folder.
	 * @param folder the source folder
	 */
	public FolderSnapshot(File folder) {
		Assert.notNull(folder, "Folder must not be null");
		Assert.isTrue(folder.isDirectory(), "Folder must not be a file");
		this.folder = folder;
		this.time = new Date();
		Set<FileSnapshot> files = new LinkedHashSet<FileSnapshot>();
		collectFiles(folder, files);
		this.files = Collections.unmodifiableSet(files);
	}

	private void collectFiles(File source, Set<FileSnapshot> result) {
		File[] children = source.listFiles();
		if (children != null) {
			for (File child : children) {
				if (child.isDirectory() && !DOT_FOLDERS.contains(child.getName())) {
					collectFiles(child, result);
				}
				else if (child.isFile()) {
					result.add(new FileSnapshot(child));
				}
			}
		}
	}

	public ChangedFiles getChangedFiles(FolderSnapshot snapshot) {
		Assert.notNull(snapshot, "Snapshot must not be null");
		Assert.isTrue(snapshot.folder.equals(this.folder),
				"Snapshot source folder must be '" + this.folder + "'");
		Set<ChangedFile> changes = new LinkedHashSet<ChangedFile>();
		Map<File, FileSnapshot> previousFiles = getFilesMap();
		for (FileSnapshot currentFile : snapshot.files) {
			FileSnapshot previousFile = previousFiles.remove(currentFile.getFile());
			if (previousFile == null) {
				changes.add(new ChangedFile(currentFile.getFile(), Type.ADD));
			}
			else if (!previousFile.equals(currentFile)) {
				changes.add(new ChangedFile(currentFile.getFile(), Type.MODIFY));
			}
		}
		for (FileSnapshot previousFile : previousFiles.values()) {
			changes.add(new ChangedFile(previousFile.getFile(), Type.DELETE));
		}
		return new ChangedFiles(this.folder, changes);
	}

	private Map<File, FileSnapshot> getFilesMap() {
		Map<File, FileSnapshot> files = new LinkedHashMap<File, FileSnapshot>();
		for (FileSnapshot file : this.files) {
			files.put(file.getFile(), file);
		}
		return files;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof FolderSnapshot) {
			FolderSnapshot other = (FolderSnapshot) obj;
			return this.folder.equals(other.folder) && this.files.equals(other.files);
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		int hashCode = this.folder.hashCode();
		hashCode = 31 * hashCode + this.files.hashCode();
		return hashCode;
	}

	/**
	 * Return the source folder of this snapshot.
	 * @return the source folder
	 */
	public File getFolder() {
		return this.folder;
	}

	@Override
	public String toString() {
		return this.folder + " snaphost at " + this.time;
	}

}
