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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * @author Phillip Webb
 */
class VirtualZipDataBlock extends VirtualDataBlock {

	private final FileChannelDataBlock data;

	VirtualZipDataBlock(FileChannelDataBlock data, String namePrefix, CentralDirectoryFileHeaderRecord[] centralRecords)
			throws IOException {
		this.data = data;
		List<DataBlock> parts = new ArrayList<>();
		List<DataBlock> endParts = new ArrayList<>();
		int prefixLen = (namePrefix != null) ? namePrefix.getBytes(StandardCharsets.UTF_8).length : 0;
		for (CentralDirectoryFileHeaderRecord central : centralRecords) {
			DataBlock name = new DataPart(central.fileNamePos() + prefixLen, central.fileNameLength() - prefixLen);
			LocalFileHeaderRecord local = LocalFileHeaderRecord.load(this.data, central.offsetToLocalHeader());
			LocalFileHeaderRecord virtualLocal = local.withFileNameLength(name.size()).withExtraFieldLength(0);
			CentralDirectoryFileHeaderRecord virtualCentral = central;
			DataBlock updatedLocal = new ByteArrayDataBlock(local.withUpdatedLengths(name.size(), 0).toByteArray());
			DataBlock contentPart = new DataPart(central.offsetToLocalHeader() + local.size(),
					central.compressedSize());

		}
		setParts(parts);
	}

	private List<DataBlock> createParts(CentralDirectoryFileHeaderRecord[] centralDirectoryRecords) throws IOException {
		long size = 0;
		for (CentralDirectoryFileHeaderRecord central : centralDirectoryRecords) {
			LocalFileHeaderRecord local = LocalFileHeaderRecord.load(this.data, central.offsetToLocalHeader());
			DataPart localPart = new DataPart(central.offsetToLocalHeader(), local.size() + central.uncompressedSize());
			size += localPart.size();
		}
		return null;
	}

	private List<DataBlock> createRenamedParts(String prefix,
			CentralDirectoryFileHeaderRecord[] centralDirectoryRecords) throws IOException {

		List<DataBlock> parts = new ArrayList<>();
		List<DataBlock> centralParts = new ArrayList<>();
		long localSize = 0;
		for (CentralDirectoryFileHeaderRecord central : centralDirectoryRecords) {

			localSize += addLocal(parts, localPart, name, content);
		}
		return null;
	}

	private long addLocal(List<DataBlock> parts, DataBlock header, DataPart name, DataPart content) throws IOException {
		parts.add(header);
		parts.add(name);
		parts.add(content);
		return header.size() + name.size() + content.size();
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
				dst.limit(remaining);
			}
			int result = VirtualZipDataBlock.this.data.read(dst, this.offset + pos);
			if (originalDestinationLimit != -1) {
				dst.limit(originalDestinationLimit);
			}
			return result;
		}

	}

}
