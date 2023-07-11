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
 * A Zip64 end of central directory locator.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @param pos the position where this record begins in the source {@link DataBlock}
 * @param numberOfThisDisk the number of the disk with the start of the zip64 end of
 * central directory
 * @param offsetToZip64EndOfCentralDirectoryRecord the relative offset of the zip64 end of
 * central directory record
 * @param totalNumberOfDisks the total number of disks
 * @see <a href="https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">Chapter
 * 4.3.15 of the Zip File Format Specification</a>
 */
record Zip64EndOfCentralDirectoryLocator(long pos, int numberOfThisDisk, long offsetToZip64EndOfCentralDirectoryRecord,
		int totalNumberOfDisks) {

	private static final int SIGNATURE = 0x07064b50;

	static final int SIZE = 20;

	/**
	 * Return the {@link Zip64EndOfCentralDirectoryLocator} or {@code null} if this is not
	 * a Zip64 file.
	 * @param dataBlock the source data block
	 * @param endOfCentralDirectoryRecord the {@link EndOfCentralDirectoryRecord}
	 * @return a {@link Zip64EndOfCentralDirectoryLocator} instance or null
	 * @throws IOException on I/O error
	 */
	static Zip64EndOfCentralDirectoryLocator find(DataBlock dataBlock,
			EndOfCentralDirectoryRecord endOfCentralDirectoryRecord) throws IOException {
		long pos = endOfCentralDirectoryRecord.pos() - SIZE;
		if (pos < 0) {
			return null;
		}
		ByteBuffer buffer = ByteBuffer.allocate(SIZE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		dataBlock.read(buffer, pos);
		buffer.rewind();
		if (buffer.getInt() != SIGNATURE) {
			return null;
		}
		return new Zip64EndOfCentralDirectoryLocator(pos, buffer.getInt(), buffer.getLong(), buffer.getInt());
	}

}
