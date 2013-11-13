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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.springframework.boot.loader.AsciiString;
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
public class JarFile extends java.util.jar.JarFile implements Iterable<JarEntryData> {

	private static final AsciiString META_INF = new AsciiString("META-INF/");

	private static final AsciiString MANIFEST_MF = new AsciiString("META-INF/MANIFEST.MF");

	private final RandomAccessDataFile rootJarFile;

	private RandomAccessData data;

	private final String name;

	private final long size;

	private Map<AsciiString, JarEntryData> entries = new LinkedHashMap<AsciiString, JarEntryData>();

	private JarEntryData manifestData;

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
		loadJarEntries(filters);
	}

	private void loadJarEntries(JarEntryFilter[] filters) throws IOException {
		CentralDirectoryEndRecord endRecord = new CentralDirectoryEndRecord(this.data);
		RandomAccessData centralDirectory = endRecord.getCentralDirectory(this.data);
		InputStream inputStream = centralDirectory.getInputStream();
		try {
			JarEntryData data = JarEntryData.get(this, inputStream, centralDirectory);
			while (data != null) {
				addJarEntry(data, filters);
				data = JarEntryData.get(this, inputStream, centralDirectory);
			}
		}
		finally {
			inputStream.close();
		}
	}

	private void addJarEntry(JarEntryData entryData, JarEntryFilter[] filters) {
		AsciiString name = entryData.getName();
		for (JarEntryFilter filter : filters) {
			name = (filter == null || name == null ? name : filter.apply(name, entryData));
		}
		if (name != null) {
			entryData.setName(name);
			this.entries.put(name, entryData);
			if (name.startsWith(META_INF)) {
				processMetaInfEntry(name, entryData);
			}
		}
	}

	private void processMetaInfEntry(AsciiString name, JarEntryData entryData) {
		if (name.equals(MANIFEST_MF)) {
			this.manifestData = entryData;
		}
	}

	protected final RandomAccessDataFile getRootJarFile() {
		return this.rootJarFile;
	}

	@Override
	public Manifest getManifest() throws IOException {
		if (this.manifestData == null) {
			return null;
		}
		if (this.manifest == null) {
			InputStream inputStream = this.manifestData.getInputStream();
			try {
				this.manifest = new Manifest(inputStream);
			}
			finally {
				inputStream.close();
			}
		}
		return this.manifest;
	}

	@Override
	public Enumeration<java.util.jar.JarEntry> entries() {
		final Iterator<JarEntryData> iterator = iterator();
		return new Enumeration<java.util.jar.JarEntry>() {

			@Override
			public boolean hasMoreElements() {
				return iterator.hasNext();
			}

			@Override
			public java.util.jar.JarEntry nextElement() {
				return iterator.next().getJarEntry();
			}
		};
	}

	@Override
	public Iterator<JarEntryData> iterator() {
		return this.entries.values().iterator();
	}

	@Override
	public JarEntry getJarEntry(String name) {
		return (JarEntry) getEntry(name);
	}

	@Override
	public ZipEntry getEntry(String name) {
		if (name == null) {
			return null;
		}
		JarEntryData entryData = this.entries.get(new AsciiString(name));
		if (entryData == null && !name.endsWith("/")) {
			entryData = this.entries.get(new AsciiString(name + "/"));
		}
		return entryData.getJarEntry();
	}

	@Override
	public synchronized InputStream getInputStream(ZipEntry ze) throws IOException {
		return getContainedEntry(ze).getSource().getInputStream();
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
		return getNestedJarFile(getContainedEntry(ze).getSource());
	}

	/**
	 * Return a nested {@link JarFile} loaded from the specified entry.
	 * @param ze the zip entry
	 * @param filters an optional set of jar entry filters to be applied
	 * @return a {@link JarFile} for the entry
	 * @throws IOException
	 */
	public synchronized JarFile getNestedJarFile(final JarEntryData ze,
			JarEntryFilter... filters) throws IOException {
		if (ze.isDirectory()) {
			return getNestedJarFileFromDirectoryEntry(ze, filters);
		}
		return getNestedJarFileFromFileEntry(ze, filters);
	}

	private JarFile getNestedJarFileFromDirectoryEntry(JarEntryData entry,
			JarEntryFilter... filters) throws IOException {
		final AsciiString sourceName = entry.getName();
		JarEntryFilter[] filtersToUse = new JarEntryFilter[filters.length + 1];
		System.arraycopy(filters, 0, filtersToUse, 1, filters.length);
		filtersToUse[0] = new JarEntryFilter() {
			@Override
			public AsciiString apply(AsciiString entryName, JarEntryData ze) {
				if (entryName.startsWith(sourceName) && !entryName.equals(sourceName)) {
					return entryName.substring(sourceName.length());
				}
				return null;
			}
		};
		return new JarFile(this.rootJarFile, getName() + "!/"
				+ entry.getName().substring(0, sourceName.length() - 1), this.data,
				filtersToUse);
	}

	private JarFile getNestedJarFileFromFileEntry(JarEntryData entry,
			JarEntryFilter... filters) throws IOException {
		if (entry.getMethod() != ZipEntry.STORED) {
			throw new IllegalStateException("Unable to open nested compressed entry "
					+ entry.getName());
		}
		return new JarFile(this.rootJarFile, getName() + "!/" + entry.getName(),
				entry.getData(), filters);
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

	private synchronized JarEntry getContainedEntry(ZipEntry zipEntry) throws IOException {
		if (zipEntry != null && zipEntry instanceof JarEntry) {
			JarEntry jarEntry = (JarEntry) zipEntry;
			if (this.entries.get(jarEntry.getSource().getName()) == jarEntry.getSource()) {
				return jarEntry;
			}
		}
		throw new IllegalArgumentException("ZipEntry must be contained in this file");
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
