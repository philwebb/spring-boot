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

	private final JarEntryName name;

	private final byte[] extra;

	private final byte[] comment;

	private final byte[] header;

	private final RandomAccessData data;

	private volatile JarEntry entry;

	public JarEntryData(byte[] header, InputStream inputStream, RandomAccessData data)
			throws IOException {
		byte[] name = Bytes.get(inputStream, LittleEndian.valueOf(header, 28, 2));
		this.name = new JarEntryName(name);
		this.extra = Bytes.get(inputStream, LittleEndian.valueOf(header, 30, 2));
		this.comment = Bytes.get(inputStream, LittleEndian.valueOf(header, 32, 2));
		this.header = header;
		this.data = data;
	}

	public JarEntry getJarEntry() {
		if (this.entry == null) {
			long compressedSize = LittleEndian.valueOf(this.header, 20, 4);
			long localFileOffset = LittleEndian.valueOf(this.header, 42, 4);
			localFileOffset += LOCAL_FILE_HEADER_SIZE + this.name.length()
					+ this.extra.length;
			JarEntry entry = new JarEntry(this.name.toString(), this.data.getSubsection(
					localFileOffset, compressedSize));
			entry.setCompressedSize(compressedSize);
			entry.setMethod((int) LittleEndian.valueOf(this.header, 10, 2));
			entry.setCrc(LittleEndian.valueOf(this.header, 16, 4));
			entry.setSize(LittleEndian.valueOf(this.header, 24, 4));
			entry.setExtra(this.extra);
			entry.setComment(new String(this.comment, UTF_8));
			entry.setSize(LittleEndian.valueOf(this.header, 24, 4));
			entry.setTime(LittleEndian.valueOf(this.header, 12, 4));
			this.entry = entry;
		}
		return this.entry;
	}

	public static JarEntryData get(InputStream inputStream, RandomAccessData data)
			throws IOException {
		byte[] header = new byte[46];
		if (!Bytes.fill(inputStream, header)) {
			return null;
		}
		return new JarEntryData(header, inputStream, data);
	}

}
