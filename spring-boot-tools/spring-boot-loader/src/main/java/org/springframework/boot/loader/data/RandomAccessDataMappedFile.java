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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

/**
 * @author Phillip Webb
 */
public class RandomAccessDataMappedFile implements RandomAccessData {

	private final FileChannel channel;

	private final ByteBufferRandomAccessData data;

	private final File file;

	public RandomAccessDataMappedFile(File file) {
		if (file == null) {
			throw new IllegalArgumentException("File must not be null");
		}
		if (!file.exists()) {
			throw new IllegalArgumentException("File must exist");
		}
		try {
			this.file = file;
			this.channel = new RandomAccessFile(file, "r").getChannel();
			this.data = new ByteBufferRandomAccessData(this.channel.map(
					MapMode.READ_ONLY, 0, file.length()));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	public InputStream getInputStream(ResourceAccess access) throws IOException {
		return this.data.getInputStream(access);
	}

	@Override
	public RandomAccessData getSubsection(long offset, long length) {
		return this.data.getSubsection(offset, length);
	}

	@Override
	public long getSize() {
		return this.data.getSize();
	}

	public File getFile() {
		return this.file;
	}

	public void close() throws IOException {
		this.channel.close();
	}

}
