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

package org.springframework.boot.loader.jar;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;

import org.springframework.boot.loader.data.RandomAccessData;
import org.springframework.boot.loader.data.RandomAccessData.ResourceAccess;
import org.springframework.boot.loader.util.AsciiBytes;

/**
 * Provides access to the collection of entries from a JarFile.
 *
 * @author Phillip Webb
 */
class JarFileEntries implements Iterable<JarEntryData> {

	private static final AsciiBytes SLASH = new AsciiBytes("/");

	private final JarFile source;

	private final List<JarEntryData> entries;

	private SoftReference<Map<AsciiBytes, JarEntryData>> entriesByName;

	JarFileEntries(JarFile source, CentralDirectoryEndRecord endRecord)
			throws IOException {
		this.source = source;
		this.entries = loadJarEntries(endRecord);
	}

	JarFileEntries(JarFile source, JarFileEntries entries, JarEntryFilter filter) {
		this.source = source;
		this.entries = filterEntries(entries.entries, filter);
	}

	private List<JarEntryData> loadJarEntries(CentralDirectoryEndRecord endRecord)
			throws IOException {
		RandomAccessData centralDirectory = endRecord
				.getCentralDirectory(this.source.getData());
		int numberOfRecords = endRecord.getNumberOfRecords();
		List<JarEntryData> entries = new ArrayList<JarEntryData>(numberOfRecords);
		InputStream inputStream = centralDirectory.getInputStream(ResourceAccess.ONCE);
		try {
			JarEntryData entry = JarEntryData.fromInputStream(this.source, inputStream);
			while (entry != null) {
				entries.add(entry);
				processEntry(entry);
				entry = JarEntryData.fromInputStream(this.source, inputStream);
			}
		}
		finally {
			inputStream.close();
		}
		return entries;
	}

	protected void processEntry(JarEntryData entry) {
	}

	private List<JarEntryData> filterEntries(List<JarEntryData> entries,
			JarEntryFilter filter) {
		List<JarEntryData> filteredEntries = new ArrayList<JarEntryData>(entries.size());
		for (JarEntryData entry : entries) {
			AsciiBytes name = entry.getName();
			name = (filter == null || name == null ? name : filter.apply(name, entry));
			if (name != null) {
				JarEntryData filteredCopy = entry.createFilteredCopy(this.source, name);
				filteredEntries.add(filteredCopy);
				processEntry(filteredCopy);
			}
		}
		return filteredEntries;
	}

	@Override
	public Iterator<JarEntryData> iterator() {
		return this.entries.iterator();
	}

	public JarEntryData getJarEntryData(AsciiBytes name) {
		if (name == null) {
			return null;
		}
		Map<AsciiBytes, JarEntryData> entriesByName = (this.entriesByName == null ? null
				: this.entriesByName.get());
		if (entriesByName == null) {
			entriesByName = new HashMap<AsciiBytes, JarEntryData>();
			for (JarEntryData entry : this.entries) {
				entriesByName.put(entry.getName(), entry);
			}
			this.entriesByName = new SoftReference<Map<AsciiBytes, JarEntryData>>(
					entriesByName);
		}

		JarEntryData entryData = entriesByName.get(name);
		if (entryData == null && !name.endsWith(SLASH)) {
			entryData = entriesByName.get(name.append(SLASH));
		}
		return entryData;
	}

	public InputStream getInputStream(AsciiBytes name) throws IOException {
		JarEntryData entryData = getJarEntryData(name);
		return (entryData == null ? null
				: getInputStream(entryData, ResourceAccess.PER_READ));
	}

	public InputStream getInputStream(JarEntryData entry, ResourceAccess access)
			throws IOException {
		InputStream inputStream = entry.getData().getInputStream(access);
		if (entry.getMethod() == ZipEntry.DEFLATED) {
			inputStream = new ZipInflaterInputStream(inputStream, entry.getSize());
		}
		return inputStream;

	}

}
