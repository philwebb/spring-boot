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

package org.springframework.boot.loader.nio.file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;

import org.springframework.boot.loader.zip.CloseableDataBlock;
import org.springframework.boot.loader.zip.ZipContent;

class NestedSeekableByteChannel implements SeekableByteChannel {

	private CloseableDataBlock zipData;

	private long position;

	NestedSeekableByteChannel(Path path, String nestedEntryName) throws IOException {
		ZipContent zipContent = ZipContent.open(path, nestedEntryName);
		CloseableDataBlock zipData = zipContent.openRawZipData();
		this.zipData = zipData;
	}

	@Override
	public boolean isOpen() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public void close() throws IOException {
		this.zipData.close();
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		int count = this.zipData.read(dst, this.position);
		if (count > 0) {
			this.position += count;
		}
		return count;
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		throw new NonWritableChannelException();
	}

	@Override
	public long position() throws IOException {
		return this.position;
	}

	@Override
	public SeekableByteChannel position(long position) throws IOException {
		this.position = position;
		return this;
	}

	@Override
	public long size() throws IOException {
		return this.zipData.size();
	}

	@Override
	public SeekableByteChannel truncate(long size) throws IOException {
		throw new NonWritableChannelException();
	}

}
