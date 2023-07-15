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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.TestJarCreator;
import org.springframework.boot.loader.zip.ZipContent.Entry;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link ZipContent}.
 *
 * @author Phillip Webb
 * @author Martin Lau
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class ZipContentTests {

	@TempDir
	File tempDir;

	private File file;

	private ZipContent zipContent;

	@BeforeEach
	void setup() throws Exception {
		this.file = new File(this.tempDir, "test.jar");
		TestJarCreator.createTestJar(this.file);
		this.zipContent = ZipContent.open(this.file.toPath());
	}

	@AfterEach
	void tearDown() throws Exception {
		if (this.zipContent != null) {
			this.zipContent.close();
		}
	}

	@Test
	void getCommentReturnsComment() {
		assertThat(this.zipContent.getComment()).isEqualTo("outer");
	}

	@Test
	void getCommentWhenClosedThrowsException() throws IOException {
		this.zipContent.close();
		this.zipContent.close();
		assertThatIllegalStateException().isThrownBy(() -> this.zipContent.getComment())
			.withMessage("Zip content has been closed");
	}

	@Test
	void getEntryWhenPresentreturnsEntry() {
		Entry entry = this.zipContent.getEntry("1.dat");
		assertThat(entry).isNotNull();
		assertThat(entry.getName()).isEqualTo("1.dat");
	}

	@Test
	void getEntryWhenMissingReturnsNull() {
		assertThat(this.zipContent.getEntry("missing.dat")).isNull();
	}

	@Test
	void iteratorIteratesEntries() {
		assertHasExpectedEntries(this.zipContent.iterator());
	}

	@Test
	void iteratorWhenClosedThrowsException() {

	}

	@Test
	void iteratorWhenClosedLaterThrowsException() {

	}

	@Test
	void getEntryWhenUsingSlashesIsCompatibleWithZipFile() throws IOException {
		try (ZipFile zipFile = new ZipFile(this.file)) {
			assertThat(zipFile.getEntry("META-INF").getName()).isEqualTo("META-INF/");
			assertThat(this.zipContent.getEntry("META-INF").getName()).isEqualTo("META-INF/");
			assertThat(zipFile.getEntry("META-INF/").getName()).isEqualTo("META-INF/");
			assertThat(this.zipContent.getEntry("META-INF/").getName()).isEqualTo("META-INF/");
			assertThat(zipFile.getEntry("d/9.dat").getName()).isEqualTo("d/9.dat");
			assertThat(this.zipContent.getEntry("d/9.dat").getName()).isEqualTo("d/9.dat");
			assertThat(zipFile.getEntry("d/9.dat/")).isNull();
			assertThat(this.zipContent.getEntry("d/9.dat/")).isNull();
		}
	}

	@Test
	void getManifestEntry() throws Exception {
		Entry entry = this.zipContent.getEntry("META-INF/MANIFEST.MF");
		try (CloseableDataBlock dataBlock = entry.openContent()) {
			Manifest manifest = new Manifest(asInflaterInputStream(dataBlock));
			assertThat(manifest.getMainAttributes().getValue("Built-By")).isEqualTo("j1");
		}
	}

	@Test
	void getEntryAsCreatesCompatibleEntries() throws IOException {
		try (ZipFile zipFile = new ZipFile(this.file)) {
			Iterator<? extends ZipEntry> expected = zipFile.entries().asIterator();
			Iterator<Entry> actual = this.zipContent.iterator();
			while (expected.hasNext()) {
				assertThatFieldsAreEqual(actual.next().as(ZipEntry::new), expected.next());
			}
		}
	}

	private void assertThatFieldsAreEqual(ZipEntry actual, ZipEntry expected) {
		assertThat(actual.getName()).isEqualTo(expected.getName());
		assertThat(actual.getTime()).isEqualTo(expected.getTime());
		assertThat(actual.getLastModifiedTime()).isEqualTo(expected.getLastModifiedTime());
		assertThat(actual.getLastAccessTime()).isEqualTo(expected.getLastAccessTime());
		assertThat(actual.getCreationTime()).isEqualTo(expected.getCreationTime());
		assertThat(actual.getSize()).isEqualTo(expected.getSize());
		assertThat(actual.getCompressedSize()).isEqualTo(expected.getCompressedSize());
		assertThat(actual.getCrc()).isEqualTo(expected.getCrc());
		assertThat(actual.getMethod()).isEqualTo(expected.getMethod());
		assertThat(actual.getExtra()).isEqualTo(expected.getExtra());
		assertThat(actual.getComment()).isEqualTo(expected.getComment());
	}

	@Test
	void sizeReturnsNumberOfEntries() {
		assertThat(this.zipContent.size()).isEqualTo(12);
	}

	@Test
	void sizeWhenClosedThrowsException() throws IOException {
		this.zipContent.close();
		assertThatIllegalStateException().isThrownBy(() -> this.zipContent.size())
			.withMessage("Zip content has been closed");
	}

	@Test
	void nestedJarFileReturnsNestedaJar() throws IOException {
		try (ZipContent nested = ZipContent.open(this.file.toPath(), "nested.jar")) {
			assertThat(nested.size()).isEqualTo(5);
			assertThat(nested.getComment()).isEqualTo("nested");
			Iterator<Entry> iterator = nested.iterator();
			assertThat(iterator.next().getName()).isEqualTo("META-INF/");
			assertThat(iterator.next().getName()).isEqualTo("META-INF/MANIFEST.MF");
			assertThat(iterator.next().getName()).isEqualTo("3.dat");
			assertThat(iterator.next().getName()).isEqualTo("4.dat");
			assertThat(iterator.next().getName()).isEqualTo("\u00E4.dat");
			assertThat(iterator.hasNext()).isFalse();
		}
	}

	@Test
	void nestedJarDirectoryReturnsNestedJar() {
		fail("FIXME");
	}

	@Test
	void loadWhenHasFrontMatterOpensZip() throws IOException {
		File fileWithFrontMatter = new File(this.tempDir, "withfrontmatter.jar");
		FileOutputStream outputStream = new FileOutputStream(fileWithFrontMatter);
		StreamUtils.copy("#/bin/bash", Charset.defaultCharset(), outputStream);
		FileCopyUtils.copy(new FileInputStream(this.file), outputStream);
		try (ZipContent zipWithFrontMatter = ZipContent.open(fileWithFrontMatter.toPath())) {
			assertHasExpectedEntries(zipWithFrontMatter.iterator());
		}
	}

	@Test
	@Disabled
	void openWhenZip64ThatExceedsZipEntryLimitOpensZip() throws Exception {
		File zip64File = new File(this.tempDir, "zip64.zip");
		FileCopyUtils.copy(zip64Bytes(), zip64File);
		try (ZipContent zip64Content = ZipContent.open(zip64File.toPath())) {
			List<Entry> entries = zip64Content.stream().toList();
			assertThat(entries).hasSize(65537);
			for (int i = 0; i < entries.size(); i++) {
				Entry entry = entries.get(i);
				try (CloseableDataBlock dataBlock = entry.openContent()) {
					assertThat(asInflaterInputStream(dataBlock)).hasContent("Entry " + (i + 1));
				}
			}
		}
	}

	@Test
	@Disabled
	void openWhenZip64ThatExceedsZipSizeLimitOpensZip() throws Exception {
		Assumptions.assumeTrue(this.tempDir.getFreeSpace() > 6 * 1024 * 1024 * 1024, "Insufficient disk space");
		File zip64File = new File(this.tempDir, "zip64.zip");
		File entryFile = new File(this.tempDir, "entry.dat");
		CRC32 crc32 = new CRC32();
		try (FileOutputStream entryOut = new FileOutputStream(entryFile)) {
			byte[] data = new byte[1024 * 1024];
			new Random().nextBytes(data);
			for (int i = 0; i < 1024; i++) {
				entryOut.write(data);
				crc32.update(data);
			}
		}
		try (ZipOutputStream zipOutput = new ZipOutputStream(new FileOutputStream(zip64File))) {
			for (int i = 0; i < 6; i++) {
				ZipEntry storedEntry = new ZipEntry("huge-" + i);
				storedEntry.setSize(entryFile.length());
				storedEntry.setCompressedSize(entryFile.length());
				storedEntry.setCrc(crc32.getValue());
				storedEntry.setMethod(ZipEntry.STORED);
				zipOutput.putNextEntry(storedEntry);
				try (FileInputStream entryIn = new FileInputStream(entryFile)) {
					StreamUtils.copy(entryIn, zipOutput);
				}
				zipOutput.closeEntry();
			}
		}
		try (ZipContent zip64Content = ZipContent.open(zip64File.toPath())) {
			assertThat(zip64Content.stream()).hasSize(6);
		}
	}

	@Test
	@Disabled
	void nestedZip64CanBeRead() throws Exception {
		File containerFile = new File(this.tempDir, "outer.zip");
		try (ZipOutputStream jarOutput = new ZipOutputStream(new FileOutputStream(containerFile))) {
			ZipEntry nestedEntry = new ZipEntry("nested-zip64.zip");
			byte[] contents = zip64Bytes();
			nestedEntry.setSize(contents.length);
			nestedEntry.setCompressedSize(contents.length);
			CRC32 crc32 = new CRC32();
			crc32.update(contents);
			nestedEntry.setCrc(crc32.getValue());
			nestedEntry.setMethod(ZipEntry.STORED);
			jarOutput.putNextEntry(nestedEntry);
			jarOutput.write(contents);
			jarOutput.closeEntry();
		}
		try (ZipContent nestedZip = ZipContent.open(containerFile.toPath(), "nested-zip64.zip")) {
			List<Entry> entries = nestedZip.stream().toList();
			assertThat(entries).hasSize(65537);
			for (int i = 0; i < entries.size(); i++) {
				Entry entry = entries.get(i);
				try (CloseableDataBlock content = entry.openContent()) {
					assertThat(asInflaterInputStream(content)).hasContent("Entry " + (i + 1));
				}
			}
		}
	}

	private byte[] zip64Bytes() throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ZipOutputStream zipOutput = new ZipOutputStream(bytes);
		for (int i = 0; i < 65537; i++) {
			zipOutput.putNextEntry(new ZipEntry(i + ".dat"));
			zipOutput.write(("Entry " + (i + 1)).getBytes(StandardCharsets.UTF_8));
			zipOutput.closeEntry();
		}
		zipOutput.close();
		return bytes.toByteArray();
	}

	// @formatter:off
//
//	@Test
//	void jarFileEntryWithEpochTimeOfZeroShouldNotFail() throws Exception {
//		File file = createJarFileWithEpochTimeOfZero();
//		try (JarFile jar = new JarFile(file)) {
//			Enumeration<java.util.jar.JarEntry> entries = jar.entries();
//			JarEntry entry = entries.nextElement();
//			assertThat(entry.getLastModifiedTime().toInstant()).isEqualTo(Instant.EPOCH);
//			assertThat(entry.getName()).isEqualTo("1.dat");
//		}
//	}
//
//	private File createJarFileWithEpochTimeOfZero() throws Exception {
//		File jarFile = new File(this.tempDir, "temp.jar");
//		FileOutputStream fileOutputStream = new FileOutputStream(jarFile);
//		String comment = "outer";
//		try (JarOutputStream jarOutputStream = new JarOutputStream(fileOutputStream)) {
//			jarOutputStream.setComment(comment);
//			JarEntry entry = new JarEntry("1.dat");
//			entry.setLastModifiedTime(FileTime.from(Instant.EPOCH));
//			jarOutputStream.putNextEntry(entry);
//			jarOutputStream.write(new byte[] { (byte) 1 });
//			jarOutputStream.closeEntry();
//		}
//
//		byte[] data = Files.readAllBytes(jarFile.toPath());
//		int headerPosition = data.length - ZipFile.ENDHDR - comment.getBytes().length;
//		int centralHeaderPosition = (int) Bytes.littleEndianValue(data, headerPosition + ZipFile.ENDOFF, 1);
//		int localHeaderPosition = (int) Bytes.littleEndianValue(data, centralHeaderPosition + ZipFile.CENOFF, 1);
//		writeTimeBlock(data, centralHeaderPosition + ZipFile.CENTIM, 0);
//		writeTimeBlock(data, localHeaderPosition + ZipFile.LOCTIM, 0);
//
//		File jar = new File(this.tempDir, "zerotimed.jar");
//		Files.write(jar.toPath(), data);
//		return jar;
//	}
//
//	private static void writeTimeBlock(byte[] data, int pos, int value) {
//		data[pos] = (byte) (value & 0xff);
//		data[pos + 1] = (byte) ((value >> 8) & 0xff);
//		data[pos + 2] = (byte) ((value >> 16) & 0xff);
//		data[pos + 3] = (byte) ((value >> 24) & 0xff);
//	}
	// @formatter:on

	private void assertHasExpectedEntries(Iterator<Entry> entries) {
		assertThat(entries.next().getName()).isEqualTo("META-INF/");
		assertThat(entries.next().getName()).isEqualTo("META-INF/MANIFEST.MF");
		assertThat(entries.next().getName()).isEqualTo("1.dat");
		assertThat(entries.next().getName()).isEqualTo("2.dat");
		assertThat(entries.next().getName()).isEqualTo("d/");
		assertThat(entries.next().getName()).isEqualTo("d/9.dat");
		assertThat(entries.next().getName()).isEqualTo("special/");
		assertThat(entries.next().getName()).isEqualTo("special/\u00EB.dat");
		assertThat(entries.next().getName()).isEqualTo("nested.jar");
		assertThat(entries.next().getName()).isEqualTo("another-nested.jar");
		assertThat(entries.next().getName()).isEqualTo("space nested.jar");
		assertThat(entries.next().getName()).isEqualTo("multi-release.jar");
		assertThat(entries.hasNext()).isFalse();
	}

	private InputStream asInflaterInputStream(DataBlock dataBlock) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate((int) dataBlock.size() + 1);
		buffer.limit(buffer.limit() - 1);
		dataBlock.readFully(buffer, 0);
		ByteArrayInputStream in = new ByteArrayInputStream(buffer.array());
		return new InflaterInputStream(in, new Inflater(true));
	}

}
