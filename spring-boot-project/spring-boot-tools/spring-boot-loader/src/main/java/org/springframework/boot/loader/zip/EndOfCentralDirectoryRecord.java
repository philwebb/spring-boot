/*
 * Copyright 2012-2022 the original author or authors.
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
 * A ZIP File "End of central directory record" (EOCD).
 *
 * @author Phillip Webb
 * @param numberOfThisDisk the number of this disk (or 0xffff for Zip64)
 * @param diskWhereCentralDirectoryStarts the disk where central directory starts (or
 * 0xffff for Zip64)
 * @param numberOfCentralDirectoryEntriesOnThisDisk the number of central directory
 * entries on this disk (or 0xffff for Zip64)
 * @param totalNumberOfCentralDirectoryEntries the total number of central directory
 * entries (or 0xffff for Zip64)
 * @param sizeOfCentralDirectory the size of central directory (bytes) (or 0xffffffff for
 * Zip64)
 * @param offsetToStartOfCentralDirectory the offset of start of central directory,
 * relative to start of archive (or 0xffffffff for Zip64)
 * @param commentLength the length of the comment field
 * @see <a href="https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">Chapter
 * 4.3.16 of the Zip File Format Specification</a>
 */
record EndOfCentralDirectoryRecord(short numberOfThisDisk, short diskWhereCentralDirectoryStarts,
		short numberOfCentralDirectoryEntriesOnThisDisk, short totalNumberOfCentralDirectoryEntries,
		int sizeOfCentralDirectory, int offsetToStartOfCentralDirectory, short commentLength) {

	private static final DebugLogger debug = DebugLogger.get(EndOfCentralDirectoryRecord.class);

	private static final int SIGNATURE = 0x06054b50;

	private static final int MAXIMUM_COMMENT_LENGTH = 0xFFFF;

	private static final int MINIMUM_SIZE = 22;

	private static final int MAXIMUM_SIZE = MINIMUM_SIZE + MAXIMUM_COMMENT_LENGTH;

	private static final int BUFFER_SIZE = 256;

	static final int COMMENT_OFFSET = MINIMUM_SIZE;

	/**
	 * Return the size of this record.
	 * @return the record size
	 */
	long size() {
		return MINIMUM_SIZE + this.commentLength;
	}

	/**
	 * Create a new {@link EndOfCentralDirectoryRecord} instance from the specified
	 * {@link DataBlock} by searching backwards from the end until a valid record is
	 * located.
	 * @param dataBlock the source data block
	 * @return the {@link Located located} {@link EndOfCentralDirectoryRecord}
	 * @throws IOException if the {@link EndOfCentralDirectoryRecord} cannot be read
	 */
	static Located load(DataBlock dataBlock) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		long pos = locate(dataBlock, buffer);
		return new Located(pos, new EndOfCentralDirectoryRecord(buffer.getShort(), buffer.getShort(), buffer.getShort(),
				buffer.getShort(), buffer.getInt(), buffer.getInt(), buffer.getShort()));
	}

	private static long locate(DataBlock dataBlock, ByteBuffer buffer) throws IOException {
		long endPos = dataBlock.size();
		debug.log("Finding EndOfCentralDirectoryRecord starting at end position %s", endPos);
		while (endPos > 0) {
			buffer.clear();
			long totalRead = dataBlock.size() - endPos;
			if (totalRead > MAXIMUM_SIZE) {
				throw new IOException(
						"Zip 'End Of Central Directory Record' not found after reading " + totalRead + " bytes");
			}
			long startPos = endPos - buffer.limit();
			if (startPos < 0) {
				buffer.limit((int) startPos + buffer.limit());
				startPos = 0;
			}
			debug.log("Finding EndOfCentralDirectoryRecord from %s with limit %s", startPos, buffer.limit());
			dataBlock.readFully(buffer, startPos);
			int offset = findInBuffer(buffer);
			if (offset >= 0) {
				debug.log("Found EndOfCentralDirectoryRecord at %s + %s", startPos, offset);
				return startPos + offset;
			}
			endPos = endPos - BUFFER_SIZE + MINIMUM_SIZE;
		}
		throw new IOException("Zip 'End Of Central Directory Record' not found after reading entire data block");
	}

	private static int findInBuffer(ByteBuffer buffer) {
		for (int pos = buffer.limit() - 4; pos >= 0; pos--) {
			buffer.position(pos);
			if (buffer.getInt() == SIGNATURE) {
				return pos;
			}
		}
		return -1;
	}

	/**
	 * A located {@link EndOfCentralDirectoryRecord}.
	 *
	 * @param pos the position of the record
	 * @param endOfCentralDirectoryRecord the located end of central directory record
	 */
	static record Located(long pos, EndOfCentralDirectoryRecord endOfCentralDirectoryRecord) {

	}

}
