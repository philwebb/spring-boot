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

package org.springframework.boot.loader.nio.file;

import java.io.File;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.net.protocol.jar.JarUrl;
import org.springframework.boot.loader.testsupport.TestJar;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link NestedFileSystem} in combination with
 * {@code ZipFileSystem}.
 *
 * @author Phillip Webb
 */
class NestedFileSystemZipFileSystemIntegrationTests {

	@TempDir
	File temp;

	@Test
	void zip() throws Exception {
		File jar = new File(this.temp, "test.jar");
		TestJar.create(jar);
		URI uri = JarUrl.create(jar).toURI();
		FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
		assertThat(Files.readAllBytes(fs.getPath("1.dat"))).containsExactly(0x1);
	}

	@Test
	void nestedZip() throws Exception {
		File jar = new File(this.temp, "test.jar");
		TestJar.create(jar);
		URI uri = JarUrl.create(jar, "nested.jar").toURI();
		FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
		assertThat(Files.readAllBytes(fs.getPath("3.dat"))).containsExactly(0x3);
	}

}
