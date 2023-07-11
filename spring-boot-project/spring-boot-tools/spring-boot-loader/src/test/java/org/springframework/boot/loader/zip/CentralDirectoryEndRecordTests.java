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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link EndOfCentralDirectoryRecord}.
 *
 * @author pwebb
 */
class CentralDirectoryEndRecordTests {

	@TempDir
	File temp;

	@Test
	void test() throws IOException {
		File file = new File(this.temp, "test");
		Files.write(file.toPath(), new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
		FileChannelDataBlock block = new FileChannelDataBlock(FileChannel.open(file.toPath(), StandardOpenOption.READ));
		ByteBuffer buffer = ByteBuffer.allocate(5);
		// buffer.order(ByteOrder.BIG_ENDIANz);
		block.readFully(buffer, 0);
		System.out.println(HexFormat.of().formatHex(buffer.array()));
		buffer.rewind();
		System.out.println(buffer.getInt());

		// we don't need bytes

		buffer.position(buffer.limit() - 4);
		System.out.println(buffer.getInt()); // 0x1020304
		buffer.position(buffer.limit() - 5);
		System.out.println(buffer.getInt()); // 0x0010203

	}

}
