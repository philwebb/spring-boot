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
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;

import org.springframework.boot.loader.AsciiString;
import org.springframework.boot.loader.data.RandomAccessData;

/**
 * Holds the underlying data of a {@link JarEntry}, allowing creation to be deferred until
 * the entry is actually needed.
 * 
 * @author Phillip Webb
 */
public final class JarEntryData {

	private static final long LOCAL_FILE_HEADER_SIZE = 30;

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private JarFile source;

	private AsciiString name;

	private final byte[] extra;

	private final byte[] comment;

	private final byte[] header;

	private final RandomAccessData entryData;

	private volatile JarEntry entry;

	private long offset;

	private long compressedSize;

	public JarEntryData(JarFile source, byte[] header, InputStream inputStream,
			RandomAccessData data) throws IOException {
		this.source = source;
		long nameLength = Bytes.littleEndianValue(header, 28, 2);
		this.name = new AsciiString(Bytes.get(inputStream, nameLength));
		this.extra = Bytes.get(inputStream, Bytes.littleEndianValue(header, 30, 2));
		this.comment = Bytes.get(inputStream, Bytes.littleEndianValue(header, 32, 2));
		this.header = header;
		this.offset = Bytes.littleEndianValue(this.header, 42, 4)
				+ LOCAL_FILE_HEADER_SIZE + this.name.length() + this.extra.length;
		this.compressedSize = Bytes.littleEndianValue(this.header, 20, 4);
		RandomAccessData entryData = this.entryData.getSubsection(this.offset,
				this.compressedSize);
		this.entryData = entryData;
	}

	JarFile getSource() {
		return this.source;
	}

	void setName(AsciiString name) {
		this.name = name;
	}

	public AsciiString getName() {
		return this.name;
	}

	public JarEntry getJarEntry() {
		if (this.entry == null) {
			JarEntry entry = new JarEntry(this);
			entry.setCompressedSize(this.compressedSize);
			entry.setMethod((int) Bytes.littleEndianValue(this.header, 10, 2));
			entry.setCrc(Bytes.littleEndianValue(this.header, 16, 4));
			entry.setSize(Bytes.littleEndianValue(this.header, 24, 4));
			entry.setExtra(this.extra);
			entry.setComment(new String(this.comment, UTF_8));
			entry.setSize(Bytes.littleEndianValue(this.header, 24, 4));
			entry.setTime(Bytes.littleEndianValue(this.header, 12, 4));
			this.entry = entry;
		}
		return this.entry;
	}

	public static JarEntryData get(JarFile source, InputStream inputStream,
			RandomAccessData data) throws IOException {
		byte[] header = new byte[46];
		if (!Bytes.fill(inputStream, header)) {
			return null;
		}
		return new JarEntryData(source, header, inputStream, data);
	}

	/**
	 * @return
	 */
	public boolean isDirectory() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @return
	 */
	public int getMethod() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @return the entryData
	 */
	public RandomAccessData getData() {
		return this.entryData;
	}

	public InputStream getInputStream() {
		// InputStream inputStream = getData().getInputStream();
		// if (getMethod() == ZipEntry.DEFLATED) {
		// inputStream = new ZipInflaterInputStream(inputStream, (int) getSize());
		// }
		// return inputStream;
		throw new UnsupportedOperationException();
	}

}
