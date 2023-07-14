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
 * A ZIP File "Central directory file header record" (CDFH).
 *
 * @author Phillip Webb
 * @param pos the position where this record begins in the source {@link DataBlock}
 * @param versionMadeBy the version that made the zip
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
 * @param fileCommentLength the comment length
 * @param diskNumberStart the disk number where the entry starts
 * @param internalFileAttributes the internal file attributes
 * @param externalFileAttributes the external file attributes
 * @param offsetToLocalHeader the relative offset to the local file header
 * @see <a href="https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">Chapter
 * 4.3.12 of the Zip File Format Specification</a>
 */
record CentralDirectoryFileHeaderRecord(long pos, short versionMadeBy, short versionNeededToExtract,
		short generalPurposeBitFlag, short compressionMethod, short lastModFileTime, short lastModFileDate, int crc32,
		int compressedSize, int uncompressedSize, short fileNameLength, short extraFieldLength, short fileCommentLength,
		short diskNumberStart, short internalFileAttributes, int externalFileAttributes, int offsetToLocalHeader) {

	private static final DebugLogger debug = DebugLogger.get(CentralDirectoryFileHeaderRecord.class);

	private static final int SIGNATURE = 0x02014b50;

	private static final int MINIMUM_SIZE = 46;

	/**
	 * Return the size of this record.
	 * @return the record size
	 */
	long size() {
		return MINIMUM_SIZE + fileNameLength() + extraFieldLength() + fileCommentLength();
	}

	/**
	 * Return the start position of the file name.
	 * @return the file name start position
	 */
	long fileNamePos() {
		return this.pos + MINIMUM_SIZE;
	}

	static CentralDirectoryFileHeaderRecord load(DataBlock dataBlock, long pos) throws IOException {
		debug.log("Loading CentralDirectoryFileHeaderRecord from position %s", pos);
		ByteBuffer buffer = ByteBuffer.allocate(MINIMUM_SIZE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		dataBlock.readFully(buffer, pos);
		buffer.rewind();
		int signature = buffer.getInt();
		if (signature != SIGNATURE) {
			debug.log("Found incorrect CentralDirectoryFileHeaderRecord signature %s at position %s", signature, pos);
			throw new IOException("Zip 'Central Directory File Header Record' not found at position " + pos);
		}
		return new CentralDirectoryFileHeaderRecord(pos, buffer.getShort(), buffer.getShort(), buffer.getShort(),
				buffer.getShort(), buffer.getShort(), buffer.getShort(), buffer.getInt(), buffer.getInt(),
				buffer.getInt(), buffer.getShort(), buffer.getShort(), buffer.getShort(), buffer.getShort(),
				buffer.getShort(), buffer.getInt(), buffer.getInt());
	}

}
