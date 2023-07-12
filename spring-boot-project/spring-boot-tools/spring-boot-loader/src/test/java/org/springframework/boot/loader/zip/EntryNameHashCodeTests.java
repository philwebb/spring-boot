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
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.TestJarCreator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EntryName}.
 *
 * @author Phillip Webb
 */
class EntryNameHashCodeTests {

	private static final String TEST_NAME_WITH_SLASH = "test/one/";

	private static final String TEST_NAME_WITHOUT_SLASH = "test/one";

	@Test
	void calculateWithCharSequenceReturnsSameHashCodeAsString() {
		StringBuilder charSequence = new StringBuilder(TEST_NAME_WITH_SLASH);
		assertThat(EntryName.calculate(charSequence)).isEqualTo(TEST_NAME_WITH_SLASH.hashCode());
	}

	@Test
	void calculateWithCharSequenceWhenHasNoSlashReturnsSameHashCodeAsStringWithSlash() {
		StringBuilder charSequence = new StringBuilder(TEST_NAME_WITHOUT_SLASH);
		assertThat(EntryName.calculate(charSequence)).isEqualTo(TEST_NAME_WITH_SLASH.hashCode());
	}

	@Test
	void calculateWithStringReturnsSameHashCodeAsString() {
		assertThat(EntryName.calculate(TEST_NAME_WITH_SLASH)).isEqualTo(TEST_NAME_WITH_SLASH.hashCode());
	}

	@Test
	void calculateWithStringWhenHasNoSlashReturnsSameHashCodeAsStringWithSlash() {
		assertThat(EntryName.calculate(TEST_NAME_WITHOUT_SLASH)).isEqualTo(TEST_NAME_WITH_SLASH.hashCode());
	}

	@Test
	void calculateWithCharSequenceWhenEmptyReturnsEmptyHashCode() {
		assertThat(EntryName.calculate("")).isEqualTo("/".hashCode());
	}

	@Test
	void caclculateWithDataBlockCalculatesHashCode(@TempDir File tempDir) throws Exception {
		String s = "\u0432\u0435\u0441\u043D\u0430";
		System.out.println(HexFormat.of().formatHex(s.getBytes(StandardCharsets.UTF_16)));
		System.out.println(s);

		File zipFile = new File(tempDir, "test.jar");
		TestJarCreator.createTestJar(zipFile);
		try (FileChannelDataBlock dataBlock = FileChannelDataBlock.open(zipFile.toPath())) {
			EndOfCentralDirectoryRecord eocd = EndOfCentralDirectoryRecord.load(dataBlock);
			EntryName.calculate(dataBlock, eocd.pos() + eocd.size() - eocd.commentLength(),
					eocd.commentLength());
		}
	}

}
