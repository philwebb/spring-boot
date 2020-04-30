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
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.InputStreamSource;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DirectoryPathProperySource}.
 *
 * @author Phillip Webb
 */
class DirectoryPathProperySourceTests {

	@TempDir
	Path directory;

	@Test
	void createWhenNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DirectoryPathProperySource(null, this.directory))
				.withMessageContaining("name must contain");
	}

	@Test
	void createWhenSourceIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DirectoryPathProperySource("test", null))
				.withMessage("Property source must not be null");
	}

	@Test
	void createWhenSourceDoesNotExistThrowsException() {
		Path missing = this.directory.resolve("missing");
		assertThatIllegalArgumentException().isThrownBy(() -> new DirectoryPathProperySource("test", missing))
				.withMessage("Directory '" + missing + "' does not exist");
	}

	@Test
	void createWhenSourceIsFileThrowsException() throws Exception {
		Path file = this.directory.resolve("file");
		FileCopyUtils.copy("test".getBytes(StandardCharsets.UTF_8), file.toFile());
		assertThatIllegalArgumentException().isThrownBy(() -> new DirectoryPathProperySource("test", file))
				.withMessage("File '" + file + "' is not a directory");
	}

	@Test
	void getPropertyNamesFromFlatReturnsPropertyNames() throws Exception {
		DirectoryPathProperySource properySource = getFlatPropertySource();
		assertThat(properySource.getPropertyNames()).containsExactly("a", "b", "c");
	}

	@Test
	void getPropertyFromFlatReturnsFileContent() throws Exception {
		DirectoryPathProperySource properySource = getFlatPropertySource();
		assertThat(properySource.getProperty("b")).hasToString("B");
	}

	@Test
	void getPropertyFromFlatWhenMissingReturnsNull() throws Exception {
		DirectoryPathProperySource properySource = getFlatPropertySource();
		assertThat(properySource.getProperty("missing")).isNull();
	}

	@Test
	void getPropertyFromFlatWhenFileDeletedThrowsException() {
		// FIXME
	}

	@Test
	void getOriginFromFlatReturnsOrigin() throws Exception {
		DirectoryPathProperySource properySource = getFlatPropertySource();
		TextResourceOrigin origin = (TextResourceOrigin) properySource.getOrigin("b");
		assertThat(origin.getResource().getFile()).isEqualTo(this.directory.resolve("b").toFile());
		assertThat(origin.getLocation().getLine()).isEqualTo(0);
		assertThat(origin.getLocation().getColumn()).isEqualTo(0);
	}

	@Test
	void getOriginFromFlatWhenMissingReturnsNull() throws Exception {
		DirectoryPathProperySource properySource = getFlatPropertySource();
		assertThat(properySource.getOrigin("missing")).isNull();
	}

	@Test
	void getOriginFromFlatWhenFileDeletedThrowsException() {
		// FIXME
	}

	@Test
	void getPropertyViaEnvironmentSupportsConversion() throws Exception {
		StandardEnvironment environment = new StandardEnvironment();
		ConversionService conversionService = ApplicationConversionService.getSharedInstance();
		environment.setConversionService((ConfigurableConversionService) conversionService);
		environment.getPropertySources().addFirst(getFlatPropertySource());
		assertThat(environment.getProperty("a")).isEqualTo("A");
		assertThat(environment.getProperty("b")).isEqualTo("B");
		assertThat(environment.getProperty("c", InputStreamSource.class).getInputStream()).hasContent("C");
		assertThat(environment.getProperty("c", byte[].class)).contains('C');
	}

	private DirectoryPathProperySource getFlatPropertySource() throws IOException {
		addProperty("a", "A");
		addProperty("b", "B");
		addProperty("c", "C");
		return new DirectoryPathProperySource("test", this.directory);
	}

	private void addProperty(String path, String value) throws IOException {
		File file = this.directory.resolve(path).toFile();
		FileCopyUtils.copy(value.getBytes(StandardCharsets.UTF_8), file);
	}

}
