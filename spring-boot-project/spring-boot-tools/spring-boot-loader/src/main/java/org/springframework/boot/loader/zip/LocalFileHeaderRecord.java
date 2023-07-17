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

import org.springframework.boot.loader.log.DebugLogger;

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
 * 4.3.7 of the Zip File Format Specification</a>
 */
record LocalFileHeaderRecord(short versionNeededToExtract, short generalPurposeBitFlag, short compressionMethod,
		short lastModFileTime, short lastModFileDate, int crc32, int compressedSize, int uncompressedSize,
		short fileNameLength, short extraFieldLength) {

	private static final DebugLogger debug = DebugLogger.get(LocalFileHeaderRecord.class);

	private static final int SIGNATURE = 0x04034b50;

	private static final int MINIMUM_SIZE = 30;

	/**
	 * Return the size of this record.
	 * @return the record size
	 */
	long size() {
		return MINIMUM_SIZE + fileNameLength() + extraFieldLength();
	}

	LocalFileHeaderRecord withExtraFieldLength(short extraFieldLength) {
		return new LocalFileHeaderRecord(this.versionNeededToExtract, this.generalPurposeBitFlag,
				this.compressionMethod, this.lastModFileTime, this.lastModFileDate, this.crc32, this.compressedSize,
				this.uncompressedSize, this.fileNameLength, extraFieldLength);
	}

	LocalFileHeaderRecord withFileNameLength(short fileNameLength) {
		return new LocalFileHeaderRecord(this.versionNeededToExtract, this.generalPurposeBitFlag,
				this.compressionMethod, this.lastModFileTime, this.lastModFileDate, this.crc32, this.compressedSize,
				this.uncompressedSize, fileNameLength, this.extraFieldLength);
	}

	byte[] asByteArray() {
		ByteBuffer buffer = ByteBuffer.allocate(MINIMUM_SIZE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(SIGNATURE);
		buffer.putShort(this.versionNeededToExtract);
		buffer.putShort(this.generalPurposeBitFlag);
		buffer.putShort(this.compressionMethod);
		buffer.putShort(this.lastModFileTime);
		buffer.putShort(this.lastModFileDate);
		buffer.putInt(this.crc32);
		buffer.putInt(this.compressedSize);
		buffer.putInt(this.uncompressedSize);
		buffer.putShort(this.fileNameLength);
		buffer.putShort(this.extraFieldLength);
		return buffer.array();
	}

	static LocalFileHeaderRecord load(DataBlock dataBlock, long pos) throws IOException {
		debug.log("Loading LocalFileHeaderRecord from position %s", pos);
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
