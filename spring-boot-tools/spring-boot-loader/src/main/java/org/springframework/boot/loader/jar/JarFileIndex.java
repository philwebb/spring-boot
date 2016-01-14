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
import java.util.jar.JarEntry;

import org.springframework.boot.loader.data.RandomAccessData;
import org.springframework.boot.loader.data.RandomAccessData.ResourceAccess;

/**
 * @author Phillip Webb
 */
class JarFileIndex implements Iterable<JarEntry> {

	private int CENTRAL_DIRECTORY_HEADER_BASE_SIZE = 46;

	private final RandomAccessData centralDirectoryData;

	private final int[] nameHashCodes;

	private final int[] centralDirectoryOffsets;

	private final int[] localHeaderOffsets;

	private SoftReference<JarEntry[]> entries = new SoftReference<JarEntry[]>(null);

	JarFileIndex(RandomAccessData data, CentralDirectoryEndRecord endRecord)
			throws IOException {
		this.centralDirectoryData = endRecord.getCentralDirectory(data);
		int size = endRecord.getNumberOfRecords();
		this.nameHashCodes = new int[size];
		this.centralDirectoryOffsets = new int[size];
		this.localHeaderOffsets = new int[size];
		parseCentralDirectory();
	}

	private void parseCentralDirectory() throws IOException {
		InputStream inputStream = this.centralDirectoryData
				.getInputStream(ResourceAccess.ONCE);
		try {
			int centralDirectoryOffset = 0;
			for (int i = 0; i < this.nameHashCodes.length; i++) {
				CentralDirectoryFileHeader header = CentralDirectoryFileHeader
						.fromInputStream(inputStream);
				this.nameHashCodes[i] = hashCode(header.getName());
				this.centralDirectoryOffsets[i] = centralDirectoryOffset;
				this.localHeaderOffsets[i] = (int) header.getLocalHeaderOffset();
				centralDirectoryOffset += this.CENTRAL_DIRECTORY_HEADER_BASE_SIZE
						+ header.getName().length() + +header.getComment().length()
						+ header.getExtra().length;
			}
		}
		finally {
			inputStream.close();
		}
		sort();
	}

	public void sort() {
		JarEntry[] entries = this.entries.get();
		sort(0, this.nameHashCodes.length - 1, entries);
		this.entries = new SoftReference<JarEntry[]>(entries);
	}

	private void sort(int left, int right, JarEntry[] jarEntries) {
		if (left < right) {
			int pivot = this.nameHashCodes[left + (right - left) / 2];
			int i = left;
			int j = right;
			while (i <= j) {
				while (this.nameHashCodes[i] < pivot) {
					i++;
				}
				while (this.nameHashCodes[j] > pivot) {
					j--;
				}
				if (i <= j) {
					swap(i, j, jarEntries);
					i++;
					j--;
				}
			}
			if (left < j) {
				sort(left, j, jarEntries);
			}
			if (right > i) {
				sort(i, right, jarEntries);
			}
		}
	}

	private void swap(int i, int j, JarEntry[] jarEntries) {
		if (i != j) {
			swap(this.nameHashCodes, i, j);
			swap(this.centralDirectoryOffsets, i, j);
			swap(this.localHeaderOffsets, i, j);
			if (jarEntries != null) {
				swap(jarEntries, i, j);
			}
		}
	}

	private void swap(Object[] array, int i, int j) {
		Object temp = array[i];
		array[i] = array[j];
		array[j] = temp;
	}

	private void swap(int[] array, int i, int j) {
		int temp = array[i];
		array[i] = array[j];
		array[j] = temp;
	}

	public InputStream getInputStream(String name) {
		getIndex(name);
		return null;
	}

	private int getIndex(String name) {
		int hashCode = hashCode(name);
		int index = getFirstIndex(hashCode);
		do {
			index++;
		}
		while (index < this.nameHashCodes.length
				&& this.nameHashCodes[index] == hashCode);
		// for each hash code
		// load CDFH or use the entries if already loaded
		// check that the name matches
		// if it does, return the input stream
		return -1;
	}

	private int getFirstIndex(int hashCode) {
		int index = Arrays.binarySearch(this.nameHashCodes, hashCode);
		if (index < 0) {
			return -1;
		}
		while (index > 0 && this.nameHashCodes[index - 1] == hashCode) {
			index--;
		}
		return index;
	}

	@Override
	public Iterator<JarEntry> iterator() {
		return null;
	}

	private int hashCode(String name) {
		return name.hashCode(); // FIXME;
	}

	private int hashCode(AsciiBytes name) {
		return name.toString().hashCode(); // FIXME;
	}

}
