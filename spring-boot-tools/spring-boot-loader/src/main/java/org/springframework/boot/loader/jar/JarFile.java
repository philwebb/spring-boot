/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.loader.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.springframework.boot.loader.data.RandomAccessData;
import org.springframework.boot.loader.data.RandomAccessDataFile;

/**
 * Extended variant of {@link java.util.jar.JarFile} that behaves in the same way but
 * offers the following additional functionality.
 * <ul>
 * <li>Jar entries can be {@link JarEntryFilter filtered} during construction and new
 * filtered files can be {@link #getFilteredJarFile(JarEntryFilter...) created} from
 * existing files.</li>
 * <li>A nested {@link JarFile} can be
 * {@link #getNestedJarFile(ZipEntry, JarEntryFilter...) obtained} based on any directory
 * entry.</li>
 * <li>A nested {@link JarFile} can be
 * {@link #getNestedJarFile(ZipEntry, JarEntryFilter...) obtained} for embedded JAR files
 * (as long as their entry is not compressed).</li>
 * <li>Entry data can be accessed as {@link RandomAccessData}.</li>
 * </ul>
 * 
 * @author Phillip Webb
 */
public class JarFile extends java.util.jar.JarFile {

	private final RandomAccessDataFile rootJarFile;

	private RandomAccessData data;

	private final String name;

	private final long size;

	private Map<JarEntryName, java.util.jar.JarEntry> entries = new LinkedHashMap<JarEntryName, java.util.jar.JarEntry>();

	private Manifest manifest;

	/**
	 * Create a new {@link JarFile} backed by the specified file.
	 * @param file the root jar file
	 * @param filters an optional set of jar entry filters
	 * @throws IOException
	 */
	public JarFile(File file, JarEntryFilter... filters) throws IOException {
		this(new RandomAccessDataFile(file), filters);
	}

	/**
	 * Create a new {@link JarFile} backed by the specified file.
	 * @param file the root jar file
	 * @param filters an optional set of jar entry filters
	 * @throws IOException
	 */
	private JarFile(RandomAccessDataFile file, JarEntryFilter... filters)
			throws IOException {
		this(file, file.getFile().getPath(), file, filters);
	}

	/**
	 * Private constructor used to create a new {@link JarFile} either directly or from a
	 * nested entry.
	 * @param rootJarFile the root jar file
	 * @param name the name of this file
	 * @param data the underlying data
	 * @param filters an optional set of jar entry filters
	 * @throws IOException
	 */
	private JarFile(RandomAccessDataFile rootJarFile, String name, RandomAccessData data,
			JarEntryFilter... filters) throws IOException {
		super(rootJarFile.getFile());
		this.rootJarFile = rootJarFile;
		this.name = name;
		this.data = data;
		this.size = data.getSize();

		RandomAccessDataJarEntryReader entryReader = new RandomAccessDataJarEntryReader(
				data);
		try {
			JarEntry entry = entryReader.getNextEntry();
			while (entry != null) {
				addJarEntry(entry, filters);
				entry = entryReader.getNextEntry();
			}
			this.manifest = entryReader.getManifest();
		}
		finally {
			entryReader.close();
		}
	}

	private void addJarEntry(JarEntry entry, JarEntryFilter... filters) {
		String name = entry.getName();
		for (JarEntryFilter filter : filters) {
			name = (filter == null || name == null ? name : filter.apply(name, entry));
		}
		if (name != null) {
			entry.setName(name);
			this.entries.put(name, entry);
		}
	}

	protected final RandomAccessDataFile getRootJarFile() {
		return this.rootJarFile;
	}

	@Override
	public Manifest getManifest() throws IOException {
		return this.manifest;
	}

	@Override
	public Enumeration<java.util.jar.JarEntry> entries() {
		return Collections.enumeration(this.entries.values());
	}

	@Override
	public JarEntry getJarEntry(String name) {
		return (JarEntry) getEntry(name);
	}

	@Override
	public ZipEntry getEntry(String name) {
		java.util.jar.JarEntry entry = this.entries.get(name);
		if (entry == null && name != null && !name.endsWith("/")) {
			entry = this.entries.get(name + "/");
		}
		return entry;
	}

	@Override
	public synchronized InputStream getInputStream(ZipEntry ze) throws IOException {
		return getContainedEntry(ze).getInputStream();
	}

	/**
	 * Return a nested {@link JarFile} loaded from the specified entry.
	 * @param ze the zip entry
	 * @param filters an optional set of jar entry filters to be applied
	 * @return a {@link JarFile} for the entry
	 * @throws IOException
	 */
	public synchronized JarFile getNestedJarFile(final ZipEntry ze,
			JarEntryFilter... filters) throws IOException {
		if (ze == null) {
			throw new IllegalArgumentException("ZipEntry must not be null");
		}

		if (ze.isDirectory()) {
			return getNestedJarFileFromDirectoryEntry(ze, filters);
		}

		return getNestedJarFileFromFileEntry(ze, filters);
	}

	private JarFile getNestedJarFileFromDirectoryEntry(final ZipEntry entry,
			JarEntryFilter... filters) throws IOException {
		final String name = entry.getName();
		JarEntryFilter[] filtersToUse = new JarEntryFilter[filters.length + 1];
		System.arraycopy(filters, 0, filtersToUse, 1, filters.length);
		filtersToUse[0] = new JarEntryFilter() {
			@Override
			public String apply(String entryName, java.util.jar.JarEntry ze) {
				if (entryName.startsWith(name) && !entryName.equals(name)) {
					return entryName.substring(entry.getName().length());
				}
				return null;
			}
		};
		return new JarFile(this.rootJarFile, getName() + "!/"
				+ name.substring(0, name.length() - 1), this.data, filtersToUse);
	}

	private JarFile getNestedJarFileFromFileEntry(ZipEntry entry,
			JarEntryFilter... filters) throws IOException {
		if (entry.getMethod() != ZipEntry.STORED) {
			throw new IllegalStateException("Unable to open nested compressed entry "
					+ entry.getName());
		}
		return new JarFile(this.rootJarFile, getName() + "!/" + entry.getName(),
				getContainedEntry(entry).getData(), filters);
	}

	/**
	 * Return a new jar based on the filtered contents of this file.
	 * @param filters the set of jar entry filters to be applied
	 * @return a filtered {@link JarFile}
	 * @throws IOException
	 */
	public synchronized JarFile getFilteredJarFile(JarEntryFilter... filters)
			throws IOException {
		return new JarFile(this.rootJarFile, getName(), this.data, filters);
	}

	private synchronized JarEntry getContainedEntry(ZipEntry ze) throws IOException {
		if (!this.entries.containsValue(ze)) {
			throw new IllegalArgumentException("ZipEntry must be contained in this file");
		}
		return ((JarEntry) ze);
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public int size() {
		return (int) this.size;
	}

	@Override
	public void close() throws IOException {
		this.rootJarFile.close();
	}

	@Override
	public String toString() {
		return getName();
	}

	/**
	 * Return a URL that can be used to access this JAR file. NOTE: the specified URL
	 * cannot be serialized and or cloned.
	 * @return the URL
	 * @throws MalformedURLException
	 */
	public URL getUrl() throws MalformedURLException {
		JarURLStreamHandler handler = new JarURLStreamHandler(this);
		return new URL("jar", "", -1, "file:" + getName() + "!/", handler);
	}

}
