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
import java.util.zip.ZipEntry;

import org.springframework.boot.loader.AsciiBytes;
import org.springframework.boot.loader.data.RandomAccessData;

/**
 * Holds the underlying data of a {@link JarEntry}, allowing creation to be deferred until
 * the entry is actually needed.
 * 
 * @author Phillip Webb
 */
public final class JarEntryData {

	private static final long LOCAL_FILE_HEADER_SIZE = 30;

	private static final AsciiBytes SLASH = new AsciiBytes("/");

	private final JarFile source;

	private AsciiBytes name;

	private final int method;

	private long time;

	private long crc;

	private int compressedSize;

	private final int size;

	private final byte[] extra;

	private final AsciiBytes comment;

	private final RandomAccessData data;

	private JarEntry entry;

	public JarEntryData(JarFile source, byte[] header, InputStream inputStream)
			throws IOException {

		this.source = source;

		this.method = (int) Bytes.littleEndianValue(header, 10, 2);
		this.time = Bytes.littleEndianValue(header, 12, 4);
		this.crc = Bytes.littleEndianValue(header, 16, 4);
		this.compressedSize = (int) Bytes.littleEndianValue(header, 20, 4);
		this.size = (int) Bytes.littleEndianValue(header, 24, 4);
		long nameLength = Bytes.littleEndianValue(header, 28, 2);
		long extraLength = Bytes.littleEndianValue(header, 30, 2);
		long commentLength = Bytes.littleEndianValue(header, 32, 2);

		this.name = new AsciiBytes(Bytes.get(inputStream, nameLength));
		this.extra = Bytes.get(inputStream, extraLength);
		this.comment = new AsciiBytes(Bytes.get(inputStream, commentLength));

		long offset = Bytes.littleEndianValue(header, 42, 4);
		offset += LOCAL_FILE_HEADER_SIZE;
		offset += this.name.length();
		offset += this.extra.length;
		this.data = source.getData().getSubsection(offset, this.compressedSize);
	}

	JarFile getSource() {
		return this.source;
	}

	void setName(AsciiBytes name) {
		this.name = name;
	}

	public AsciiBytes getName() {
		return this.name;
	}

	public boolean isDirectory() {
		return this.name.endsWith(SLASH);
	}

	public int getMethod() {
		return this.method;
	}

	public long getTime() {
		return this.time;
	}

	public long getCrc() {
		return this.crc;
	}

	public int getCompressedSize() {
		return this.compressedSize;
	}

	public int getSize() {
		return this.size;
	}

	public byte[] getExtra() {
		return this.extra;
	}

	public AsciiBytes getComment() {
		return this.comment;
	}

	public InputStream getInputStream() {
		InputStream inputStream = getData().getInputStream();
		if (this.method == ZipEntry.DEFLATED) {
			inputStream = new ZipInflaterInputStream(inputStream, this.size);
		}
		return inputStream;
	}

	public RandomAccessData getData() {
		return this.data;
	}

	JarEntry getJarEntry() {
		if (this.entry == null) {
			JarEntry entry = new JarEntry(this);
			entry.setCompressedSize(this.compressedSize);
			entry.setMethod(this.method);
			entry.setCrc(this.crc);
			entry.setSize(this.size);
			entry.setExtra(this.extra);
			entry.setComment(this.comment.toString());
			entry.setSize(this.size);
			entry.setTime(this.time);
			this.entry = entry;
		}
		return this.entry;
	}

	public static JarEntryData fromInputStream(JarFile source, InputStream inputStream)
			throws IOException {
		byte[] header = new byte[46];
		if (!Bytes.fill(inputStream, header)) {
			return null;
		}
		return new JarEntryData(source, header, inputStream);
	}

}
