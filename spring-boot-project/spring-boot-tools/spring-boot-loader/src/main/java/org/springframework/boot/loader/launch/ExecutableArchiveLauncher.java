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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.springframework.boot.loader.launch.Archive.Entry;

/**
 * Base class for executable archive {@link Launcher}s.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 */
public abstract class ExecutableArchiveLauncher extends Launcher {

	private static final String START_CLASS_ATTRIBUTE = "Start-Class";

	protected static final String BOOT_CLASSPATH_INDEX_ATTRIBUTE = "Spring-Boot-Classpath-Index";

	protected static final String DEFAULT_CLASSPATH_INDEX_FILE_NAME = "classpath.idx";

	private final Archive rootArchive;

	private final ClassPathIndexFile classPathIndex;

	public ExecutableArchiveLauncher() throws Exception {
		this(createArchive());
	}

	protected ExecutableArchiveLauncher(Archive rootArchive) throws Exception {
		this.rootArchive = rootArchive;
		this.classPathIndex = getClassPathIndex(this.rootArchive);
	}

	protected ClassPathIndexFile getClassPathIndex(Archive archive) throws IOException {
		if (!archive.isExploded()) {
			return null; // Regular archives already have a defined order
		}
		String location = getClassPathIndexFileLocation(archive);
		return ClassPathIndexFile.loadIfPossible(archive.getUrl(), location);
	}

	private String getClassPathIndexFileLocation(Archive archive) throws IOException {
		Manifest manifest = archive.getManifest();
		Attributes attributes = (manifest != null) ? manifest.getMainAttributes() : null;
		String location = (attributes != null) ? attributes.getValue(BOOT_CLASSPATH_INDEX_ATTRIBUTE) : null;
		return (location != null) ? location : getArchiveEntryPathPrefix() + DEFAULT_CLASSPATH_INDEX_FILE_NAME;
	}

	@Override
	protected String getMainClass() throws Exception {
		Manifest manifest = this.rootArchive.getManifest();
		String mainClass = (manifest != null) ? manifest.getMainAttributes().getValue(START_CLASS_ATTRIBUTE) : null;
		if (mainClass == null) {
			throw new IllegalStateException("No 'Start-Class' manifest entry specified in " + this);
		}
		return mainClass;
	}

	@Override
	protected ClassLoader createClassLoader(Iterator<Archive> archives) throws Exception {
		List<URL> urls = new ArrayList<>(guessClassPathSize());
		archives.forEachRemaining((archive) -> urls.add(archive.getUrl()));
		if (this.classPathIndex != null) {
			urls.addAll(this.classPathIndex.getUrls());
		}
		return createClassLoader(urls.toArray(new URL[0]));
	}

	private int guessClassPathSize() {
		return (this.classPathIndex != null) ? this.classPathIndex.size() + 10 : DEFAULT_NUMBER_OF_CLASSPATH_URLS;
	}

	@Override
	protected Iterator<Archive> getArchives() throws Exception {
		Iterator<Archive> archives = this.rootArchive.getNestedArchives(this::isSearchCandidate, this::isIncluded);
		return postProcessClassPathArchives(archives);
	}

	/**
	 * Determine if the specified entry is a candidate for further searching.
	 * @param entry the entry to check
	 * @return {@code true} if the entry is a candidate for further searching
	 */
	protected boolean isSearchCandidate(Archive.Entry entry) {
		if (getArchiveEntryPathPrefix() == null) {
			return true;
		}
		return entry.getName().startsWith(getArchiveEntryPathPrefix());
	}

	private boolean isIncluded(Entry entry) {
		return isNestedArchive(entry) && !isEntryIndexed(entry);
	}

	private boolean isEntryIndexed(Archive.Entry entry) {
		return (this.classPathIndex != null) ? this.classPathIndex.containsEntry(entry.getName()) : false;
	}

	@Override
	protected final Archive getRootArchive() {
		return this.rootArchive;
	}

	@Override
	protected boolean isExploded() {
		return this.rootArchive.isExploded();
	}

	/**
	 * Determine if the specified entry is a nested item that should be added to the
	 * classpath.
	 * @param entry the entry to check
	 * @return {@code true} if the entry is a nested item (jar or directory)
	 */
	protected abstract boolean isNestedArchive(Archive.Entry entry);

	/**
	 * Apply any required post processing to the given archives. By default this method
	 * returns the original archives.
	 * @param archives the archives to post process
	 * @return the post processed archives
	 * @throws Exception on error
	 */
	protected Iterator<Archive> postProcessClassPathArchives(Iterator<Archive> archives) throws Exception {
		return archives;
	}

	/**
	 * Return the path prefix for entries in the archive.
	 * @return the path prefix
	 */
	protected String getArchiveEntryPathPrefix() {
		return null;
	}

}
