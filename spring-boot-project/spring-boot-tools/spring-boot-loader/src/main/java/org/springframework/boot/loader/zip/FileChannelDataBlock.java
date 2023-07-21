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
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;

import org.springframework.boot.loader.log.DebugLogger;

/**
 * Reference counted {@link DataBlock} implementation backed by a {@link FileChannel} with
 * support for slicing.
 *
 * @author Phillip Webb
 */
class FileChannelDataBlock implements CloseableDataBlock {

	private static final DebugLogger debug = DebugLogger.get(FileChannelDataBlock.class);

	private volatile FileChannel fileChannel;

	private volatile int referenceCount;

	private final Path path;

	private final Opener opener;

	private final Closer closer;

	private final long offset;

	private final long size;

	private final Object lock = new Object();

	/**
	 * Create a new {@link FileChannelDataBlock} instance.
	 * @param path the file path
	 * @param opener the {@link Opener} to use when opening the file
	 * @param closer the {@link Closer} to use when closing the file
	 * @throws IOException on I/O error
	 */
	FileChannelDataBlock(Path path, Opener opener, Closer closer) throws IOException {
		this(path, opener, closer, 0, -1);
	}

	private FileChannelDataBlock(Path path, Opener opener, Closer closer, long offset, long size) throws IOException {
		this.path = path;
		this.fileChannel = opener.open();
		this.referenceCount = 1;
		this.opener = opener;
		this.closer = closer;
		this.offset = offset;
		this.size = (size != -1) ? size : this.fileChannel.size();
		debug.log("Created new FileChannelDataBlock '%s' at %s with size %s", path, offset, size);
	}

	@Override
	public long size() throws IOException {
		return this.size;
	}

	@Override
	public int read(ByteBuffer dst, long pos) throws IOException {
		if (pos < 0) {
			throw new IllegalArgumentException("Position must not be negative");
		}
		ensureOpen();
		int remaining = (int) (this.size - pos);
		if (remaining <= 0) {
			return -1;
		}
		int originalDestinationLimit = -1;
		if (dst.remaining() > remaining) {
			originalDestinationLimit = dst.limit();
			dst.limit(dst.position() + remaining);
		}
		int result = this.fileChannel.read(dst, this.offset + pos);
		if (originalDestinationLimit != -1) {
			dst.limit(originalDestinationLimit);
		}
		return result;
	}

	/**
	 * Return a version of this instance with front matter removed. Note the result of
	 * this method is expected to replace this instance and as such calls do not increment
	 * the reference count.
	 * @param pos the new start position
	 * @return a {@link FileChannelDataBlock} with front matter removed
	 * @throws IOException on I/O error
	 */
	FileChannelDataBlock removeFrontMatter(long pos) throws IOException {
		if (pos <= 0) {
			return this;
		}
		return new FileChannelDataBlock(this.path, this.opener, this.closer, this.offset + pos, this.size - pos);
	}

	/**
	 * Open a new {@link FileChannelDataBlock} slice providing access to a subset of the
	 * data. The caller is responsible for closing resulting {@link FileChannelDataBlock}.
	 * @param offset the start offset for the slice relative to this block
	 * @param size the size of the new slice
	 * @return a new {@link FileChannelDataBlock} instance
	 * @throws IOException on I/O error
	 */
	FileChannelDataBlock openSlice(long offset, long size) throws IOException {
		if (offset < 0) {
			throw new IllegalArgumentException("Offset must not be negative");
		}
		if (size < 0 || offset + size > this.size) {
			throw new IllegalArgumentException("Size must not be negative and must be within bounds");
		}
		debug.log("Openning slice from %s at %s with size %s", this.path, offset, size);
		return new FileChannelDataBlock(this.path, this::openDuplicate, this::closeDuplicate, this.offset + offset,
				size);
	}

	private FileChannel openDuplicate() throws IOException {
		open();
		return this.fileChannel;
	}

	private void closeDuplicate(FileChannel fileChannel) throws IOException {
		close();
	}

	/**
	 * Ensure that the underlying file channel is currently open.
	 * @throws ClosedChannelException if the channel is closed
	 */
	void ensureOpen() throws ClosedChannelException {
		ensureOpen(ClosedChannelException::new);
	}

	/**
	 * Ensure that the underlying file channel is currently open.
	 * @param exceptionSupplier a supplier providing the exception to throw
	 * @param <E> the exception type
	 * @throws E if the channel is closed
	 */
	<E extends Exception> void ensureOpen(Supplier<E> exceptionSupplier) throws E {
		synchronized (this.lock) {
			if (this.referenceCount == 0) {
				throw exceptionSupplier.get();
			}
		}
	}

	/**
	 * Open a connection to this block, increasing the reference count and re-opening the
	 * underlying file channel if necessary.
	 * @return this instance
	 * @throws IOException on I/O error
	 */
	FileChannelDataBlock open() throws IOException {
		synchronized (this.lock) {
			if (this.referenceCount == 0) {
				debug.log("Reopening '%s'", this.path);
				this.fileChannel = this.opener.open();
			}
			this.referenceCount++;
			debug.log("Reference count for '%s' (%s,%s) incremented to %s", this.path, this.offset, this.size,
					this.referenceCount);
			return this;
		}
	}

	/**
	 * Close a connection to this block, decreasing the reference count and closing the
	 * underlying file channel if necessary.
	 * @throws IOException on I/O error
	 */
	@Override
	public void close() throws IOException {
		synchronized (this.lock) {
			if (this.referenceCount == 0) {
				return;
			}
			this.referenceCount--;
			if (this.referenceCount == 0) {
				debug.log("Closing '%s'", this.path);
				this.closer.close(this.fileChannel);
				this.fileChannel = null;
			}
			debug.log("Reference count for '%s' (%s,%s) decremented to %s", this.path, this.offset, this.size,
					this.referenceCount);
		}
	}

	/**
	 * Opens a new {@link FileChannelDataBlock} backed by the given file.
	 * @param path the path of the file to open
	 * @return a new file channel instance
	 * @throws IOException on I/O error
	 */
	static FileChannelDataBlock open(Path path) throws IOException {
		if (!Files.isRegularFile(path)) {
			throw new IllegalArgumentException(path + " must be a regular file");
		}
		return new FileChannelDataBlock(path, () -> FileChannel.open(path, StandardOpenOption.READ),
				FileChannel::close);
	}

	/**
	 * Strategy interface used to handle opening of a {@link FileChannel}.
	 */
	interface Opener {

		/**
		 * Opens the file channel.
		 * @return the file channel instance
		 * @throws IOException on I/O error
		 * @see FileChannel#open(Path, java.nio.file.OpenOption...)
		 */
		FileChannel open() throws IOException;

	}

	/**
	 * Strategy interface used to handle closing of a {@link FileChannel}.
	 */
	interface Closer {

		/**
		 * Close the file channel.
		 * @param channel the file channel to close
		 * @throws IOException on I/O error
		 */
		void close(FileChannel channel) throws IOException;

	}

}
