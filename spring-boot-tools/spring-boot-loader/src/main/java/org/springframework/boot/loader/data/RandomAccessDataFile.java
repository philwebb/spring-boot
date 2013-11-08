/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

/**
 * {@link RandomAccessData} implementation backed by a {@link RandomAccessFile}.
 * 
 * @author Phillip Webb
 */
public class RandomAccessDataFile implements RandomAccessData {

	private static final int DEFAULT_CONCURRENT_READS = 4;

	private static final int READ_BLOCK_SIZE = 1024 * 400 * 20;

	private File file;

	private final FilePool filePool;

	private final long offset;

	private final long length;

	/**
	 * Create a new {@link RandomAccessDataFile} backed by the specified file.
	 * @param file the underlying file
	 * @throws IllegalArgumentException if the file is null or does not exist
	 * @see #RandomAccessDataFile(File, int)
	 */
	public RandomAccessDataFile(File file) {
		this(file, DEFAULT_CONCURRENT_READS);
	}

	/**
	 * Create a new {@link RandomAccessDataFile} backed by the specified file.
	 * @param file the underlying file
	 * @param concurrentReads the maximum number of concurrent reads allowed on the
	 * underlying file before blocking
	 * @throws IllegalArgumentException if the file is null or does not exist
	 * @see #RandomAccessDataFile(File)
	 */
	public RandomAccessDataFile(File file, int concurrentReads) {
		if (file == null) {
			throw new IllegalArgumentException("File must not be null");
		}
		if (!file.exists()) {
			throw new IllegalArgumentException("File must exist");
		}
		this.file = file;
		this.filePool = new FilePool(concurrentReads);
		this.offset = 0L;
		this.length = file.length();
	}

	/**
	 * Private constructor used to create a {@link #getSubsection(long, long) subsection}.
	 * @param pool the underlying pool
	 * @param offset the offset of the section
	 * @param length the length of the section
	 */
	private RandomAccessDataFile(FilePool pool, long offset, long length) {
		this.filePool = pool;
		this.offset = offset;
		this.length = length;
	}

	/**
	 * Returns the underling File.
	 * @return the underlying file
	 */
	public File getFile() {
		return this.file;
	}

	@Override
	public InputStream getInputStream() {
		return new DataInputStream();
	}

	@Override
	public RandomAccessData getSubsection(long offset, long length) {
		if (offset < 0 || length < 0 || offset + length > this.length) {
			throw new IndexOutOfBoundsException();
		}
		return new RandomAccessDataFile(this.filePool, this.offset + offset, length);
	}

	@Override
	public long getSize() {
		return this.length;
	}

	public void close() throws IOException {
		this.filePool.close();
	}

	/**
	 * {@link RandomAccessDataInputStream} implementation for the
	 * {@link RandomAccessDataFile}.
	 */
	private class DataInputStream extends InputStream {

		private long position;

		@Override
		public int read() throws IOException {
			return doRead(null, 0, 1);
		}

		@Override
		public int read(byte[] b) throws IOException {
			return read(b, 0, b == null ? 0 : b.length);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (b == null) {
				throw new NullPointerException("Bytes must not be null");
			}
			return doRead(b, off, len);
		}

		/**
		 * Perform the actual read.
		 * @param b the bytes to read or {@code null} when reading a single byte
		 * @param off the offset of the byte array
		 * @param len the length of data to read
		 * @return the number of bytes read into {@code b} or the actual read byte if
		 * {@code b} is {@code null}. Returns -1 when the end of the stream is reached
		 * @throws IOException
		 */
		public int doRead(byte[] b, int off, int len) throws IOException {
			if (len == 0) {
				return 0;
			}
			if (cap(len) <= 0) {
				return -1;
			}
			BlockCachingRandomAccessFile file = RandomAccessDataFile.this.filePool
					.acquire();
			try {
				file.seek(RandomAccessDataFile.this.offset + this.position);
				if (b == null) {
					int rtn = file.read();
					moveOn(rtn == -1 ? 0 : 1);
					return rtn;
				}
				else {
					return (int) moveOn(file.read(b, off, (int) cap(len)));
				}
			}
			finally {
				RandomAccessDataFile.this.filePool.release(file);
			}
		}

		@Override
		public long skip(long n) throws IOException {
			return (n <= 0 ? 0 : moveOn(cap(n)));
		}

		/**
		 * Cap the specified value such that it cannot exceed the number of bytes
		 * remaining.
		 * @param n the value to cap
		 * @return the capped value
		 */
		private long cap(long n) {
			return Math.min(RandomAccessDataFile.this.length - this.position, n);
		}

		/**
		 * Move the stream position forwards the specified amount
		 * @param amount the amount to move
		 * @return the amount moved
		 */
		private long moveOn(long amount) {
			this.position += amount;
			return amount;
		}
	}

	/**
	 * Manage a pool that can be used to perform concurrent reads on the underlying
	 * {@link RandomAccessFile}.
	 */
	private class FilePool {

		private int size;

		private final Semaphore available;

		private final Queue<BlockCachingRandomAccessFile> files;

		public FilePool(int size) {
			this.size = size;
			this.available = new Semaphore(size);
			this.files = new ConcurrentLinkedQueue<BlockCachingRandomAccessFile>();
		}

		public BlockCachingRandomAccessFile acquire() throws IOException {
			try {
				this.available.acquire();
				BlockCachingRandomAccessFile file = this.files.poll();
				return (file == null ? new BlockCachingRandomAccessFile() : file);
			}
			catch (InterruptedException ex) {
				throw new IOException(ex);
			}
		}

		public void release(BlockCachingRandomAccessFile file) {
			this.files.add(file);
			this.available.release();
		}

		public void close() throws IOException {
			try {
				this.available.acquire(this.size);
				try {
					BlockCachingRandomAccessFile file = this.files.poll();
					while (file != null) {
						file.close();
						file = this.files.poll();
					}
				}
				finally {
					this.available.release(this.size);
				}
			}
			catch (InterruptedException ex) {
				throw new IOException(ex);
			}
		}
	}

	/**
	 * Wraps a {@link RandomAccessFile} to provide read caching and to load large blocks.`
	 */
	private class BlockCachingRandomAccessFile {

		private byte[] singleByte = new byte[1];

		private RandomAccessFile file;

		private long position;

		private int blockNumber = 0;

		private int blockSize = -1;

		private WeakReference<byte[]> block;

		public BlockCachingRandomAccessFile() throws FileNotFoundException {
			this.file = new RandomAccessFile(RandomAccessDataFile.this.file, "r");
		}

		public void close() throws IOException {
			this.file.close();
		}

		public int read() throws IOException {
			long read = read(this.singleByte, 0, 1);
			return (read == 1 ? this.singleByte[0] : -1);
		}

		public long read(byte[] b, int off, int len) throws IOException {
			long totalRead = 0;
			byte[] blockBytes = (this.block == null ? null : this.block.get());
			while (len > 0) {
				int requiredDataBlock = (int) (this.position / READ_BLOCK_SIZE);
				int blockStartIndex = requiredDataBlock * READ_BLOCK_SIZE;
				if (this.blockNumber != requiredDataBlock || blockBytes == null) {
					if (blockBytes == null) {
						blockBytes = new byte[READ_BLOCK_SIZE];
					}
					this.file.seek(blockStartIndex);
					this.blockSize = this.file.read(blockBytes);
					this.blockNumber = requiredDataBlock;
					this.block = new WeakReference<byte[]>(blockBytes);
				}

				// 0 1 2 3
				// 012 345 678 9 BS=3
				// 01234 56789 BS=5

				int blockOffset = (int) this.position - blockStartIndex;
				int readAmount = Math.min(this.blockSize - blockOffset, len);
				if (readAmount <= 0) {
					return (totalRead == 0 ? readAmount : totalRead);
				}
				System.arraycopy(blockBytes, blockOffset, b, off, readAmount);
				totalRead += readAmount;
				this.position += readAmount;
				off += readAmount;
				len -= readAmount;
			}
			return totalRead;
		}

		public void seek(long pos) throws IOException {
			this.position = pos;
		}
	}

}
