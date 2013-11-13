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
		EndOfCentralDirectoryRecord endRecord = new EndOfCentralDirectoryRecord(data);
		RandomAccessData centralDirectory = endRecord.getCentralDirectory(data);
		this.entries = new ArrayList<JarEntry>(
				endRecord.getNumberOfRecords());
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
				ZipData.getValue(header, 28, 2));
		byte[] extra = this.zipData.readBytes(inputStream,
				ZipData.getValue(header, 30, 2));
		String comment = this.zipData.readString(inputStream,
				ZipData.getValue(header, 32, 2));

		long compressedSize = ZipData.getValue(header, 20, 4);
		long localFileOffset = ZipData.getValue(header, 42, 4);
		localFileOffset += LOCAL_FILE_HEADER_SIZE + name.length() + extra.length;

		RandomAccessData entryData = data.getSubsection(localFileOffset, compressedSize);
		JarEntry entry = new JarEntry(name, entryData);

		entry.setCompressedSize(compressedSize);
		entry.setMethod((int) ZipData.getValue(header, 10, 2));
		entry.setCrc(ZipData.getValue(header, 16, 4));
		entry.setSize(ZipData.getValue(header, 24, 4));
		entry.setExtra(extra);
		entry.setComment(comment);
		entry.setSize(ZipData.getValue(header, 24, 4));
		entry.setTime(ZipData.getValue(header, 12, 4));
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

	private class EndOfCentralDirectoryRecord {

		private static final int MINIMUM_SIZE = 22;

		private static final int MAXIMUM_COMMENT_LENGTH = 0xFFFF;

		private static final int MAXIMUM_SIZE = MINIMUM_SIZE + MAXIMUM_COMMENT_LENGTH;

		private static final int SIGNATURE = 0x06054b50;

		private static final int COMMENT_LENGTH_OFFSET = 20;

		private static final int READ_BLOCK_SIZE = 256;

		private int size;

		private byte[] block;

		public EndOfCentralDirectoryRecord(RandomAccessData data) throws IOException {
			this.block = createBlockFromEndOfData(data, READ_BLOCK_SIZE);
			this.size = MINIMUM_SIZE;
			while (!isValid()) {
				this.size++;
				if (this.size > this.block.length) {
					if (this.size >= MAXIMUM_SIZE || this.size > data.getSize()) {
						throw new IOException("Unable to find ZIP central directory "
								+ "records after reading " + this.size + " bytes");
					}
					this.block = createBlockFromEndOfData(data, this.size
							+ READ_BLOCK_SIZE);

				}
			}
		}

		private byte[] createBlockFromEndOfData(RandomAccessData data, int size)
				throws IOException {
			long length = Math.min(data.getSize(), size);
			return RandomAccessDataJarEntryReader.this.zipData.readBytes(data
					.getSubsection(data.getSize() - length, length));
		}

		private boolean isValid() {
			if (this.block.length < MINIMUM_SIZE || getValue(0, 4) != SIGNATURE) {
				return false;
			}
			// Total size must be the structure size + comment
			long commentLength = getValue(COMMENT_LENGTH_OFFSET, 2);
			return this.size == MINIMUM_SIZE + commentLength;
		}

		public RandomAccessData getCentralDirectory(RandomAccessData data) {
			long offset = getValue(16, 4);
			long length = getValue(12, 4);
			return data.getSubsection(offset, length);
		}

		public int getNumberOfRecords() {
			return (int) getValue(10, 2);
		}

		private long getValue(int offset, int length) {
			int start = this.block.length - this.size;
			return ZipData.getValue(this.block, start + offset, length);
		}
	}

}
