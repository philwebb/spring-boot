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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FileChannelSlice}.
 *
 * @author Phillip Webb
 */
class FileChannelSliceTests {

	@TempDir
	File tempDir;

	File tempFile;

	@BeforeEach
	void writeTempFile() throws IOException {
		this.tempFile = new File(this.tempDir, "content");
		Files.write(this.tempFile.toPath(), new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05 });
	}

	@Test
	void readAdvancesPosition() throws IOException {
		FileChannelSlice slice = FileChannelSlice.open(this.tempFile.toPath(), 1, 4);
		ByteBuffer dst1 = ByteBuffer.allocate(2);
		assertThat(slice.position()).isEqualTo(0);
		assertThat(slice.read(dst1)).isEqualTo(2);
		assertThat(slice.position()).isEqualTo(2);
		assertThat(dst1.array()).contains(0x01, 0x02);
		ByteBuffer dst2 = ByteBuffer.allocate(2);
		assertThat(slice.read(dst2)).isEqualTo(2);
		assertThat(slice.position()).isEqualTo(4);
		assertThat(dst2.array()).contains(0x03, 0x04);
	}

	@Test
	void readWhenBufferIsLargerThanRemaining() throws IOException {
		FileChannelSlice slice = FileChannelSlice.open(this.tempFile.toPath(), 1, 4);
		ByteBuffer dst = ByteBuffer.allocate(8);
		assertThat(slice.read(dst)).isEqualTo(4);
		assertThat(dst.array()).startsWith(0x01, 0x02, 0x03, 0x04);
	}

	@Test
	void readWhenClosedThrowsException() {

	}

	@Test
	void positionWhenLessThanZeroThrowsException() {

	}

	@Test
	void positionWhenGreaterThanMaxIntegerThrowsException() {

	}

	@Test
	void positionWhenClosedThrowsException() {

	}

	@Test
	void positionUpdatesPosition() throws IOException {
		FileChannelSlice slice = FileChannelSlice.open(this.tempFile.toPath(), 1, 4);
		ByteBuffer dst = ByteBuffer.allocate(2);
		assertThat(slice.position()).isEqualTo(0);
		slice.position(2);
		assertThat(slice.position()).isEqualTo(2);
		slice.read(dst);
		assertThat(dst.array()).contains(0x03, 0x04);
	}

	@Test
	void isOpenWhenOpen() {

	}

	@Test
	void isOpenWhenClosed() {
	}

	@Test
	void writeThrowsException() {

	}

	@Test
	void truncateThrowsException() {

	}

	@Test
	void openWhenSlicedAtBounds() throws IOException {
		FileChannelSlice slice = FileChannelSlice.open(this.tempFile.toPath(), 0, 6);
		ByteBuffer dst = ByteBuffer.allocate(6);
		assertThat(slice.size()).isEqualTo(6);
		assertThat(slice.read(dst)).isEqualTo(6);
		assertThat(dst.array()).contains(0x00, 0x01, 0x02, 0x03, 0x04, 0x05);
	}

	@Test
	void openWhenSlicedWithinBounds() throws IOException {
		FileChannelSlice slice = FileChannelSlice.open(this.tempFile.toPath(), 1, 4);
		ByteBuffer dst = ByteBuffer.allocate(4);
		assertThat(slice.size()).isEqualTo(4);
		assertThat(slice.read(dst)).isEqualTo(4);
		assertThat(dst.array()).contains(0x01, 0x02, 0x03, 0x04);
	}

	// FIXME open asserts

}
