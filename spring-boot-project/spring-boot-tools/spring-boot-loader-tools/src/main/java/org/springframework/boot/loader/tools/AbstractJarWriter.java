/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.zip.UnixStat;

/**
 * Abstract base class for Jar writers.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public abstract class AbstractJarWriter implements LoaderClassesWriter {

	private static final UnpackHandler NEVER_UNPACK = new NeverUnpackHandler();

	private static final String NESTED_LOADER_JAR = "META-INF/loader/spring-boot-loader.jar";

	private static final int BUFFER_SIZE = 32 * 1024;

	private final Set<String> writtenEntries = new HashSet<>();

	AbstractJarWriter() {
	}

	/**
	 * Write the specified manifest.
	 * @param manifest the manifest to write
	 * @throws IOException of the manifest cannot be written
	 */
	public void writeManifest(Manifest manifest) throws IOException {
		JarArchiveEntry entry = new JarArchiveEntry("META-INF/MANIFEST.MF");
		writeEntry(entry, manifest::write, NEVER_UNPACK);
	}

	/**
	 * Write all entries from the specified jar file.
	 * @param jarFile the source jar file
	 * @throws IOException if the entries cannot be written
	 */
	public void writeEntries(JarFile jarFile) throws IOException {
		this.writeEntries(jarFile, new IdentityEntryTransformer(), NEVER_UNPACK);
	}

	void writeEntries(JarFile jarFile, UnpackHandler unpackHandler) throws IOException {
		this.writeEntries(jarFile, new IdentityEntryTransformer(), unpackHandler);
	}

	void writeEntries(JarFile jarFile, EntryTransformer entryTransformer, UnpackHandler unpackHandler)
			throws IOException {
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarArchiveEntry entry = new JarArchiveEntry(entries.nextElement());
			setUpEntry(jarFile, entry);
			try (ZipHeaderPeekInputStream inputStream = new ZipHeaderPeekInputStream(jarFile.getInputStream(entry))) {
				EntryWriter entryWriter = new InputStreamEntryWriter(inputStream);
				JarArchiveEntry transformedEntry = entryTransformer.transform(entry);
				if (transformedEntry != null) {
					writeEntry(transformedEntry, entryWriter, unpackHandler);
				}
			}
		}
	}

	private void setUpEntry(JarFile jarFile, JarArchiveEntry entry) throws IOException {
		try (ZipHeaderPeekInputStream inputStream = new ZipHeaderPeekInputStream(jarFile.getInputStream(entry))) {
			if (inputStream.hasZipHeader() && entry.getMethod() != ZipEntry.STORED) {
				new CrcAndSize(inputStream).setupStoredEntry(entry);
			}
			else {
				entry.setCompressedSize(-1);
			}
		}
	}

	/**
	 * Writes an entry. The {@code inputStream} is closed once the entry has been written
	 * @param entryName the name of the entry
	 * @param inputStream the stream from which the entry's data can be read
	 * @throws IOException if the write fails
	 */
	@Override
	public void writeEntry(String entryName, InputStream inputStream) throws IOException {
		JarArchiveEntry entry = new JarArchiveEntry(entryName);
		try {
			writeEntry(entry, new InputStreamEntryWriter(inputStream), NEVER_UNPACK);
		}
		finally {
			inputStream.close();
		}
	}

	/**
	 * Write a nested library.
	 * @param destination the destination of the library
	 * @param library the library
	 * @throws IOException if the write fails
	 */
	public void writeNestedLibrary(String destination, Library library) throws IOException {
		File file = library.getFile();
		JarArchiveEntry entry = new JarArchiveEntry(destination + library.getName());
		entry.setTime(getNestedLibraryTime(file));
		new CrcAndSize(file).setupStoredEntry(entry);
		try (FileInputStream input = new FileInputStream(file)) {
			writeEntry(entry, new InputStreamEntryWriter(input), new LibraryUnpackHandler(library));
		}
	}

	private long getNestedLibraryTime(File file) {
		try {
			try (JarFile jarFile = new JarFile(file)) {
				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					if (!entry.isDirectory()) {
						return entry.getTime();
					}
				}
			}
		}
		catch (Exception ex) {
			// Ignore and just use the source file timestamp
		}
		return file.lastModified();
	}

	/**
	 * Write the required spring-boot-loader classes to the JAR.
	 * @throws IOException if the classes cannot be written
	 */
	@Override
	public void writeLoaderClasses() throws IOException {
		writeLoaderClasses(NESTED_LOADER_JAR);
	}

	/**
	 * Write the required spring-boot-loader classes to the JAR.
	 * @param loaderJarResourceName the name of the resource containing the loader classes
	 * to be written
	 * @throws IOException if the classes cannot be written
	 */
	@Override
	public void writeLoaderClasses(String loaderJarResourceName) throws IOException {
		URL loaderJar = getClass().getClassLoader().getResource(loaderJarResourceName);
		try (JarInputStream inputStream = new JarInputStream(new BufferedInputStream(loaderJar.openStream()))) {
			JarEntry entry;
			while ((entry = inputStream.getNextJarEntry()) != null) {
				if (entry.getName().endsWith(".class")) {
					writeEntry(new JarArchiveEntry(entry), new InputStreamEntryWriter(inputStream), NEVER_UNPACK);
				}
			}
		}
	}

	/**
	 * Perform the actual write of a {@link JarEntry}. All other write methods delegate to
	 * this one.
	 * @param entry the entry to write
	 * @param entryWriter the entry writer or {@code null} if there is no content
	 * @param unpackHandler handles possible unpacking for the entry
	 * @throws IOException in case of I/O errors
	 */
	private void writeEntry(JarArchiveEntry entry, EntryWriter entryWriter, UnpackHandler unpackHandler)
			throws IOException {
		String parent = entry.getName();
		if (parent.endsWith("/")) {
			parent = parent.substring(0, parent.length() - 1);
			entry.setUnixMode(UnixStat.DIR_FLAG | UnixStat.DEFAULT_DIR_PERM);
		}
		else {
			entry.setUnixMode(UnixStat.FILE_FLAG | UnixStat.DEFAULT_FILE_PERM);
		}
		if (parent.lastIndexOf('/') != -1) {
			parent = parent.substring(0, parent.lastIndexOf('/') + 1);
			if (!parent.isEmpty()) {
				writeEntry(new JarArchiveEntry(parent), null, unpackHandler);
			}
		}
		if (this.writtenEntries.add(entry.getName())) {
			entryWriter = addUnpackCommentIfNecessary(entry, entryWriter, unpackHandler);
			writeEntry(entry, entryWriter);
		}
	}

	protected abstract void writeEntry(JarArchiveEntry entry, EntryWriter entryWriter) throws IOException;

	private EntryWriter addUnpackCommentIfNecessary(JarArchiveEntry entry, EntryWriter entryWriter,
			UnpackHandler unpackHandler) throws IOException {
		if (entryWriter == null || !unpackHandler.requiresUnpack(entry.getName())) {
			return entryWriter;
		}
		// FIXME eats too much memory?
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		entryWriter.write(output);
		entry.setComment("UNPACK:" + unpackHandler.sha1Hash(entry.getName()));
		return new InputStreamEntryWriter(new ByteArrayInputStream(output.toByteArray()));
	}

	/**
	 * {@link EntryWriter} that writes content from an {@link InputStream}.
	 */
	private static class InputStreamEntryWriter implements EntryWriter {

		private final InputStream inputStream;

		InputStreamEntryWriter(InputStream inputStream) {
			this.inputStream = inputStream;
		}

		@Override
		public void write(OutputStream outputStream) throws IOException {
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead;
			while ((bytesRead = this.inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}
			outputStream.flush();
		}

	}

	/**
	 * Data holder for CRC and Size.
	 */
	private static class CrcAndSize {

		private final CRC32 crc = new CRC32();

		private long size;

		CrcAndSize(File file) throws IOException {
			try (FileInputStream inputStream = new FileInputStream(file)) {
				load(inputStream);
			}
		}

		CrcAndSize(InputStream inputStream) throws IOException {
			load(inputStream);
		}

		private void load(InputStream inputStream) throws IOException {
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				this.crc.update(buffer, 0, bytesRead);
				this.size += bytesRead;
			}
		}

		void setupStoredEntry(JarArchiveEntry entry) {
			entry.setSize(this.size);
			entry.setCompressedSize(this.size);
			entry.setCrc(this.crc.getValue());
			entry.setMethod(ZipEntry.STORED);
		}

	}

	/**
	 * An {@code EntryTransformer} enables the transformation of {@link JarEntry jar
	 * entries} during the writing process.
	 */
	interface EntryTransformer {

		JarArchiveEntry transform(JarArchiveEntry jarEntry);

	}

	/**
	 * An {@code EntryTransformer} that returns the entry unchanged.
	 */
	private static final class IdentityEntryTransformer implements EntryTransformer {

		@Override
		public JarArchiveEntry transform(JarArchiveEntry jarEntry) {
			return jarEntry;
		}

	}

	/**
	 * An {@code UnpackHandler} determines whether or not unpacking is required and
	 * provides a SHA1 hash if required.
	 */
	interface UnpackHandler {

		boolean requiresUnpack(String name);

		String sha1Hash(String name) throws IOException;

	}

	private static final class NeverUnpackHandler implements UnpackHandler {

		@Override
		public boolean requiresUnpack(String name) {
			return false;
		}

		@Override
		public String sha1Hash(String name) {
			throw new UnsupportedOperationException();
		}

	}

	private static final class LibraryUnpackHandler implements UnpackHandler {

		private final Library library;

		private LibraryUnpackHandler(Library library) {
			this.library = library;
		}

		@Override
		public boolean requiresUnpack(String name) {
			return this.library.isUnpackRequired();
		}

		@Override
		public String sha1Hash(String name) throws IOException {
			return FileUtils.sha1Hash(this.library.getFile());
		}

	}

}
