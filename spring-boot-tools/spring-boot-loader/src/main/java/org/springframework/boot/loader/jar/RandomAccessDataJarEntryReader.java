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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Manifest;

import org.springframework.boot.loader.data.RandomAccessData;

/**
 * @author Phillip Webb
 */
class RandomAccessDataJarEntryReader {

	private static final long LOCAL_FILE_HEADER_SIZE = 30;

	private final ZipData zipData = new ZipData();

	private final List<JarEntry> entries;

	private Manifest manifest;

	private final Iterator<JarEntry> entriesIterator;

	public RandomAccessDataJarEntryReader(RandomAccessData data) throws IOException {
		ZipEndOfCentralDirectoryRecord endRecord = new ZipEndOfCentralDirectoryRecord(
				data);
		RandomAccessData centralDirectory = endRecord.getCentralDirectory(data);
		this.entries = new ArrayList<JarEntry>(endRecord.getNumberOfRecords());
		byte[] header = new byte[46];
		InputStream stream = centralDirectory.getInputStream();
		try {
			while (this.zipData.fillBytes(stream, header)) {
				JarEntry entry = createEntry(data, header, stream);
				this.entries.add(entry);
			}
		}
		finally {
			stream.close();
		}

		this.entriesIterator = this.entries.iterator();
	}

	private JarEntry createEntry(RandomAccessData data, byte[] header,
			InputStream inputStream) throws IOException {
		String name = this.zipData.readString(inputStream,
				LittleEndian.valueOf(header, 28, 2));
		byte[] extra = this.zipData.readBytes(inputStream,
				LittleEndian.valueOf(header, 30, 2));
		String comment = this.zipData.readString(inputStream,
				LittleEndian.valueOf(header, 32, 2));

		long compressedSize = LittleEndian.valueOf(header, 20, 4);
		long localFileOffset = LittleEndian.valueOf(header, 42, 4);
		localFileOffset += LOCAL_FILE_HEADER_SIZE + name.length() + extra.length;

		RandomAccessData entryData = data.getSubsection(localFileOffset, compressedSize);
		JarEntry entry = new JarEntry(name, entryData);

		entry.setCompressedSize(compressedSize);
		entry.setMethod((int) LittleEndian.valueOf(header, 10, 2));
		entry.setCrc(LittleEndian.valueOf(header, 16, 4));
		entry.setSize(LittleEndian.valueOf(header, 24, 4));
		entry.setExtra(extra);
		entry.setComment(comment);
		entry.setSize(LittleEndian.valueOf(header, 24, 4));
		entry.setTime(LittleEndian.valueOf(header, 12, 4));
		return entry;
	}

	private Manifest createManifest() throws IOException {
		int i = 0;
		for (JarEntry entry : this.entries) {
			if (entry.getName().equals("META-INF/MANIFEST.MF")) {
				InputStream inputStream = entry.getInputStream();
				try {
					return new Manifest(inputStream);
				}
				finally {
					inputStream.close();
				}
			}
			i++;
			if (i > 4) {
				return null;
			}
		}
		return null;
	}

	public JarEntry getNextEntry() throws IOException {
		if (this.entriesIterator.hasNext()) {
			return this.entriesIterator.next();
		}
		return null;
	}

	public Manifest getManifest() {
		if (this.manifest == null) {
			try {
				this.manifest = createManifest();
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}
		return this.manifest;
	}

	public void close() throws IOException {
		// FIXME delete
	}

}
