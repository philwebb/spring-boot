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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Provides raw access to content from a regular or nested zip file. This class performs
 * the low level parsing of a zip file and provide access to raw entry data that it
 * contains. Unlike {@link java.util.zip.ZipFile}, this implementation can load content
 * from a zip file nested inside another file as long as the entry is not compressed.
 * <p>
 * In order to reduce memory consumption, this implementation stores only the the hash
 * code of the entry name, the central directory offset and the original position. Entries
 * are stored internally in {@code hashCode} order so that a binary search can be used to
 * quickly find an entry by name or determine if the zip file doesn't have a given entry.
 * <p>
 * {@link ZipContent} for a typical Spring Boot application JAR will have somewhere in the
 * region of 10,500 entries which should consume about 122K.
 * <p>
 * {@link ZipContent} results are cached and it is assumed that zip content will not
 * change once loaded. Only UTF-8 strings are supported by this parser.
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

	private static final Map<Source, ZipContent> cache = new HashMap<>();

	private final int[] nameHashCode;

	private final int[] centralDirectoryOffset;

	private final int[] position;

	protected ZipContent(int size) {
		this.nameHashCode = new int[size];
		this.centralDirectoryOffset = new int[size];
		this.position = new int[size];
	}

	public DataBlock getData() {
		return null;
	}

	@Override
	public Iterator<Entry> iterator() {
		return null;
	}

	@Override
	public void close() throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	FileHeader get(CharSequence name) {
		int nameHashCode = 0; // FIXME JDK does interesting trick to save two lookups
		// See ZipCoder.hash...
		FileHeader fileHeader = get(nameHashCode, name, NO_SUFFIX);
		if (fileHeader == null) {
			// FIXME udpate hash code
			fileHeader = get(nameHashCode, name, SLASH);
		}
		return null;
	}

	private FileHeader get(int nameHashCode, CharSequence name, char suffix) {
		int index = getFirstIndex(nameHashCode);
		while (index >= 0 && index < this.nameHashCode.length && this.nameHashCode[index] == nameHashCode) {
			FileHeader candidagte = getByIndex(index);
			if (candidagte.hasName(name, suffix)) {
				return candidagte;
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

	private FileHeader getByIndex(int index) {
		try {
			FileHeader fileHeader = this.entryCache.get(index);
			if (fileHeader != null) {
				return fileHeader;
			}
			long centralDirectoryOffset = this.centralDirectoryOffset[index];
			fileHeader = DunnnoFileHeader.from(centralDirectoryOffset);
			this.entryCache.put(index, fileHeader);
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

	public static ZipContent from(Path zip) {
		return from(new Source(zip.toAbsolutePath(), null));
	}

	public static ZipContent from(Path containerZip, String nestedEntryName) {
		return from(new Source(containerZip.toAbsolutePath(), nestedEntryName));
	}

	private static ZipContent from(Source source) {
		return null;
	}

	private static record Source(Path path, String nestedEntryName) {

	}

	public interface Entry {

		DataBlock getData();

	}

}
