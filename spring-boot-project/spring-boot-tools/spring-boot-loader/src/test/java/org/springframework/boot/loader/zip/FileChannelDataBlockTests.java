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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.zip.FileChannelDataBlock.Closer;
import org.springframework.boot.loader.zip.FileChannelDataBlock.Opener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link FileChannelDataBlock}.
 *
 * @author Phillip Webb
 */
class FileChannelDataBlockTests {

	private static final byte[] CONTENT = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05 };

	@TempDir
	File tempDir;

	File tempFile;

	@BeforeEach
	void writeTempFile() throws IOException {
		this.tempFile = new File(this.tempDir, "content");
		Files.write(this.tempFile.toPath(), CONTENT);
	}

	@Test
	void sizeReturnsFileSize() throws IOException {
		try (FileChannelDataBlock block = FileChannelDataBlock.open(this.tempFile.toPath())) {
			assertThat(block.size()).isEqualTo(CONTENT.length);
		}
	}

	@Test
	void readReadsFile() throws IOException {
		try (FileChannelDataBlock block = FileChannelDataBlock.open(this.tempFile.toPath())) {
			ByteBuffer buffer = ByteBuffer.allocate(CONTENT.length);
			assertThat(block.read(buffer, 0)).isEqualTo(6);
			assertThat(buffer.array()).containsExactly(CONTENT);
		}
	}

	@Test
	void readDoesNotReadPastEndOfFile() throws IOException {
		try (FileChannelDataBlock block = FileChannelDataBlock.open(this.tempFile.toPath())) {
			ByteBuffer buffer = ByteBuffer.allocate(CONTENT.length);
			assertThat(block.read(buffer, 2)).isEqualTo(4);
			assertThat(buffer.array()).containsExactly(0x02, 0x03, 0x04, 0x05, 0x0, 0x0);
		}
	}

	@Test
	void readWhenPosAtSizeReturnsMinusOne() throws IOException {
		try (FileChannelDataBlock block = FileChannelDataBlock.open(this.tempFile.toPath())) {
			ByteBuffer buffer = ByteBuffer.allocate(CONTENT.length);
			assertThat(block.read(buffer, 6)).isEqualTo(-1);
		}
	}

	@Test
	void readWhenPosOverSizeReturnsMinusOne() throws IOException {
		try (FileChannelDataBlock block = FileChannelDataBlock.open(this.tempFile.toPath())) {
			ByteBuffer buffer = ByteBuffer.allocate(CONTENT.length);
			assertThat(block.read(buffer, 7)).isEqualTo(-1);
		}
	}

	@Test
	void readWhenPosIsNegativeThrowsException() throws IOException {
		try (FileChannelDataBlock block = FileChannelDataBlock.open(this.tempFile.toPath())) {
			ByteBuffer buffer = ByteBuffer.allocate(CONTENT.length);
			assertThatIllegalArgumentException().isThrownBy(() -> block.read(buffer, -1));
		}
	}

	@Test
	void openSliceWhenOffsetIsNegativeThrowsException() throws IOException {
		try (FileChannelDataBlock block = FileChannelDataBlock.open(this.tempFile.toPath())) {
			assertThatIllegalArgumentException().isThrownBy(() -> block.openSlice(-1, 0))
				.withMessage("Offset must not be negative");
		}
	}

	@Test
	void openSliceWhenSizeIsNegativeThrowsException() throws IOException {
		try (FileChannelDataBlock block = FileChannelDataBlock.open(this.tempFile.toPath())) {
			assertThatIllegalArgumentException().isThrownBy(() -> block.openSlice(0, -1))
				.withMessage("Size must not be negative and must be within bounds");
		}
	}

	@Test
	void openSliceWhenSizeIsOutOfBoundsThrowsException() throws IOException {
		try (FileChannelDataBlock block = FileChannelDataBlock.open(this.tempFile.toPath())) {
			assertThatIllegalArgumentException().isThrownBy(() -> block.openSlice(2, 5))
				.withMessage("Size must not be negative and must be within bounds");
		}
	}

	@Test
	void openSliceOpensSlice() throws IOException {
		try (FileChannelDataBlock block = FileChannelDataBlock.open(this.tempFile.toPath())) {
			try (FileChannelDataBlock slice = block.openSlice(1, 4)) {
				assertThat(slice.size()).isEqualTo(4);
				ByteBuffer buffer = ByteBuffer.allocate(4);
				assertThat(slice.read(buffer, 0)).isEqualTo(4);
				assertThat(buffer.array()).containsExactly(0x01, 0x02, 0x03, 0x04);
			}
		}
	}

	@Test
	void openAndCloseHandleReferenceCounting() throws IOException {
		TestOpener opener = new TestOpener();
		TestCloser closer = new TestCloser();
		FileChannelDataBlock block = new FileChannelDataBlock(opener, closer);
		assertThat(block).extracting("referenceCount").isEqualTo(1);
		assertThat(opener.calls).isEqualTo(1);
		assertThat(closer.calls).isEqualTo(0);
		block.open();
		assertThat(block).extracting("referenceCount").isEqualTo(2);
		assertThat(opener.calls).isEqualTo(1);
		assertThat(closer.calls).isEqualTo(0);
		block.close();
		assertThat(block).extracting("referenceCount").isEqualTo(1);
		assertThat(opener.calls).isEqualTo(1);
		assertThat(closer.calls).isEqualTo(0);
		block.close();
		assertThat(block).extracting("referenceCount").isEqualTo(0);
		assertThat(opener.calls).isEqualTo(1);
		assertThat(closer.calls).isEqualTo(1);
		block.close();
		assertThat(block).extracting("referenceCount").isEqualTo(0);
		assertThat(opener.calls).isEqualTo(1);
		assertThat(closer.calls).isEqualTo(1);
		block.open();
		assertThat(block).extracting("referenceCount").isEqualTo(1);
		assertThat(opener.calls).isEqualTo(2);
		assertThat(closer.calls).isEqualTo(1);
		block.close();
		assertThat(block).extracting("referenceCount").isEqualTo(0);
		assertThat(opener.calls).isEqualTo(2);
		assertThat(closer.calls).isEqualTo(2);
	}

	@Test
	void openAndCloseSliceHandleReferenceCounting() throws IOException {
		TestOpener opener = new TestOpener();
		TestCloser closer = new TestCloser();
		FileChannelDataBlock block = new FileChannelDataBlock(opener, closer);
		assertThat(block).extracting("referenceCount").isEqualTo(1);
		assertThat(opener.calls).isEqualTo(1);
		assertThat(closer.calls).isEqualTo(0);
		FileChannelDataBlock slice = block.openSlice(1, 4);
		assertThat(block).extracting("referenceCount").isEqualTo(2);
		assertThat(slice).extracting("referenceCount").isEqualTo(1);
		slice.open();
		assertThat(block).extracting("referenceCount").isEqualTo(2);
		assertThat(slice).extracting("referenceCount").isEqualTo(2);
		slice.close();
		assertThat(block).extracting("referenceCount").isEqualTo(2);
		assertThat(slice).extracting("referenceCount").isEqualTo(1);
		slice.close();
		assertThat(block).extracting("referenceCount").isEqualTo(1);
		assertThat(slice).extracting("referenceCount").isEqualTo(0);
		block.close();
		assertThat(block).extracting("referenceCount").isEqualTo(0);
		assertThat(slice).extracting("referenceCount").isEqualTo(0);
		assertThat(opener.calls).isEqualTo(1);
		assertThat(closer.calls).isEqualTo(1);
		slice.open();
		assertThat(block).extracting("referenceCount").isEqualTo(1);
		assertThat(slice).extracting("referenceCount").isEqualTo(1);
		slice.close();
		assertThat(block).extracting("referenceCount").isEqualTo(0);
		assertThat(slice).extracting("referenceCount").isEqualTo(0);
		assertThat(opener.calls).isEqualTo(2);
		assertThat(closer.calls).isEqualTo(2);
	}

	class TestOpener implements Opener {

		int calls;

		@Override
		public FileChannel open() throws IOException {
			this.calls++;
			return FileChannel.open(FileChannelDataBlockTests.this.tempFile.toPath(), StandardOpenOption.READ);
		}

	}

	class TestCloser implements Closer {

		int calls;

		@Override
		public void close(FileChannel channel) throws IOException {
			this.calls++;
			channel.close();
		}

	}

}
