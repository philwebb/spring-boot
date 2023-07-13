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
 * A Zip64 end of central directory record.
 *
 * @author Phillip Webb
 * @param sizeOfZip64EndOfCentralDirectoryRecord the size of zip64 end of central
 * directory record
 * @param versionMadeBy the version that made the zip
 * @param versionNeededToExtract the version needed to extract the zip
 * @param numberOfThisDisk the number of this disk (or 0xffff for ZIP64)
 * @param diskWhereCentralDirectoryStarts the disk where central directory starts (or
 * 0xffff for ZIP64)
 * @param numberOfCentralDirectoryEntriesOnThisDisk the number of central directory
 * entries on this disk (or 0xffff for ZIP64)
 * @param totalNumberOfCentralDirectoryEntries the total number of central directory
 * entries (or 0xffff for ZIP64)
 * @param sizeOfCentralDirectory the size of central directory (bytes) (or 0xffffffff for
 * ZIP64)
 * @param offsetToStartOfCentralDirectory the offset of start of central directory,
 * relative to start of archive (or 0xffffffff for ZIP64)
 * @see <a href="https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">Chapter
 * 4.3.14 of the Zip File Format Specification</a>
 */
record Zip64EndOfCentralDirectoryRecord(long sizeOfZip64EndOfCentralDirectoryRecord, short versionMadeBy,
		short versionNeededToExtract, int numberOfThisDisk, int diskWhereCentralDirectoryStarts,
		long numberOfCentralDirectoryEntriesOnThisDisk, long totalNumberOfCentralDirectoryEntries,
		long sizeOfCentralDirectory, long offsetToStartOfCentralDirectory) {

	private static final DebugLogger debug = DebugLogger.get(Zip64EndOfCentralDirectoryRecord.class);

	private static final int SIGNATURE = 0x06054b50;

	private static final int MINIMUM_SIZE = 56;

	static Zip64EndOfCentralDirectoryRecord load(DataBlock dataBlock, Zip64EndOfCentralDirectoryLocator locator)
			throws IOException {
		if (locator == null) {
			return null;
		}
		ByteBuffer buffer = ByteBuffer.allocate(MINIMUM_SIZE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		long pos = locator.offsetToZip64EndOfCentralDirectoryRecord();
		debug.log("Loading Zip64EndOfCentralDirectoryRecord from position %s", pos);
		dataBlock.readFully(buffer, pos);
		buffer.rewind();
		int signature = buffer.getInt();
		if (signature != SIGNATURE) {
			debug.log("Found incorrect Zip64EndOfCentralDirectoryRecord signature %s at position %s", signature, pos);
			throw new IOException("Zip64 'End Of Central Directory Record' not found at position " + pos
					+ ". Zip file is corrupt or includes prefixed bytes which are not supported with Zip64 files");
		}
		return new Zip64EndOfCentralDirectoryRecord(buffer.getLong(), buffer.getShort(), buffer.getShort(), signature,
				signature, buffer.getLong(), buffer.getLong(), buffer.getLong(), buffer.getLong());
	}

}
