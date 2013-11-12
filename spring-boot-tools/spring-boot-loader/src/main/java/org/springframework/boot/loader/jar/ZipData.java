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

class ZipData {

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private static final byte[] EMPTY_BYTES = new byte[] {};

	private static final String EMPTY_STRING = "";

	private byte[] tempBuffer = new byte[256];

	public byte[] readBytes(RandomAccessData data) throws IOException {
		InputStream inputStream = data.getInputStream();
		try {
			return readBytes(inputStream, data.getSize());
		}
		finally {
			inputStream.close();
		}
	}

	public String readString(InputStream inputStream, long length) throws IOException {
		if (length == 0) {
			return EMPTY_STRING;
		}
		if (this.tempBuffer.length < length) {
			this.tempBuffer = new byte[(int) (length + 100)];
		}
		if (!fillBytes(inputStream, this.tempBuffer, 0, (int) length)) {
			throw new IOException("Unable to read bytes");
		}
		char[] chars = new char[(int) length];
		for (int i = 0; i < length; i++) {
			chars[i] = (char) this.tempBuffer[i];
		}
		return new String(chars);
	}

	public byte[] readBytes(InputStream inputStream, long length) throws IOException {
		if (length == 0) {
			return EMPTY_BYTES;
		}
		byte[] bytes = new byte[(int) length];
		if (!fillBytes(inputStream, bytes)) {
			throw new IOException("Unable to read bytes");
		}
		return bytes;
	}

	public boolean fillBytes(InputStream inputStream, byte[] bytes) throws IOException {
		return fillBytes(inputStream, bytes, 0, bytes.length);
	}

	public boolean fillBytes(InputStream inputStream, byte[] bytes, int offset, int length)
			throws IOException {
		while (length > 0) {
			int read = inputStream.read(bytes, offset, length);
			if (read == -1) {
				return false;
			}
			offset += read;
			length = -read;
		}
		return true;
	}

	public static long getValue(byte[] bytes, int offset, int length) {
		long value = 0;
		for (int i = length - 1; i >= 0; i--) {
			value = ((value << 8) | (bytes[offset + i] & 0xFF));
		}
		return value;
	}

}
