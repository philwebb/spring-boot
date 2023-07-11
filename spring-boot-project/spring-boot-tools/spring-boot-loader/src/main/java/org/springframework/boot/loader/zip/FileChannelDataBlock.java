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

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Reference counted {@link DataBlock} implementation backed by a {@link FileChannel} with
 * support for slicing.
 *
 * @author Phillip Webb
 */
class FileChannelDataBlock implements DataBlock, Closeable {

	private final Object lock = new Object();

	private volatile int referenceCount;

	private volatile FileChannel fileChannel;

	private final Opener opener;

	private final Closer closer;

	private final long offset;

	private final long size;

	FileChannelDataBlock(Opener opener, Closer closer) throws IOException {
		this(opener, closer, 0, -1);
	}

	private FileChannelDataBlock(Opener opener, Closer closer, long offset, long size) throws IOException {
		this.referenceCount = 1;
		this.fileChannel = opener.open();
		this.opener = opener;
		this.closer = closer;
		this.offset = offset;
		this.size = (size != -1) ? size : this.fileChannel.size();
	}

	@Override
	public long size() throws IOException {
		return this.size;
	}

	@Override
	public int read(ByteBuffer dst, long position) throws IOException {
		if (position < 0) {
			throw new IllegalArgumentException("Position must not be negative");
		}
		ensureOpen();
		int remaining = (int) (this.size - position);
		if (remaining <= 0) {
			return -1;
		}
		int originalDestinationLimit = -1;
		if (dst.remaining() > remaining) {
			originalDestinationLimit = dst.limit();
			dst.limit(remaining);
		}
		int result = this.fileChannel.read(dst, this.offset + position);
		if (originalDestinationLimit != -1) {
			dst.limit(originalDestinationLimit);
		}
		return result;
	}

	FileChannelDataBlock openSlice(long offset, long size) throws IOException {
		if (offset < 0) {
			throw new IllegalArgumentException("Offset must not be negative");
		}
		if (size < 0 || offset + size > this.size) {
			throw new IllegalArgumentException("Size must not be negative and must be within bounds");
		}
		return new FileChannelDataBlock(this::openDuplicate, this::closeDuplicate, this.offset + offset, size);
	}

	private FileChannel openDuplicate() throws IOException {
		open();
		return this.fileChannel;
	}

	private void closeDuplicate(FileChannel fileChannel) throws IOException {
		close();
	}

	private void ensureOpen() throws ClosedChannelException {
		synchronized (this.lock) {
			if (this.referenceCount == 0) {
				throw new ClosedChannelException();
			}
		}
	}

	void open() throws IOException {
		synchronized (this.lock) {
			if (this.referenceCount == 0) {
				this.fileChannel = this.opener.open();
			}
			this.referenceCount++;
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (this.lock) {
			if (this.referenceCount == 0) {
				return;
			}
			this.referenceCount--;
			if (this.referenceCount == 0) {
				this.closer.close(this.fileChannel);
				this.fileChannel = null;
			}
		}
	}

	static FileChannelDataBlock open(Path path) throws IOException {
		if (!Files.isRegularFile(path)) {
			throw new IllegalArgumentException(path + " must be a regular file");
		}
		return new FileChannelDataBlock(() -> FileChannel.open(path, StandardOpenOption.READ), FileChannel::close);
	}

	interface Opener {

		FileChannel open() throws IOException;

	}

	interface Closer {

		void close(FileChannel channel) throws IOException;

	}

}
