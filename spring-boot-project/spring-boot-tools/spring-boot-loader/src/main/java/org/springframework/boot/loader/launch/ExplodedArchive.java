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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.Manifest;

/**
 * {@link Archive} implementation backed by an exploded archive directory.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class ExplodedArchive implements Archive {

	private static final Object NO_MANIFEST = new Object();

	private final File root;

	private final String rootUriPath;

	private final boolean recursive;

	private volatile Object manifest;

	/**
	 * Create a new {@link ExplodedArchive} instance.
	 * @param root the root directory
	 * @throws IOException on IO error
	 */
	public ExplodedArchive(File root) throws IOException {
		this(root, true);
	}

	/**
	 * Create a new {@link ExplodedArchive} instance.
	 * @param root the root directory
	 * @param recursive if recursive searching should be used to locate the manifest.
	 * Defaults to {@code true}, directories with a large tree might want to set this to
	 * {@code false}.
	 * @throws IOException on IO error
	 */
	public ExplodedArchive(File root, boolean recursive) throws IOException {
		if (!root.exists() || !root.isDirectory()) {
			throw new IllegalArgumentException("Invalid source directory " + root);
		}
		this.root = root;
		this.rootUriPath = ExplodedArchive.this.root.toURI().getPath();
		this.recursive = recursive;
	}

	@Override
	public Manifest getManifest() throws IOException {
		Object manifest = this.manifest;
		if (manifest == null) {
			manifest = loadManifest();
			this.manifest = manifest;
		}
		return (manifest != NO_MANIFEST) ? (Manifest) manifest : null;
	}

	private Object loadManifest() throws IOException {
		File file = new File(this.root, "META-INF/MANIFEST.MF");
		if (!file.exists()) {
			return NO_MANIFEST;
		}
		try (FileInputStream inputStream = new FileInputStream(file)) {
			return new Manifest(inputStream);
		}
	}

	@Override
	public Set<URL> getClassPathUrls(Predicate<Entry> searchFilter, Predicate<Entry> includeFilter) throws IOException {
		Set<URL> urls = new LinkedHashSet<>();
		for (FileEntryIterator iterator = new FileEntryIterator(searchFilter, includeFilter); iterator.hasNext();) {
			urls.add(iterator.next().getUrl());
		}
		return urls;
	}

	public File getRoot() {
		return this.root;
	}

	@Override
	public String toString() {
		return this.root.toString();
	}

	/**
	 * Nested archives contained in the exploded archive.
	 */
	private class FileEntryIterator implements Iterator<FileEntry> {

		// FIXME try to inline

		private static final Set<String> SKIPPED_NAMES = new HashSet<>(Arrays.asList(".", ".."));

		private static final Predicate<Entry> INCLUDE_ALL = (entry) -> true;

		private static final Comparator<File> entryComparator = Comparator.comparing(File::getAbsolutePath);

		private final Predicate<Entry> searchFilter;

		private final Predicate<Entry> includeFilter;

		private final Deque<Iterator<File>> stack = new LinkedList<>();

		private FileEntry current;

		FileEntryIterator(Predicate<Entry> searchFilter, Predicate<Entry> includeFilter) {
			this.searchFilter = (searchFilter != null) ? searchFilter : INCLUDE_ALL;
			this.includeFilter = (includeFilter != null) ? includeFilter : INCLUDE_ALL;
			this.stack.add(listFiles(ExplodedArchive.this.root));
			this.current = poll();
		}

		@Override
		public boolean hasNext() {
			return this.current != null;
		}

		@Override
		public FileEntry next() {
			FileEntry entry = this.current;
			if (entry == null) {
				throw new NoSuchElementException();
			}
			this.current = poll();
			return entry;
		}

		private FileEntry poll() {
			while (!this.stack.isEmpty()) {
				while (this.stack.peek().hasNext()) {
					File file = this.stack.peek().next();
					if (SKIPPED_NAMES.contains(file.getName())) {
						continue;
					}
					FileEntry entry = getFileEntry(file);
					if (isListable(entry)) {
						this.stack.addFirst(listFiles(file));
					}
					if (this.includeFilter.test(entry)) {
						return entry;
					}
				}
				this.stack.poll();
			}
			return null;
		}

		private FileEntry getFileEntry(File file) {
			String name = file.toURI().getPath().substring(ExplodedArchive.this.rootUriPath.length());
			return new FileEntry(name, file);
		}

		private boolean isListable(FileEntry entry) {
			return entry.isDirectory() && (ExplodedArchive.this.recursive || isImmediateChild(entry))
					&& this.searchFilter.test(entry) && !this.includeFilter.test(entry);
		}

		private boolean isImmediateChild(FileEntry entry) {
			return entry.isImmediateChildOf(ExplodedArchive.this.root);
		}

		private Iterator<File> listFiles(File file) {
			File[] files = file.listFiles();
			if (files == null) {
				return Collections.emptyIterator();
			}
			Arrays.sort(files, entryComparator);
			return Arrays.asList(files).iterator();
		}

	}

	/**
	 * {@link Entry} backed by a File.
	 */
	private static class FileEntry implements Entry {

		private final String name;

		private final File file;

		FileEntry(String name, File file) {
			this.name = name;
			this.file = file;
		}

		boolean isImmediateChildOf(File parent) {
			return this.file.getParentFile().equals(parent);
		}

		URL getUrl() throws MalformedURLException {
			return this.file.toURI().toURL();
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public boolean isDirectory() {
			return this.file.isDirectory();
		}

	}

}
