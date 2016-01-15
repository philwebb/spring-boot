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
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;

import org.springframework.boot.loader.data.RandomAccessData;
import org.springframework.boot.loader.data.RandomAccessData.ResourceAccess;

/**
 * Maintains an index of entries.
 *
 * @author Phillip Webb
 */
class JarFileIndex implements CentralDirectoryVistor {

	private static final String SLASH = "/";

	private static final String NO_SUFFIX = "";

	private final JarFile jarFile;

	private final JarEntryFilter filter;

	private RandomAccessData centralDirectoryData;

	private int size;

	private int[] hashCodes;

	private int[] centralDirectoryOffsets;

	private int[] localHeaderOffsets;

	private int[] positions;

	private SoftReference<JarFileEntry[]> entries = new SoftReference<JarFileEntry[]>(
			null);

	JarFileIndex(JarFile jarFile, JarEntryFilter filter) {
		this.jarFile = jarFile;
		this.filter = filter;
	}

	@Override
	public void visitStart(CentralDirectoryEndRecord endRecord,
			RandomAccessData centralDirectoryData) {
		int maxSize = endRecord.getNumberOfRecords();
		this.centralDirectoryData = centralDirectoryData;
		this.hashCodes = new int[maxSize];
		this.centralDirectoryOffsets = new int[maxSize];
		this.localHeaderOffsets = new int[maxSize];
		this.positions = new int[maxSize];
	}

	@Override
	public void visitFileHeader(CentralDirectoryFileHeader fileHeader, int dataOffset) {
		AsciiBytes name = applyFilter(fileHeader.getName());
		if (name != null) {
			add(name, fileHeader, dataOffset);
		}
	}

	private void add(AsciiBytes name, CentralDirectoryFileHeader fileHeader,
			int dataOffset) {
		this.hashCodes[this.size] = name.hashCode();
		this.centralDirectoryOffsets[this.size] = dataOffset;
		this.localHeaderOffsets[this.size] = (int) fileHeader.getLocalHeaderOffset();
		this.positions[this.size] = this.size;
		this.size++;
	}

	@Override
	public void visitEnd() {
		sort(0, this.size - 1);
		int[] positions = this.positions;
		this.positions = new int[positions.length];
		for (int i = 0; i < positions.length; i++) {
			this.positions[positions[i]] = i;
		}
	}

	private void sort(int left, int right) {
		if (left < right) {
			int pivot = this.hashCodes[left + (right - left) / 2];
			int i = left;
			int j = right;
			while (i <= j) {
				while (this.hashCodes[i] < pivot) {
					i++;
				}
				while (this.hashCodes[j] > pivot) {
					j--;
				}
				if (i <= j) {
					swap(i, j);
					i++;
					j--;
				}
			}
			if (left < j) {
				sort(left, j);
			}
			if (right > i) {
				sort(i, right);
			}
		}
	}

	private void swap(int i, int j) {
		swap(this.hashCodes, i, j);
		swap(this.centralDirectoryOffsets, i, j);
		swap(this.localHeaderOffsets, i, j);
		swap(this.positions, i, j);
	}

	private void swap(int[] array, int i, int j) {
		int temp = array[i];
		array[i] = array[j];
		array[j] = temp;
	}

	public Iterator<JarEntry> getEntries() {
		return new EntryIterator(getOrCreateEntries());
	}

	public boolean containsEntry(String name) throws IOException {
		return getLocalFileHeader(name) != null;
	}

	public JarFileEntry getEntry(String name) {
		JarFileEntry[] entries = getOrCreateEntries();
		int hashCode = AsciiBytes.hashCode(name);
		JarFileEntry entry = findEntry(entries, hashCode, name, NO_SUFFIX);
		if (entry == null) {
			hashCode = AsciiBytes.hashCode(hashCode, SLASH);
			entry = findEntry(entries, hashCode, name, SLASH);
		}
		return entry;
	}

	private JarFileEntry[] getOrCreateEntries() {
		JarFileEntry[] entries = this.entries.get();
		if (entries == null) {
			entries = new JarFileEntry[this.size];
			this.entries = new SoftReference<JarFileEntry[]>(entries);
		}
		return entries;
	}

	private JarFileEntry findEntry(JarFileEntry[] entries, int hashCode, String name,
			String suffix) {
		int index = getFirstIndex(hashCode);
		while (index >= 0 && index < this.size && this.hashCodes[index] == hashCode) {
			JarFileEntry entry = getEntry(entries, index);
			if (isNameMatch(entry.getName(), name, suffix)) {
				return entry;
			}
			index++;
		}
		return null;
	}

	private JarFileEntry getEntry(JarFileEntry[] entries, int index) {
		JarFileEntry entry = entries[index];
		if (entry == null) {
			try {
				CentralDirectoryFileHeader header = CentralDirectoryFileHeader
						.fromRandomAccessData(this.centralDirectoryData,
								this.centralDirectoryOffsets[index]);
				entry = new JarFileEntry(this.jarFile,
						applyFilter(header.getName()).toString());
				entry.setCompressedSize(header.getCompressedSize());
				entry.setMethod(header.getMethod());
				entry.setCrc(header.getCrc());
				entry.setSize(header.getSize());
				entry.setExtra(header.getExtra());
				entry.setComment(header.getComment().toString());
				entry.setSize(header.getSize());
				entry.setTime(header.getTime());
				entries[index] = entry;
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}
		return entry;
	}

	private boolean isNameMatch(String candidate, String name, String suffix) {
		return (candidate.length() == name.length() + suffix.length())
				&& candidate.startsWith(name) && candidate.endsWith(suffix);
	}

	public InputStream getInputStream(String name, ResourceAccess access)
			throws IOException {
		LocalFileHeader header = getLocalFileHeader(name);
		if (header == null) {
			return null;
		}
		InputStream inputStream = getEntryData(header).getInputStream(access);
		if (header.getMethod() == ZipEntry.DEFLATED) {
			inputStream = new ZipInflaterInputStream(inputStream, header.getSize());
		}
		return inputStream;
	}

	public RandomAccessData getEntryData(String name) throws IOException {
		LocalFileHeader header = getLocalFileHeader(name);
		return getEntryData(header);
	}

	private RandomAccessData getEntryData(LocalFileHeader header) {
		if (header == null) {
			return null;
		}
		long offset = header.getContentOffset();
		int size = header.getCompressedSize();
		return this.jarFile.getData().getSubsection(offset, size);
	}

	private LocalFileHeader getLocalFileHeader(String name) throws IOException {
		int hashCode = AsciiBytes.hashCode(name);
		LocalFileHeader localEntry = findLocalFileHeader(hashCode, name, NO_SUFFIX);
		if (localEntry == null && !name.endsWith(SLASH)) {
			hashCode = AsciiBytes.hashCode(hashCode, SLASH);
			localEntry = findLocalFileHeader(hashCode, name, SLASH);
		}
		return localEntry;
	}

	private LocalFileHeader findLocalFileHeader(int hashCode, String name, String suffix)
			throws IOException {
		int index = getFirstIndex(hashCode);
		while (index >= 0 && index < this.size && this.hashCodes[index] == hashCode) {
			LocalFileHeader header = LocalFileHeader.fromRandomAccessData(
					this.jarFile.getData(), this.localHeaderOffsets[index]);
			if (applyFilter(header.getName()).equalsString(name, suffix)) {
				return header;
			}
			index++;
		}
		return null;
	}

	private int getFirstIndex(int hashCode) {
		int index = Arrays.binarySearch(this.hashCodes, 0, this.size, hashCode);
		if (index < 0) {
			return -1;
		}
		while (index > 0 && this.hashCodes[index - 1] == hashCode) {
			index--;
		}
		return index;
	}

	private AsciiBytes applyFilter(AsciiBytes name) {
		return (this.filter == null ? name : this.filter.apply(name));
	}

	private class EntryIterator implements Iterator<JarEntry> {

		private int index = 0;

		private JarFileEntry[] entries;

		public EntryIterator(JarFileEntry[] entries) {
			this.entries = entries;
		}

		@Override
		public boolean hasNext() {
			return this.index < this.entries.length;
		}

		@Override
		public JarEntry next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			int entryIndex = JarFileIndex.this.positions[this.index];
			this.index++;
			return getEntry(this.entries, entryIndex);
		}

	}

}
