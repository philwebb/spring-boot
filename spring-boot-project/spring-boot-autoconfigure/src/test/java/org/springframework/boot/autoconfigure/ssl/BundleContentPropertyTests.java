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

package org.springframework.boot.autoconfigure.ssl;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link BundleContentProperty}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class BundleContentPropertyTests {

	private static final String PEM_TEXT = """
			-----BEGIN CERTIFICATE-----
			-----END CERTIFICATE-----
			""";

	@TempDir
	Path temp;

	@Test
	void getDirectoryGlobMatchesWhenPathIsNotGlobThrowsException() {
		BundleContentProperty property = new BundleContentProperty("name", "file.pem");
		assertThatIllegalStateException().isThrownBy(() -> property.getDirectoryGlobMatches())
			.withMessage("Property 'name' must contain a directory glob pattern");
	}

	@Test
	void getDirectoryGlobMatchesReturnsMatchingPaths() throws IOException {
		Files.createDirectory(this.temp.resolve("1d.txt"));
		Files.createDirectory(this.temp.resolve("2d.dat"));
		Files.createDirectory(this.temp.resolve("3d.txt"));
		Files.createFile(this.temp.resolve("1f.txt"));
		Files.createFile(this.temp.resolve("2f.dat"));
		Files.createFile(this.temp.resolve("3f.txt"));
		BundleContentProperty property = new BundleContentProperty("name",
				this.temp.toAbsolutePath().resolve("*.txt").toString());
		try (DirectoryStream<Path> paths = property.getDirectoryGlobMatches()) {
			List<String> names = StreamSupport.stream(paths.spliterator(), false)
				.map(Path::getFileName)
				.map(Path::toString)
				.toList();
			assertThat(names).containsExactlyInAnyOrder("1d.txt", "3d.txt", "1f.txt", "3f.txt");
		}
	}

	@Test
	void assertIsNotDirectoryGlobWhenNotGlobDoesNothing() {
		BundleContentProperty property = new BundleContentProperty("name",
				this.temp.toAbsolutePath().resolve("file.txt").toString());
		assertThatNoException().isThrownBy(property::assertIsNotDirectoryGlob);
	}

	@Test
	void assertIsNotDirectoryGlobWhenGlobThrowsException() {
		BundleContentProperty property = new BundleContentProperty("name",
				this.temp.toAbsolutePath().resolve("*.txt").toString());
		assertThatIllegalStateException().isThrownBy(property::assertIsNotDirectoryGlob)
			.withMessage("Property 'name' cannot contain a directory glob pattern");
	}

	@Test
	void isDirectoryGlobWhenHasNoValueReturnsFalse() {
		BundleContentProperty property = new BundleContentProperty("name", null);
		assertThat(property.isDirectoryGlob()).isFalse();
	}

	@Test
	void isDirectoryGlobWhenIsPemContentReturnsFalse() {
		BundleContentProperty property = new BundleContentProperty("name", PEM_TEXT);
		assertThat(property.isDirectoryGlob()).isFalse();
	}

	@Test
	void isDirectoryGlobWhenNotConvertableToFileUrlReturnsFalse() {
		BundleContentProperty property = new BundleContentProperty("name", "http://example.com");
		assertThat(property.isDirectoryGlob()).isFalse();
	}

	@Test
	void isDirectoryGlobWhenGlobReturnsTrue() {
		BundleContentProperty property = new BundleContentProperty("name",
				this.temp.toAbsolutePath().resolve("*.txt").toString());
		assertThat(property.isDirectoryGlob()).isTrue();
	}

	@Test
	void isPemContentWhenValueIsPemTextReturnsTrue() {
		BundleContentProperty property = new BundleContentProperty("name", PEM_TEXT);
		assertThat(property.isPemContent()).isTrue();
	}

	@Test
	void isPemContentWhenValueIsNotPemTextReturnsFalse() {
		BundleContentProperty property = new BundleContentProperty("name", "file.pem");
		assertThat(property.isPemContent()).isFalse();
	}

	@Test
	void hasValueWhenHasValueReturnsTrue() {
		BundleContentProperty property = new BundleContentProperty("name", "file.pem");
		assertThat(property.hasValue()).isTrue();
	}

	@Test
	void hasValueWhenHasNullValueReturnsFalse() {
		BundleContentProperty property = new BundleContentProperty("name", null);
		assertThat(property.hasValue()).isFalse();
	}

	@Test
	void hasValueWhenHasEmptyValueReturnsFalse() {
		BundleContentProperty property = new BundleContentProperty("name", "");
		assertThat(property.hasValue()).isFalse();
	}

	@Test
	void toWatchPathWhenNotPathThrowsException() {
		BundleContentProperty property = new BundleContentProperty("name", PEM_TEXT);
		assertThatIllegalStateException().isThrownBy(property::toWatchPath)
			.withMessage("Unable to convert 'name' property to a path");
	}

	@Test
	void toWatchPathWhenIsDirectoryGlobReturnsDirectory() {
		BundleContentProperty property = new BundleContentProperty("name",
				this.temp.toAbsolutePath().resolve("*.txt").toString());
		assertThat(property.toWatchPath()).isEqualTo(this.temp);
	}

	@Test
	void toWatchPathWhenPathReturnsPath() {
		Path file = this.temp.toAbsolutePath().resolve("file.txt");
		BundleContentProperty property = new BundleContentProperty("name", file.toString());
		assertThat(property.toWatchPath()).isEqualTo(file);
	}

}
