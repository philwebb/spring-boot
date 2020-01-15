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

package org.springframework.boot.loader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Manifest;

import org.springframework.boot.loader.archive.Archive;

/**
 * Base class for executable archive {@link Launcher}s.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @since 1.0.0
 */
public abstract class ExecutableArchiveLauncher extends Launcher {

	private static final String START_CLASS_ATTRIBUTE = "Start-Class";

	protected static final String BOOT_CLASSPATH_INDEX_ATTRIBUTE = "Spring-Boot-Classpath-Index";

	private final Archive archive;

	private final IndexFile classPathIndex;

	public ExecutableArchiveLauncher() {
		try {
			this.archive = createArchive();
			this.classPathIndex = getClassPathIndex(this.archive);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	protected ExecutableArchiveLauncher(Archive archive) {
		try {
			this.archive = archive;
			this.classPathIndex = getClassPathIndex(this.archive);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	protected IndexFile getClassPathIndex(Archive archive) throws IOException {
		return IndexFile.NONE;
	}

	@Override
	protected String getMainClass() throws Exception {
		Manifest manifest = this.archive.getManifest();
		String mainClass = null;
		if (manifest != null) {
			mainClass = manifest.getMainAttributes().getValue(START_CLASS_ATTRIBUTE);
		}
		if (mainClass == null) {
			throw new IllegalStateException("No 'Start-Class' manifest entry specified in " + this);
		}
		return mainClass;
	}

	@Override
	protected ClassLoader createClassLoader(Iterator<Archive> archives) throws Exception {
		return super.createClassLoader(getUrls(archives).toArray(new URL[0]));
	}

	List<URL> getUrls(Iterator<Archive> archives) throws MalformedURLException {
		List<URL> urls = new ArrayList<>(50);
		while (archives.hasNext()) {
			urls.add(archives.next().getUrl());
		}
		addIndexedUrls(urls);
		return urls;
	}

	private void addIndexedUrls(List<URL> urls) {
		// FIXME
		// List<URL> indexedUrls = this.indexed.stream().map((f) -> {
		// try {
		// return new File(ExecutableArchiveLauncher.this.archive.getUrl().getFile() +
		// f).toURI().toURL();
		// }
		// catch (MalformedURLException ex) {
		// return null;
		// }
		// }).filter(Objects::nonNull).collect(Collectors.toList());
		// urls.addAll(indexedUrls);
	}

	@Override
	protected Iterator<Archive> getClassPathArchivesIterator() throws Exception {
		Archive.EntryFilter searchFilter = (entry) -> !isClassPathIndexed(entry) && isSearchCandidate(entry);
		Iterator<Archive> archives = this.archive.getNestedArchives(searchFilter, this::isNestedArchive);
		if (isPostProcessingClassPathArchives()) {
			archives = applyClassPathArchivePostProcessing(archives);
		}
		return archives;
	}

	// FIXME
	// if (!this.indexed.isEmpty()) {
	// Archive.EntryFilter updatedSearchFilter = getEntryFilterForIndex();
	// return this.archive.getNestedArchives(updatedSearchFilter,
	// this::isNestedArchive);
	// }

	// private Archive.EntryFilter getEntryFilterForIndex() {
	// Map<String, List<IndexedFile>> grouped = this.indexed.stream().map((name) -> {
	// int i = name.lastIndexOf("/") + 1;
	// return new IndexedFile(name.substring(0, i), name.substring(i));
	// }).collect(Collectors.groupingBy(IndexedFile::getRoot));
	// return ((entry) -> ExecutableArchiveLauncher.this.isSearchCandidate(entry)
	// && !grouped.containsKey(entry.getName()));
	// }

	private boolean isClassPathIndexed(Archive.Entry entry) {
		return true;
	}

	private Iterator<Archive> applyClassPathArchivePostProcessing(Iterator<Archive> archives) throws Exception {
		List<Archive> list = new ArrayList<>();
		while (archives.hasNext()) {
			list.add(archives.next());
		}
		postProcessClassPathArchives(list);
		return list.iterator();
	}

	/**
	 * Determine if the specified entry is a a candidate for further searching.
	 * @param entry the entry to check
	 * @return {@code true} if the entry is a candidate for further searching
	 */
	protected boolean isSearchCandidate(Archive.Entry entry) {
		return true;
	}

	/**
	 * Determine if the specified entry is a nested item that should be added to the
	 * classpath.
	 * @param entry the entry to check
	 * @return {@code true} if the entry is a nested item (jar or folder)
	 */
	protected abstract boolean isNestedArchive(Archive.Entry entry);

	/**
	 * Return if post processing needs to be applied to the archives. For back
	 * compatibility this method returns {@code true}, but subclasses that don't override
	 * {@link #postProcessClassPathArchives(List)} should provide an implementation that
	 * returns {@code false}.
	 * @return if the {@link #postProcessClassPathArchives(List)} method is implemented
	 */
	protected boolean isPostProcessingClassPathArchives() {
		return true;
	}

	/**
	 * Called to post-process archive entries before they are used. Implementations can
	 * add and remove entries.
	 * @param archives the archives
	 * @throws Exception if the post processing fails
	 * @see #isPostProcessingClassPathArchives()
	 */
	protected void postProcessClassPathArchives(List<Archive> archives) throws Exception {
	}

	@Override
	protected boolean supportsNestedJars() {
		return this.archive.supportsNestedJars();
	}

	/**
	 * Return the root archive.
	 * @return the root archive
	 */
	protected final Archive getArchive() {
		return this.archive;
	}

}
