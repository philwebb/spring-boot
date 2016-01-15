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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

	private static final long LOCAL_FILE_HEADER_SIZE = 30;

	private final JarEntryFilter filter;

	private final List<JarFileEntry> entries = new ArrayList<JarFileEntry>();

	JarFileIndex(JarEntryFilter filter) {
		this.filter = filter;
	}

	@Override
	public void visitStart(CentralDirectoryEndRecord endRecord,
			RandomAccessData centralDirectoryData) {
	}

	@Override
	public void visitFileHeader(CentralDirectoryFileHeader fileHeader, int dataOffset) {
		AsciiBytes name = fileHeader.getName();
		name = (this.filter == null ? name : this.filter.apply(name));
		if (name != null) {
			JarFileEntry entry = new JarFileEntry(name.toString());
			entry.setCompressedSize(fileHeader.getCompressedSize());
			entry.setMethod(fileHeader.getMethod());
			entry.setCrc(fileHeader.getCrc());
			entry.setSize(fileHeader.getSize());
			entry.setExtra(fileHeader.getExtra());
			entry.setComment(fileHeader.getComment().toString());
			entry.setSize(fileHeader.getSize());
			entry.setTime(fileHeader.getTime());
			entry.setLocalHeaderOffset(fileHeader.getLocalHeaderOffset());
			this.entries.add(entry);
		}
	}

	@Override
	public void visitEnd() {
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Iterator<JarEntry> getEntries(RandomAccessData data) {
		return (Iterator) this.entries.iterator();
	}

	public boolean containsEntry(RandomAccessData data, String name) {
		return getEntry(data, name) != null;
	}

	public JarFileEntry getEntry(RandomAccessData data, String name) {
		for (JarFileEntry entry : this.entries) {
			if (entry.getName().equals(name)) {
				return entry;
			}
		}
		if (!name.endsWith("/")) {
			return getEntry(data, name + "/");
		}
		return null;
	}

	public InputStream getInputStream(RandomAccessData data, String name,
			ResourceAccess access) throws IOException {
		JarFileEntry entry = getEntry(data, name);
		InputStream inputStream = getData(data, entry).getInputStream(access);
		if (entry.getMethod() == ZipEntry.DEFLATED) {
			inputStream = new ZipInflaterInputStream(inputStream, (int) entry.getSize());
		}
		return inputStream;
	}

	public RandomAccessData getEntryData(RandomAccessData data, String name)
			throws IOException {
		JarFileEntry entry = getEntry(data, name);
		return getData(data, entry);

	}

	private RandomAccessData getData(RandomAccessData data, JarFileEntry entry)
			throws IOException {
		// aspectjrt-1.7.4.jar has a different ext bytes length in the
		// local directory to the central directory. We need to re-read
		// here to skip them
		byte[] localHeader = Bytes.get(
				data.getSubsection(entry.getLocalHeaderOffset(), LOCAL_FILE_HEADER_SIZE));
		long nameLength = Bytes.littleEndianValue(localHeader, 26, 2);
		long extraLength = Bytes.littleEndianValue(localHeader, 28, 2);
		return data.getSubsection(entry.getLocalHeaderOffset() + LOCAL_FILE_HEADER_SIZE
				+ nameLength + extraLength, entry.getCompressedSize());
	}

}
