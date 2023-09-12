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
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
 * @since 3.2.0
 */
class ExplodedArchive implements Archive {

	private static final Object NO_MANIFEST = new Object();

	private final File root;

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

	public Iterator<Archive> getNestedArchives(Predicate<Entry> searchFilter, Predicate<Entry> includeFilter)
			throws IOException {
		return new NestedArchives(this.root, this.recursive, searchFilter, includeFilter);
	}

	@Override
	public List<URL> getClassPathUrls(Predicate<Entry> searchFilter, Predicate<Entry> includeFilter)
			throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
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
	private static class NestedArchives implements Iterator<Archive> {

		private static final Set<String> SKIPPED_NAMES = new HashSet<>(Arrays.asList(".", ".."));

		private static final Predicate<Entry> INCLUDE_ALL = (entry) -> true;

		private static final Comparator<File> entryComparator = Comparator.comparing(File::getAbsolutePath);

		private final File root;

		private final boolean recursive;

		private final Predicate<Entry> searchFilter;

		private final Predicate<Entry> includeFilter;

		private final Deque<Iterator<File>> stack = new LinkedList<>();

		private FileEntry current;

		private final String rootUriPath;

		NestedArchives(File root, boolean recursive, Predicate<Entry> searchFilter, Predicate<Entry> includeFilter) {
			this.root = root;
			this.rootUriPath = this.root.toURI().getPath();
			this.recursive = recursive;
			this.searchFilter = (searchFilter != null) ? searchFilter : INCLUDE_ALL;
			this.includeFilter = (includeFilter != null) ? includeFilter : INCLUDE_ALL;
			this.stack.add(listFiles(root));
			this.current = poll();
		}

		@Override
		public boolean hasNext() {
			return this.current != null;
		}

		@Override
		public Archive next() {
			FileEntry entry = this.current;
			if (entry == null) {
				throw new NoSuchElementException();
			}
			this.current = poll();
			try {
				File file = entry.getFile();
				return (file.isDirectory() ? new ExplodedArchive(file) : new SimpleJarFileArchive(entry));
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
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
			String name = file.toURI().getPath().substring(this.rootUriPath.length());
			return new FileEntry(name, file);
		}

		private boolean isListable(FileEntry entry) {
			return entry.isDirectory() && (this.recursive || isImmediateChild(entry)) && this.searchFilter.test(entry)
					&& !this.includeFilter.test(entry);
		}

		private boolean isImmediateChild(FileEntry entry) {
			return entry.getFile().getParentFile().equals(this.root);
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

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public boolean isDirectory() {
			return this.file.isDirectory();
		}

		File getFile() {
			return this.file;
		}

	}

	/**
	 * {@link Archive} implementation backed by a simple JAR file that doesn't itself
	 * contain nested archives.
	 */
	private static class SimpleJarFileArchive implements Archive {

		private final URL url;

		SimpleJarFileArchive(FileEntry fileEntry) {
			try {
				this.url = fileEntry.getFile().toURI().toURL();
			}
			catch (MalformedURLException ex) {
				throw new UncheckedIOException(ex);
			}
		}

		@Override
		public Manifest getManifest() throws IOException {
			return null;
		}

		public Iterator<Archive> getNestedArchives(Predicate<Entry> searchFilter, Predicate<Entry> includeFilter)
				throws IOException {
			return Collections.emptyIterator();
		}

		@Override
		public String toString() {
			return this.url.toString();
		}

		@Override
		public List<URL> getClassPathUrls(Predicate<Entry> searchFilter, Predicate<Entry> includeFilter)
				throws IOException {
			throw new UnsupportedOperationException("Auto-generated method stub");
		}

	}

}
