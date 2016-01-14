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

	private SoftReference<JarEntry[]> entries;

	JarFileIndex(RandomAccessData data, CentralDirectoryEndRecord endRecord)
			throws IOException {
		this.centralDirectoryData = endRecord.getCentralDirectory(data);
		int numberOfRecords = endRecord.getNumberOfRecords();
		this.nameHashCodes = new int[numberOfRecords];
		this.centralDirectoryOffsets = new int[numberOfRecords];
		this.localHeaderOffsets = new int[numberOfRecords];
		InputStream inputStream = this.centralDirectoryData
				.getInputStream(ResourceAccess.ONCE);
		try {
			int centralDirectoryOffset = 0;
			for (int i = 0; i < numberOfRecords; i++) {
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
	}

	private int hashCode(AsciiBytes name) {
		return name.hashCode(); // FIXME;
	}

	public InputStream getInputStream(String name) {
		// for each hash code
		// load CDFH or use the entries if already loaded
		// check that the name matches
		// if it does, return the input stream

		return null;
	}

	@Override
	public Iterator<JarEntry> iterator() {
		return null;
	}

}
