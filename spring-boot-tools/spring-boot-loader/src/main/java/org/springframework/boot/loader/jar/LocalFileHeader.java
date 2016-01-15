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

import org.springframework.boot.loader.data.RandomAccessData;
import org.springframework.boot.loader.data.RandomAccessData.ResourceAccess;

/**
 * @author Phillip Webb
 */
class LocalFileHeader {

	private final byte[] header;

	private final AsciiBytes name;

	private final byte[] extra;

	private final long contentOffset;

	public LocalFileHeader(byte[] header, RandomAccessData data, int offset)
			throws IOException {
		this.header = header;
		long nameLength = Bytes.littleEndianValue(header, 26, 2);
		long extraLength = Bytes.littleEndianValue(header, 28, 2);
		InputStream inputStream = data
				.getSubsection(offset + header.length, nameLength + extraLength)
				.getInputStream(ResourceAccess.ONCE);
		try {
			this.name = new AsciiBytes(Bytes.get(inputStream, nameLength));
			this.extra = Bytes.get(inputStream, extraLength);
		}
		finally {
			inputStream.close();
		}
		this.contentOffset = offset + nameLength + extraLength + header.length;
	}

	public AsciiBytes getName() {
		return this.name;
	}

	public int getMethod() {
		return (int) Bytes.littleEndianValue(this.header, 8, 2);
	}

	public int getCompressedSize() {
		return (int) Bytes.littleEndianValue(this.header, 18, 4);
	}

	public int getSize() {
		return (int) Bytes.littleEndianValue(this.header, 22, 4);
	}

	public byte[] getExtra() {
		return this.extra;
	}

	public long getContentOffset() {
		return this.contentOffset;
	}

	public static LocalFileHeader fromRandomAccessData(RandomAccessData data, int offset)
			throws IOException {
		byte[] header = Bytes.get(data.getSubsection(offset, 30));
		return new LocalFileHeader(header, data, offset);
	}

}
