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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link DataBlock} that creates a virtual zip.
 *
 * @author Phillip Webb
 */
class VirtualZipDataBlock extends VirtualDataBlock {

	private final FileChannelDataBlock data;

	VirtualZipDataBlock(FileChannelDataBlock data, String namePrefix, CentralDirectoryFileHeaderRecord[] centralRecords,
			long[] centralRecordPositions) throws IOException {
		this.data = data;
		List<DataBlock> parts = new ArrayList<>();
		List<DataBlock> centralParts = new ArrayList<>();
		int prefixLen = (namePrefix != null) ? namePrefix.getBytes(StandardCharsets.UTF_8).length : 0;
		int offset = 0;
		int sizeOfCentralDirectory = 0;
		for (int i = 0; i < centralRecords.length; i++) {
			CentralDirectoryFileHeaderRecord centralRecord = centralRecords[i];
			long centralRecordPos = centralRecordPositions[i];
			DataBlock name = new DataPart(
					centralRecordPos + CentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET + prefixLen,
					centralRecord.fileNameLength() & 0xFFFF - prefixLen);
			LocalFileHeaderRecord localRecord = LocalFileHeaderRecord.load(this.data,
					centralRecord.offsetToLocalHeader());
			DataBlock content = new DataPart(centralRecord.offsetToLocalHeader() + localRecord.size(),
					centralRecord.compressedSize());
			sizeOfCentralDirectory += addToCentral(centralParts, centralRecord, centralRecordPos, name, offset);
			offset += addToLocal(parts, localRecord, name, content);
		}
		parts.addAll(centralParts);
		EndOfCentralDirectoryRecord eocd = new EndOfCentralDirectoryRecord((short) centralRecords.length,
				sizeOfCentralDirectory, offset);
		parts.add(new ByteArrayDataBlock(eocd.asByteArray()));
		setParts(parts);
	}

	private long addToCentral(List<DataBlock> parts, CentralDirectoryFileHeaderRecord originalRecord,
			long originalRecordPos, DataBlock name, int offsetToLocalHeader) throws IOException {
		CentralDirectoryFileHeaderRecord record = originalRecord.withFileNameLength((short) (name.size() & 0xFFFF))
			.withOffsetToLocalHeader(offsetToLocalHeader);
		int originalExtraFieldLength = originalRecord.extraFieldLength() & 0xFFFF;
		int originalFileCommentLength = originalRecord.fileCommentLength() & 0xFFFF;
		DataBlock extraFieldAndComment = new DataPart(
				originalRecordPos + originalRecord.size() - originalExtraFieldLength - originalFileCommentLength,
				originalExtraFieldLength + originalFileCommentLength);
		parts.add(new ByteArrayDataBlock(record.asByteArray()));
		parts.add(name);
		parts.add(extraFieldAndComment);
		return record.size();
	}

	private long addToLocal(List<DataBlock> parts, LocalFileHeaderRecord originalRecord, DataBlock name,
			DataBlock content) throws IOException {
		LocalFileHeaderRecord record = originalRecord.withExtraFieldLength((short) 0)
			.withFileNameLength((short) (name.size() & 0xFFFF));
		parts.add(new ByteArrayDataBlock(record.asByteArray()));
		parts.add(name);
		parts.add(content);
		return record.size() + content.size();
	}

	/**
	 * {@link DataBlock} that points to part of the original data block.
	 */
	final class DataPart implements DataBlock {

		private long offset;

		private long size;

		DataPart(long offset, long size) {
			this.offset = offset;
			this.size = size;
		}

		@Override
		public long size() throws IOException {
			return this.size;
		}

		@Override
		public int read(ByteBuffer dst, long pos) throws IOException {
			int remaining = (int) (this.size - pos);
			if (remaining <= 0) {
				return -1;
			}
			int originalDestinationLimit = -1;
			if (dst.remaining() > remaining) {
				originalDestinationLimit = dst.limit();
				dst.limit(dst.position() + remaining);
			}
			int result = VirtualZipDataBlock.this.data.read(dst, this.offset + pos);
			if (originalDestinationLimit != -1) {
				dst.limit(originalDestinationLimit);
			}
			return result;
		}

	}

}
