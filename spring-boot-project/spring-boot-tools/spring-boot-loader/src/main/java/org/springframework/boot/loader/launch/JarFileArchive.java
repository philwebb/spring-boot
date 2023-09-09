/*
 * Copyright 2012-2022 the original author or authors.
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.springframework.boot.loader.jar.NestedJarFile;
import org.springframework.boot.loader.net.protocol.jar.Handler;

/**
 * {@link Archive} implementation backed by a {@link JarFile}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 3.0.0
 */
public class JarFileArchive implements Archive {

	// FIXME Implement close check it's called

	private static final URLStreamHandler JAR_HANDLER = new Handler();

	private static final String UNPACK_MARKER = "UNPACK:";

	private static final FileAttribute<?>[] NO_FILE_ATTRIBUTES = {};

	private static final EnumSet<PosixFilePermission> DIRECTORY_PERMISSIONS = EnumSet.of(PosixFilePermission.OWNER_READ,
			PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);

	private static final EnumSet<PosixFilePermission> FILE_PERMISSIONS = EnumSet.of(PosixFilePermission.OWNER_READ,
			PosixFilePermission.OWNER_WRITE);

	private static final Predicate<Entry> ALL_ENTRIES = (entry) -> true;

	private final File file;

	private final JarFile jarFile;

	private final URL url;

	private Path tempUnpackDirectory;

	public JarFileArchive(File file) throws IOException {
		this(file, new JarFile(file), getUrl(file, null));
	}

	private JarFileArchive(File file, JarFile jarFile, URL url) {
		this.file = file;
		this.jarFile = jarFile;
		this.url = url;
	}

	@Override
	public URL getUrl() {
		return this.url;
	}

	@Override
	public Manifest getManifest() throws IOException {
		return this.jarFile.getManifest();
	}

	@Override
	public Iterator<Archive> getNestedArchives(Predicate<Entry> searchFilter, Predicate<Entry> includeFilter) {
		return this.jarFile.stream()
			.map(JarArchiveEntry::new)
			.filter(searchFilter != null ? searchFilter : ALL_ENTRIES)
			.filter(includeFilter != null ? includeFilter : ALL_ENTRIES)
			.map(this::getNestedArchive)
			.iterator();
	}

	@Override
	public void close() throws IOException {
		this.jarFile.close();
	}

	protected Archive getNestedArchive(JarArchiveEntry entry) {
		try {
			JarEntry jarEntry = entry.getJarEntry();
			String comment = jarEntry.getComment();
			if (comment != null && comment.startsWith(UNPACK_MARKER)) {
				return getUnpackedNestedArchive(jarEntry);
			}
			JarFile jarFile = new NestedJarFile(this.file, entry.getName());
			return new JarFileArchive(this.file, jarFile, getUrl(this.file, jarEntry));
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private Archive getUnpackedNestedArchive(JarEntry jarEntry) throws IOException {
		String name = jarEntry.getName();
		if (name.lastIndexOf('/') != -1) {
			name = name.substring(name.lastIndexOf('/') + 1);
		}
		Path path = getTempUnpackDirectory().resolve(name);
		if (!Files.exists(path) || Files.size(path) != jarEntry.getSize()) {
			unpack(jarEntry, path);
		}
		return new JarFileArchive(path.toFile());
	}

	private Path getTempUnpackDirectory() {
		if (this.tempUnpackDirectory == null) {
			Path tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"));
			this.tempUnpackDirectory = createUnpackDirectory(tempDirectory);
		}
		return this.tempUnpackDirectory;
	}

	private Path createUnpackDirectory(Path parent) {
		int attempts = 0;
		while (attempts++ < 1000) {
			String fileName = Paths.get(this.jarFile.getName()).getFileName().toString();
			Path unpackDirectory = parent.resolve(fileName + "-spring-boot-libs-" + UUID.randomUUID());
			try {
				createDirectory(unpackDirectory);
				return unpackDirectory;
			}
			catch (IOException ex) {
			}
		}
		throw new IllegalStateException("Failed to create unpack directory in directory '" + parent + "'");
	}

	private void createDirectory(Path path) throws IOException {
		Files.createDirectory(path, getFileAttributes(path.getFileSystem(), DIRECTORY_PERMISSIONS));
	}

	private void unpack(JarEntry entry, Path path) throws IOException {
		createFile(path);
		path.toFile().deleteOnExit();
		try (InputStream inputStream = this.jarFile.getInputStream(entry);
				OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.WRITE,
						StandardOpenOption.TRUNCATE_EXISTING)) {
			inputStream.transferTo(outputStream);
		}
	}

	private void createFile(Path path) throws IOException {
		Files.createFile(path, getFileAttributes(path.getFileSystem(), FILE_PERMISSIONS));
	}

	private FileAttribute<?>[] getFileAttributes(FileSystem fileSystem, Set<PosixFilePermission> permissions) {
		return (!supportsPosix(fileSystem)) ? NO_FILE_ATTRIBUTES
				: new FileAttribute<?>[] { PosixFilePermissions.asFileAttribute(permissions) };
	}

	private boolean supportsPosix(FileSystem fileSystem) {
		return fileSystem.supportedFileAttributeViews().contains("posix");
	}

	@Override
	public String toString() {
		return getUrl().toString();
	}

	private static URL getUrl(File file, JarEntry nestedEntry) {
		try {
			String filePath = file.toURI().getPath();
			String path = (nestedEntry != null) ? "nested:" + filePath + "!" + nestedEntry.getName()
					: "file:" + filePath;
			return new URL(null, "jar:" + path + "!/", JAR_HANDLER);
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException("Unable to create JarFileArchive URL", ex);
		}
	}

	/**
	 * {@link Archive.Entry} implementation backed by a {@link JarEntry}.
	 */
	private static class JarArchiveEntry implements Entry {

		private final JarEntry jarEntry;

		JarArchiveEntry(JarEntry jarEntry) {
			this.jarEntry = jarEntry;
		}

		JarEntry getJarEntry() {
			return this.jarEntry;
		}

		@Override
		public String getName() {
			return this.jarEntry.getName();
		}

		@Override
		public boolean isDirectory() {
			return this.jarEntry.isDirectory();
		}

	}

}
