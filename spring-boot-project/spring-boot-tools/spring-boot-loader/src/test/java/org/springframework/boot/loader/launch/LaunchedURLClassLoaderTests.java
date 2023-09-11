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

package org.springframework.boot.loader.launch;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.net.protocol.Handlers;
import org.springframework.boot.loader.net.protocol.jar.Handler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LaunchedURLClassLoader}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@SuppressWarnings("resource")
class LaunchedURLClassLoaderTests {

	@TempDir
	File tempDir;

	@BeforeAll
	static void setup() {
		Handlers.register();
	}

	@Test
	void resolveResourceFromArchive() throws Exception {
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(
				new URL[] { new URL("jar:file:src/test/resources/jars/app.jar!/") }, getClass().getClassLoader());
		assertThat(loader.getResource("demo/Application.java")).isNotNull();
	}

	@Test
	void resolveResourcesFromArchive() throws Exception {
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(
				new URL[] { new URL("jar:file:src/test/resources/jars/app.jar!/") }, getClass().getClassLoader());
		assertThat(loader.getResources("demo/Application.java").hasMoreElements()).isTrue();
	}

	@Test
	void resolveRootPathFromArchive() throws Exception {
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(
				new URL[] { new URL("jar:file:src/test/resources/jars/app.jar!/") }, getClass().getClassLoader());
		assertThat(loader.getResource("")).isNotNull();
	}

	@Test
	void resolveRootResourcesFromArchive() throws Exception {
		LaunchedURLClassLoader loader = new LaunchedURLClassLoader(
				new URL[] { new URL("jar:file:src/test/resources/jars/app.jar!/") }, getClass().getClassLoader());
		assertThat(loader.getResources("").hasMoreElements()).isTrue();
	}

	@Test
	void resolveFromNested() throws Exception {
		File file = new File(this.tempDir, "test.jar");
		TestJarCreator.createTestJar(file);
		URL url = Handler.create(file, "nested.jar");
		try (LaunchedURLClassLoader loader = new LaunchedURLClassLoader(new URL[] { url }, null)) {
			URL resource = loader.getResource("3.dat");
			assertThat(resource).hasToString(url + "3.dat");
			try (InputStream input = resource.openConnection().getInputStream()) {
				assertThat(input.read()).isEqualTo(3);
			}
		}
	}

}
