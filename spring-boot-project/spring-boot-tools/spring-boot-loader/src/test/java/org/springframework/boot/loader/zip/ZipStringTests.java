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

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ZipString}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ZipStringTests {

	@ParameterizedTest
	@EnumSource
	void hashCodeGeneratesCorrectHashCode(SourceType sourceType) throws Exception {
		testHashCode(sourceType, true, "abcABC123xyz!");
		testHashCode(sourceType, false, "abcABC123xyz!");
	}

	@ParameterizedTest
	@EnumSource
	void hashCodeWhenHasSpecialCharsGeneratesCorrectHashCode(SourceType sourceType) throws Exception {
		testHashCode(sourceType, true, "special/\u00EB.dat");
	}

	@ParameterizedTest
	@EnumSource
	void hashCodeWhenHasCyrillicCharsGeneratesCorrectHashCode(SourceType sourceType) throws Exception {
		testHashCode(sourceType, true, "\u0432\u0435\u0441\u043D\u0430");
	}

	@ParameterizedTest
	@EnumSource
	void hashCodeWhenHasEmojiGeneratesCorrectHashCode(SourceType sourceType) throws Exception {
		testHashCode(sourceType, true, "\ud83d\udca9");
	}

	@ParameterizedTest
	@EnumSource
	void hashCodeWhenOnlyDifferenceIsEndSlashGeneratesSameHashCode(SourceType sourceType) throws Exception {
		testHashCode(sourceType, "", true, "/".hashCode());
		testHashCode(sourceType, "/", true, "/".hashCode());
		testHashCode(sourceType, "a/b", true, "a/b/".hashCode());
		testHashCode(sourceType, "a/b/", true, "a/b/".hashCode());
	}

	void testHashCode(SourceType sourceType, boolean addEndSlash, String source) throws Exception {
		String expected = (addEndSlash && !source.endsWith("/")) ? source + "/" : source;
		testHashCode(sourceType, source, addEndSlash, expected.hashCode());
	}

	void testHashCode(SourceType sourceType, String source, boolean addEndSlash, int expected) throws Exception {
		switch (sourceType) {
			case STRING -> {
				assertThat(ZipString.hashCode(source, addEndSlash)).isEqualTo(expected);
			}
			case CHAR_SEQUENCE -> {
				CharSequence charSequence = new StringBuilder(source);
				assertThat(ZipString.hashCode(charSequence, addEndSlash)).isEqualTo(expected);
			}
			case DATA_BLOCK -> {
				ByteArrayDataBlock dataBlock = new ByteArrayDataBlock(source.getBytes(StandardCharsets.UTF_8));
				assertThat(ZipString.hashCode(dataBlock, 0, (int) dataBlock.size(), addEndSlash)).isEqualTo(expected);

			}
		}

	}

	enum SourceType {

		STRING, CHAR_SEQUENCE, DATA_BLOCK

	}

}
