/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.env;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DirectoryProperySource}.
 *
 * @author Phillip Webb
 */
class DirectoryProperySourceTests {

	@TempDir
	File directory;

	@Test
	void createWhenNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DirectoryProperySource(null, this.directory))
				.withMessageContaining("name must contain");
	}

	@Test
	void createWhenSourceIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DirectoryProperySource("test", null))
				.withMessage("Property source must not be null");
	}

	@Test
	void createWhenSourceDoesNotExistThrowsException() {
		File missing = new File(this.directory, "missing");
		assertThatIllegalArgumentException().isThrownBy(() -> new DirectoryProperySource("test", missing))
				.withMessage("Directory '" + missing + "' does not exist");
	}

	@Test
	void createWhenSourceIsFileThrowsException() throws Exception {
		File file = new File(this.directory, "file");
		FileCopyUtils.copy("test".getBytes(StandardCharsets.UTF_8), file);
		assertThatIllegalArgumentException().isThrownBy(() -> new DirectoryProperySource("test", file))
				.withMessage("File '" + file + "' is not a directory");
	}

	@Test
	void getPropertyNamesFromFlatReturnsPropertyNames() throws Exception {
		DirectoryProperySource properySource = getFlatPropertySource();
		assertThat(properySource.getPropertyNames()).containsExactly("a", "b", "c");
	}

	@Test
	void getPropertyFromFlatReturnsFileContent() throws Exception {
		DirectoryProperySource properySource = getFlatPropertySource();
		assertThat(properySource.getProperty("b")).isEqualTo("B");
	}

	@Test
	void getPropertyFromFlatWhenMissingReturnsNull() throws Exception {
		DirectoryProperySource properySource = getFlatPropertySource();
		assertThat(properySource.getProperty("missing")).isNull();
	}

	@Test
	void getPropertyFromFlatWhenFileDeletedThrowsException() {
		// FIXME
	}

	@Test
	void getOriginFromFlatReturnsOrigin() throws Exception {
		DirectoryProperySource properySource = getFlatPropertySource();
		TextResourceOrigin origin = (TextResourceOrigin) properySource.getOrigin("b");
		assertThat(origin.getResource().getFile()).isEqualTo(new File(this.directory, "b"));
		assertThat(origin.getLocation().getLine()).isEqualTo(0);
		assertThat(origin.getLocation().getColumn()).isEqualTo(0);
	}

	@Test
	void getOriginFromFlatWhenMissingReturnsNull() throws Exception {
		DirectoryProperySource properySource = getFlatPropertySource();
		assertThat(properySource.getOrigin("missing")).isNull();
	}

	@Test
	void getOriginFromFlatWhenFileDeletedThrowsException() {
		// FIXME
	}

	private DirectoryProperySource getFlatPropertySource() throws IOException {
		addProperty("a", "A");
		addProperty("b", "B");
		addProperty("c", "C");
		return new DirectoryProperySource("test", this.directory);
	}

	private void addProperty(String path, String value) throws IOException {
		File file = new File(this.directory, path);
		FileCopyUtils.copy(value.getBytes(StandardCharsets.UTF_8), file);
	}

}
