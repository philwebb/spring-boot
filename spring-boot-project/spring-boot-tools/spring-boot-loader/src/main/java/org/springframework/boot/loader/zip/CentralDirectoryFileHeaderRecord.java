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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.ValueRange;
import java.util.zip.ZipEntry;

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

	public CentralDirectoryFileHeaderRecord virtual() {
	}

	/**
	 * Copy values from this block to the given {@link ZipEntry}.
	 * @param dataBlock the source data block
	 * @param zipEntry the destination zip entry
	 * @throws IOException on I/O error
	 */
	void copyTo(DataBlock dataBlock, ZipEntry zipEntry) throws IOException {
		int fileNameLength = fileNameLength() & 0xFFFF;
		int extraLength = extraFieldLength() & 0xFFFF;
		int commentLength = fileCommentLength() & 0xFFFF;
		zipEntry.setMethod(compressionMethod() & 0xFFFF);
		zipEntry.setTime(decodeMsDosFormatDateTime(lastModFileDate(), lastModFileTime()));
		zipEntry.setCrc(crc32() & 0xFFFFFFFFL);
		zipEntry.setCompressedSize(compressedSize() & 0xFFFFFFFFL);
		zipEntry.setSize(uncompressedSize() & 0xFFFFFFFFL);
		if (extraLength > 0) {
			long pos = pos() + MINIMUM_SIZE + fileNameLength;
			ByteBuffer buffer = ByteBuffer.allocate(extraLength);
			dataBlock.readFully(buffer, pos);
			zipEntry.setExtra(buffer.array());
		}
		if ((fileCommentLength() & 0xFFFF) > 0) {
			long pos = pos() + MINIMUM_SIZE + fileNameLength + extraLength;
			zipEntry.setComment(ZipString.readString(dataBlock, pos, commentLength));
		}
	}

	/**
	 * Decode MS-DOS Date Time details. See <a href=
	 * "https://docs.microsoft.com/en-gb/windows/desktop/api/winbase/nf-winbase-dosdatetimetofiletime">
	 * Microsoft's documentation</a> for more details of the format.
	 * @param datetime the date and time
	 * @return the date and time as milliseconds since the epoch
	 */
	private long decodeMsDosFormatDateTime(short date, short time) {
		int year = getChronoValue(((date >> 9) & 0x7f) + 1980, ChronoField.YEAR);
		int month = getChronoValue((date >> 5) & 0x0f, ChronoField.MONTH_OF_YEAR);
		int day = getChronoValue(date & 0x1f, ChronoField.DAY_OF_MONTH);
		int hour = getChronoValue((time >> 11) & 0x1f, ChronoField.HOUR_OF_DAY);
		int minute = getChronoValue((time >> 5) & 0x3f, ChronoField.MINUTE_OF_HOUR);
		int second = getChronoValue((time << 1) & 0x3e, ChronoField.SECOND_OF_MINUTE);
		return ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.systemDefault())
			.toInstant()
			.truncatedTo(ChronoUnit.SECONDS)
			.toEpochMilli();
	}

	private static int getChronoValue(long value, ChronoField field) {
		ValueRange range = field.range();
		return Math.toIntExact(Math.min(Math.max(value, range.getMinimum()), range.getMaximum()));
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
