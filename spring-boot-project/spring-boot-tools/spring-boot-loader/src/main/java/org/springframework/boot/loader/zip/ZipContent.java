/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader.zip;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides raw access to content from a regular or nested zip file. This class performs
 * the low level parsing of a zip file and provide access to raw entry data that it
 * contains. Unlike {@link java.util.zip.ZipFile}, this implementation can load content
 * from a zip file nested inside another file as long as the entry is not compressed.
 * <p>
 * In order to reduce memory consumption, this implementation stores only the the hash
 * code of the entry names, the central directory offsets and the original positions.
 * Entries are stored internally in {@code hashCode} order so that a binary search can be
 * used to quickly find an entry by name or determine if the zip file doesn't have a given
 * entry.
 * <p>
 * {@link ZipContent} for a typical Spring Boot application JAR will have somewhere in the
 * region of 10,500 entries which should consume about 122K.
 * <p>
 * {@link ZipContent} results are cached and it is assumed that zip content will not
 * change once loaded.
 * <p>
 * To release {@link ZipContent} resources, the {@link #close()} method should be called
 * explicitly or by try-with-resources.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 3.2.0
 */
public final class ZipContent implements Iterable<ZipContent.Entry>, Closeable {

	private static final char NO_SUFFIX = 0;

	private static final char SLASH = '/';

	protected static final int ENTRY_CACHE_SIZE = 25;

	private static final Map<Source, ZipContent> cache = new ConcurrentHashMap<>();

	private FileChannelDataBlock dataBlock;

	private int[] nameHashCode;

	private int[] centralDirectoryOffset;

	private int[] position;

	private ZipContent(int numberOfEntries) {
		this.nameHashCode = new int[numberOfEntries];
		this.centralDirectoryOffset = new int[numberOfEntries];
		this.position = new int[numberOfEntries];
	}

	/**
	 * Return the data block containing the zip data. This may be smaller than the
	 * original file since additional bytes are permitted at the front of a zip file.
	 * @return the zip data
	 */
	public DataBlock getData() {
		return null;
	}

	/**
	 * Iterate entries in the order that they were written to the zip file.
	 *
	 * @see Iterable#iterator()
	 */
	@Override
	public Iterator<Entry> iterator() {
		return null;
	}

	private ZipContent open() throws IOException {
		this.dataBlock.open();
		return this;
	}

	/**
	 * Close this jar file, releasing the underlying file if this was the last reference.
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		this.dataBlock.close();
	}

	Entry getEntry(CharSequence name) {
		int nameHashCode = 0; // FIXME JDK does interesting trick to save two lookups
		// See ZipCoder.hash...
		Entry fileHeader = get(nameHashCode, name, NO_SUFFIX);
		if (fileHeader == null) {
			// FIXME udpate hash code
			fileHeader = get(nameHashCode, name, SLASH);
		}
		return null;
	}

	private Entry get(int nameHashCode, CharSequence name, char suffix) {
		int index = getFirstIndex(nameHashCode);
		while (index >= 0 && index < this.nameHashCode.length && this.nameHashCode[index] == nameHashCode) {
			Entry candidate = getByIndex(index);
			if (candidate.hasName(name)) {
				return candidate;
			}
			index++;
		}
		return null;
	}

	private int getFirstIndex(int nameHashCode) {
		int index = Arrays.binarySearch(this.nameHashCode, 0, this.nameHashCode.length, nameHashCode);
		if (index < 0) {
			return -1;
		}
		while (index > 0 && this.nameHashCode[index - 1] == nameHashCode) {
			index--;
		}
		return index;
	}

	private Entry getByIndex(int index) {
		try {
			long centralDirectoryOffset = this.centralDirectoryOffset[index];
			Entry fileHeader = null;// ; DunnnoFileHeader.from(centralDirectoryOffset);
			if (true) {
				throw new IOException();
			}
			return fileHeader;
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private void sort(int left, int right) {
		// Quick sort algorithm, uses nameHashCode as the source but sorts all arrays
		if (left < right) {
			int pivot = this.nameHashCode[left + (right - left) / 2];
			int i = left;
			int j = right;
			while (i <= j) {
				while (this.nameHashCode[i] < pivot) {
					i++;
				}
				while (this.nameHashCode[j] > pivot) {
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
		swap(this.nameHashCode, i, j);
		swap(this.centralDirectoryOffset, i, j);
		swap(this.position, i, j);
	}

	protected static void swap(int[] array, int i, int j) {
		int temp = array[i];
		array[i] = array[j];
		array[j] = temp;
	}

	public static ZipContent from(Path zip) throws IOException {
		return from(new Source(zip.toAbsolutePath(), null));
	}

	public static ZipContent from(Path containerZip, String nestedEntryName) throws IOException {
		return from(new Source(containerZip.toAbsolutePath(), nestedEntryName));
	}

	private static ZipContent from(Source source) throws IOException {
		ZipContent zipContent = cache.get(source);
		if (zipContent != null) {
			return zipContent.open();
		}
		zipContent = load(source);
		ZipContent previouslyCached = cache.putIfAbsent(source, zipContent);
		if (previouslyCached != null) { // Someone else got to it
			zipContent.close();
			return previouslyCached.open();
		}
		return zipContent;
	}

	private static ZipContent load(Source source) throws IOException {
		FileChannelDataBlock dataBlock = getFileChannelDataBlock(source);
		EndOfCentralDirectoryRecord endOfCentralDirectoryRecord = EndOfCentralDirectoryRecord.load(dataBlock);
		Zip64EndOfCentralDirectoryLocator zip64EndOfCentralDirectoryLocator = Zip64EndOfCentralDirectoryLocator
			.find(dataBlock, endOfCentralDirectoryRecord);
		Zip64EndOfCentralDirectoryRecord zip64EndOfCentralDirectoryRecord = Zip64EndOfCentralDirectoryRecord
			.load(dataBlock, zip64EndOfCentralDirectoryLocator);

		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	private static FileChannelDataBlock getFileChannelDataBlock(Source source) throws IOException {
		if (source.nestedEntryName() == null) {
			return FileChannelDataBlock.open(source.path());
		}
		try (ZipContent containerZip = ZipContent.from(source.path())) {
			Entry nestedEntry = containerZip.getEntry(source.nestedEntryName());
			// FIXME check not compressed and is a not a directory
			// FIXME get the offset and size
			return containerZip.dataBlock.openSlice(-1, -1);
		}
	}

	private static record Source(Path path, String nestedEntryName) {

	}

	private static class Entries {

	}

	public class Entry {

		boolean hasName(CharSequence name) {
			throw new UnsupportedOperationException("Auto-generated method stub");
		}

	}

}
