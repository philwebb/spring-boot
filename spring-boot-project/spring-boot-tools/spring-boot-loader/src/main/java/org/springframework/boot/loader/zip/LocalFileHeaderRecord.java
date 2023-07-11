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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A ZIP File "Local file header record" (LFH).
 *
 * @param versionNeededToExtract the version needed to extract the zip
 * @param generalPurposeBitFlag the general purpose bit flag
 * @param compressionMethod the compression method used for this entry
 * @param lastModFileTime the last modified file time
 * @param lastModFileDate the last modified file date
 * @param crc32 the CRC32 checksum
 * @param compressedSize the size of the entry when compressed
 * @param uncompressedSize the size of the entry when uncompressed
 * @param fileNameLength the file name length
 * @param extraFieldLength the extra field length
 * @see <a href="https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">Chapter
 * 4.7.3 of the Zip File Format Specification</a>
 */
record LocalFileHeaderRecord(short versionNeededToExtract, short generalPurposeBitFlag, short compressionMethod,
		short lastModFileTime, short lastModFileDate, int crc32, int compressedSize, int uncompressedSize,
		short fileNameLength, short extraFieldLength) {

	private static final int SIGNATURE = 0x04034b50;

	private static final int MINIMUM_SIZE = 30;

	LocalFileHeaderRecord load(DataBlock dataBlock, long pos) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(MINIMUM_SIZE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		dataBlock.readFully(buffer, pos);
		buffer.rewind();
		if (buffer.getInt() != SIGNATURE) {
			throw new IOException("Zip 'Local File Header Record' not found at position " + pos);
		}
		return new LocalFileHeaderRecord(buffer.getShort(), buffer.getShort(), buffer.getShort(), buffer.getShort(),
				buffer.getShort(), buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getShort(),
				buffer.getShort());
	}
}
