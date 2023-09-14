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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.launch.Archive.Entry;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ExplodedArchive}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 */
class ExplodedArchiveTests {

	@TempDir
	File tempDir;

	private File rootDirectory;

	private ExplodedArchive archive;

	@BeforeEach
	void setup() throws Exception {
		createArchive();
	}

	@AfterEach
	void tearDown() throws Exception {
		if (this.archive != null) {
			this.archive.close();
		}
	}

	@Test
	void getManifestReturnsManifest() throws Exception {
		assertThat(this.archive.getManifest().getMainAttributes().getValue("Built-By")).isEqualTo("j1");
	}

	@Test
	void getClassPathUrlsWhenNoPredicartesReturnsUrls() throws Exception {
		Set<URL> urls = this.archive.getClassPathUrls(null, null);
		URL[] expectedUrls = TestJarCreator.expectedEntries().stream().map(this::toUrl).toArray(URL[]::new);
		assertThat(urls).containsExactlyInAnyOrder(expectedUrls);
	}

	@Test
	void getClassPathUrlsWhenHasIncludeFilterReturnsUrls() throws Exception {
		Set<URL> urls = this.archive.getClassPathUrls(null, this::entryNameIsNestedJar);
		assertThat(urls).containsOnly(toUrl("nested.jar"));
	}

	@Test
	void getClassPathUrlsWhenHasSearchFilterReturnsUrls() throws Exception {
		Set<URL> urls = this.archive.getClassPathUrls(this::entryNameIsNestedJar, null);
		assertThat(urls).containsOnly(toUrl("nested.jar"));
	}

	@Test
	void getUrlWithSpaceInPath() throws Exception {
		createArchive("spaces in the name");
		assertThat(this.archive.getUrl()).isEqualTo(this.rootDirectory.toURI().toURL());
	}

	@Test
	void getNestedArchive() throws Exception {
		Entry entry = getEntriesMap(this.archive).get("nested.jar");
		Archive nested = this.archive.getNestedArchive(entry);
		assertThat(nested.getUrl()).hasToString(this.rootDirectory.toURI() + "nested.jar");
		nested.close();
	}

	@Test
	void nestedDirArchive() throws Exception {
		Entry entry = getEntriesMap(this.archive).get("d/");
		Archive nested = this.archive.getNestedArchive(entry);
		Map<String, Entry> nestedEntries = getEntriesMap(nested);
		assertThat(nestedEntries).hasSize(1);
		assertThat(nested.getUrl()).hasToString("file:" + this.rootDirectory.toURI().getPath() + "d/");
	}

	@Test
	void getNonRecursiveEntriesForRoot() throws Exception {
		try (ExplodedArchive explodedArchive = new ExplodedArchive(new File("/"), false)) {
			Map<String, Archive.Entry> entries = getEntriesMap(explodedArchive);
			assertThat(entries).hasSizeGreaterThan(1);
		}
	}

	@Test
	void getNonRecursiveManifest() throws Exception {
		try (ExplodedArchive explodedArchive = new ExplodedArchive(new File("src/test/resources/root"))) {
			assertThat(explodedArchive.getManifest()).isNotNull();
			Map<String, Archive.Entry> entries = getEntriesMap(explodedArchive);
			assertThat(entries).hasSize(4);
		}
	}

	@Test
	void getNonRecursiveManifestEvenIfNonRecursive() throws Exception {
		try (ExplodedArchive explodedArchive = new ExplodedArchive(new File("src/test/resources/root"), false)) {
			assertThat(explodedArchive.getManifest()).isNotNull();
			Map<String, Archive.Entry> entries = getEntriesMap(explodedArchive);
			assertThat(entries).hasSize(3);
		}
	}

	@Test
	void getResourceAsStream() throws Exception {
		try (ExplodedArchive explodedArchive = new ExplodedArchive(new File("src/test/resources/root"))) {
			assertThat(explodedArchive.getManifest()).isNotNull();
			URLClassLoader loader = new URLClassLoader(new URL[] { explodedArchive.getUrl() });
			assertThat(loader.getResourceAsStream("META-INF/spring/application.xml")).isNotNull();
			loader.close();
		}
	}

	@Test
	void getResourceAsStreamNonRecursive() throws Exception {
		try (ExplodedArchive explodedArchive = new ExplodedArchive(new File("src/test/resources/root"), false)) {
			assertThat(explodedArchive.getManifest()).isNotNull();
			URLClassLoader loader = new URLClassLoader(new URL[] { explodedArchive.getUrl() });
			assertThat(loader.getResourceAsStream("META-INF/spring/application.xml")).isNotNull();
			loader.close();
		}
	}

	private void createArchive() throws Exception {
		createArchive(null);
	}

	private void createArchive(String directoryName) throws Exception {
		File file = new File(this.tempDir, "test.jar");
		TestJarCreator.createTestJar(file);
		this.rootDirectory = (StringUtils.hasText(directoryName) ? new File(this.tempDir, directoryName)
				: new File(this.tempDir, UUID.randomUUID().toString()));
		try (JarFile jarFile = new JarFile(file)) {
			Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				File destination = new File(this.rootDirectory.getAbsolutePath() + File.separator + entry.getName());
				destination.getParentFile().mkdirs();
				if (entry.isDirectory()) {
					destination.mkdir();
				}
				else {
					try (InputStream in = jarFile.getInputStream(entry);
							OutputStream out = new FileOutputStream(destination)) {
						in.transferTo(out);
					}
				}
			}
			this.archive = new ExplodedArchive(this.rootDirectory);
		}
	}

	private URL toUrl(String name) {
		return toUrl(new File(this.rootDirectory, name));
	}

	private URL toUrl(File file) {
		try {
			return file.toURI().toURL();
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private boolean entryNameIsNestedJar(Entry entry) {
		return entry.getName().equals("nested.jar");
	}

}
