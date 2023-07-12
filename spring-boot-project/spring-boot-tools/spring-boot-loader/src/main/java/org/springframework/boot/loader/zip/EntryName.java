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

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author pwebb
 */
class EntryName {

	private static final int BUFFER_SIZE = 128;

	private static final int EMPTY_HASHCODE = "/".hashCode();

	private static final int[] INITIAL_BYTE_BITMASK = { 0x7F, 0x1F, 0x0F, 0x07 };

	private static final int SUBSEQUENT_BYTE_BITMASK = 0x3F;

	static int calculate(CharSequence entryName) {
		if (entryName == null || entryName.length() == 0) {
			return EMPTY_HASHCODE;
		}
		if (entryName instanceof String) {
			return calculateForString((String) entryName);
		}
		boolean endsWithSlash = entryName.charAt(entryName.length() - 1) == '/';
		int hash = 0;
		for (int i = 0; i < entryName.length(); i++) {
			char ch = entryName.charAt(i);
			hash = 31 * hash + ch;
		}
		hash = (!endsWithSlash) ? 31 * hash + '/' : hash;
		return hash;
	}

	private static int calculateForString(String entryName) {
		boolean endsWithSlash = entryName.charAt(entryName.length() - 1) == '/';
		int hash = entryName.hashCode();
		return (!endsWithSlash) ? 31 * hash + '/' : hash;
	}

	static int calculate(DataBlock dataBlock, long pos, int size) throws IOException {
		if (size == 0) {
			return EMPTY_HASHCODE;
		}
		ByteBuffer buffer = ByteBuffer.allocate(size < BUFFER_SIZE ? size : BUFFER_SIZE);
		byte[] bytes = buffer.array();
		int hash = 0;
		char ch = 0;
		while (size > 0) {
			buffer.clear();
			int count = dataBlock.read(buffer, pos);
			if (count < 0) {
				throw new EOFException();
			}
			System.out.println(new String(bytes, 0, count, StandardCharsets.UTF_8));
			size -= count;
			pos += count;
			for (int i = 0; i < count; i++) {
				int b = bytes[i];
				int remainingUtfBytes = getNumberOfUtfBytes(b) - 1;
				b &= INITIAL_BYTE_BITMASK[remainingUtfBytes];
				for (int j = 0; j < remainingUtfBytes; j++) {
					b = (b << 6) + (bytes[++i] & SUBSEQUENT_BYTE_BITMASK);
				}
				if (b <= 0xFFFF) {
					ch = (char) (b & 0xFFFF);
					hash = 31 * hash + ch;
				}
				else {
					hash = 31 * hash + ((b >> 0xA) + 0xD7C0);
					hash = 31 * hash + ((b & 0x3FF) + 0xDC00);
				}
			}
		}
		hash = (ch != '/') ? 31 * hash + '/' : hash;
		return hash;
	}

	private static int getNumberOfUtfBytes(int b) {
		if ((b & 0x80) == 0) {
			return 1;
		}
		int numberOfUtfBytes = 0;
		while ((b & 0x80) != 0) {
			b <<= 1;
			numberOfUtfBytes++;
		}
		return numberOfUtfBytes;
	}

}
