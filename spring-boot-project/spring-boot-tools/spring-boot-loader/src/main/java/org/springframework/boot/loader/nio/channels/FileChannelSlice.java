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

package org.springframework.boot.loader.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * {@link SeekableByteChannel} that provides read-only access to a subsequence of a
 * {@link FileChannel}.
 *
 * @author Phillip Webb
 */
public class FileChannelSlice implements SeekableByteChannel {

	private final FileChannel fileChannel;

	private final int offset;

	private final int size;

	private final ReadWriteLock access = new ReentrantReadWriteLock();

	private int position;

	private FileChannelSlice(FileChannel fileChannel, int offset, int length) {
		this.fileChannel = fileChannel;
		this.offset = offset;
		this.size = length;
	}

	@Override
	public boolean isOpen() {
		return this.fileChannel.isOpen();
	}

	@Override
	public void close() throws IOException {
		this.fileChannel.close();
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		this.access.writeLock().lock();
		try {
			ensureOpen();
			int remaning = this.size - this.position;
			ByteBuffer slice = dst.slice(0, Math.min(remaning, dst.limit()));
			int amountRead = this.fileChannel.read(slice, this.offset + this.position);
			this.position += (amountRead > 0) ? amountRead : 0;
			return amountRead;
		}
		finally {
			this.access.writeLock().unlock();
		}
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		throw new NonWritableChannelException();
	}

	@Override
	public long position() throws IOException {
		this.access.readLock().lock();
		try {
			ensureOpen();
			return this.position;
		}
		finally {
			this.access.readLock().unlock();
		}
	}

	@Override
	public SeekableByteChannel position(long position) throws IOException {
		this.access.writeLock().lock();
		try {
			ensureOpen();
			if (position < 0 || position >= Integer.MAX_VALUE) {
				throw new IllegalArgumentException("Illegal position " + this.position);
			}
			this.position = Math.min((int) position, this.size);
			return this;
		}
		finally {
			this.access.writeLock().unlock();
		}
	}

	@Override
	public long size() throws IOException {
		return this.size;
	}

	@Override
	public SeekableByteChannel truncate(long size) throws IOException {
		throw new NonWritableChannelException();
	}

	private void ensureOpen() throws IOException {
		if (!this.fileChannel.isOpen()) {
			throw new ClosedChannelException();
		}
	}

	public static FileChannelSlice open(Path path, int index, int length) throws IOException {
		FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ);
		// FIXME assert
		return new FileChannelSlice(fileChannel, index, length);
	}

}
