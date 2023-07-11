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
import java.nio.channels.FileChannel;

/**
 * @author pwebb
 */
class FileChannelDataBlock implements DataBlock {

	// FIXME needs to support slice

	private final FileChannel fileChannel;

	FileChannelDataBlock(FileChannel fileChannel) {
		this.fileChannel = fileChannel;
	}

	@Override
	public long size() throws IOException {
		return this.fileChannel.size();
	}

	@Override
	public int read(ByteBuffer dst, long position) throws IOException {
		return this.fileChannel.read(dst, position);
	}

	public void close() throws IOException {
		this.fileChannel.close();
	}

}
