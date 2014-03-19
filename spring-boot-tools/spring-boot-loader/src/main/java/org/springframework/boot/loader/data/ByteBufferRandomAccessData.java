/*
 * Copyright 2012-2014 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @author Phillip Webb
 */
public class ByteBufferRandomAccessData implements RandomAccessData {

	private final ByteBuffer buffer;

	public ByteBufferRandomAccessData(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public InputStream getInputStream(ResourceAccess access) throws IOException {
		return new ByteBufferInputStream(this.buffer.duplicate());
	}

	@Override
	public RandomAccessData getSubsection(long offset, long length) {
		if (offset < 0 || length < 0 || offset + length > getSize()) {
			throw new IndexOutOfBoundsException();
		}
		ByteBuffer duplicate = this.buffer.duplicate();
		duplicate.position(this.buffer.position() + (int) offset);
		duplicate.limit(duplicate.position() + (int) length);
		return new ByteBufferRandomAccessData(duplicate);

	}

	@Override
	public long getSize() {
		return this.buffer.limit() - this.buffer.position();
	}

	public static class ByteBufferInputStream extends InputStream {

		private final ByteBuffer buffer;

		public ByteBufferInputStream(ByteBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public int read() throws IOException {
			return (this.buffer.hasRemaining() ? this.buffer.get() & 0xFF : -1);
		}

		@Override
		public int read(byte[] bytes) throws IOException {
			return read(bytes, 0, (bytes == null ? 0 : bytes.length));
		}

		@Override
		public int read(byte[] bytes, int offset, int length) throws IOException {
			if (bytes == null) {
				throw new NullPointerException("Bytes must not be null");
			}
			length = Math.min(length,
					this.buffer.hasRemaining() ? this.buffer.remaining() : -1);
			if (length > 0) {
				this.buffer.get(bytes, offset, length);
			}
			return length;
		}
	}

}
