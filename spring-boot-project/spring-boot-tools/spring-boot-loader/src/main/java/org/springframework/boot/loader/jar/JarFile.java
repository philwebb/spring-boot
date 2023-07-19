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

package org.springframework.boot.loader.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner.Cleanable;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.springframework.boot.loader.ref.Cleaner;
import org.springframework.boot.loader.zip.ZipContent;

/**
 */
public class JarFile extends java.util.jar.JarFile {

	private static final String META_INF = "META-INF/";

	private static final int BASE_VERSION = baseVersion().feature();

	private final ManagedResources managed;

	private final Cleanable cleanup;

	private final String name;

	private final int version;

	private String lastEntryPrefix;

	private String lastEntryName;

	private ZipContent.Entry lastEntry;

	private volatile boolean closing;

	/**
	 * Opens a jar file for reading.
	 * @param file
	 * @throws IOException on I/O error
	 */
	public JarFile(File file) throws IOException {
		this(file, null);
	}

	/**
	 * Opens a jar file for reading.
	 * @param file
	 * @throws IOException on I/O error
	 */
	public JarFile(File file, String nestedEntryName) throws IOException {
		this(file, nestedEntryName, baseVersion());
	}

	/**
	 * Opens a jar file for reading.
	 * @param file
	 * @throws IOException on I/O error
	 */
	public JarFile(File file, String nestedEntryName, Runtime.Version version) throws IOException {
		super(file);
		this.managed = new ManagedResources(file, nestedEntryName);
		this.cleanup = Cleaner.register(this, this.managed);
		this.name = file.getPath() + ((nestedEntryName != null) ? "[" + nestedEntryName + "]" : "");
		this.version = version.feature();
	}

	@Override
	public Manifest getManifest() throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Enumeration<JarEntry> entries() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Stream<JarEntry> stream() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Stream<JarEntry> versionedStream() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Entry getJarEntry(String name) {
		return (Entry) getEntry(name);
	}

	@Override
	public ZipEntry getEntry(String name) {
		Objects.requireNonNull(name, "name");
		Entry entry = getVersionedEntry(name);
		return (entry != null) ? entry : getContentEntry(null, name).as(Entry::new);
	}

	private Entry getVersionedEntry(String name) {
		if (!isMultiRelease() || name.startsWith(META_INF) || BASE_VERSION < this.version) {
			return null;
		}
		MetaInfVersions metaInfVersions = this.managed.zipContent()
			.getOrCompute(MetaInfVersions.class, MetaInfVersions::from);
		int[] metaInfVersionNumbers = metaInfVersions.versions();
		String[] metaInfVersionDirectories = metaInfVersions.directories();
		for (int i = metaInfVersionNumbers.length - 1; i >= 0; i--) {
			if (metaInfVersionNumbers[i] <= this.version) {
				ZipContent.Entry entry = getContentEntry(metaInfVersionDirectories[i], name);
				if (entry != null) {
					return entry.as((realName) -> new Entry(realName, name));
				}
			}
		}
		return null;
	}

	private ZipContent.Entry getContentEntry(String namePrefix, String name) {
		synchronized (this) {
			ensureOpen();
			if (Objects.equals(namePrefix, this.lastEntryPrefix) && Objects.equals(name, this.lastEntryName)) {
				return this.lastEntry;
			}
			ZipContent.Entry entry = this.managed.zipContent().getEntry(namePrefix, name);
			this.lastEntryName = name;
			this.lastEntry = entry;
			return entry;
		}
	}

	@Override
	public synchronized InputStream getInputStream(ZipEntry entry) throws IOException {
		Objects.requireNonNull(entry, "entry");
		synchronized (this) {
			// FIXME
			return null;
		}
	}

	@Override
	public String getComment() {
		synchronized (this) {
			ensureOpen();
			return this.managed.zipContent().getComment();
		}
	}

	@Override
	public int size() {
		synchronized (this) {
			ensureOpen();
			return this.managed.zipContent().size();
		}
	}

	@Override
	public void close() throws IOException {
		if (this.closing) {
			return;
		}
		this.closing = true;
		synchronized (this) {
			try {
				this.cleanup.clean();
			}
			catch (UncheckedIOException ex) {
				throw ex.getCause();
			}
		}
	}

	@Override
	public String getName() {
		return this.name;
	}

	private void ensureOpen() {
		if (this.closing) {
			throw new IllegalStateException("zip file closed");
		}
		if (this.managed.zipContent() == null) {
			throw new IllegalStateException("The object is not initialized.");
		}
	}

	/**
	 * Resources created managed and cleaned by a {@link JarFile} instance.
	 */
	private static class ManagedResources implements Runnable {

		private ZipContent zipContent;

		ManagedResources(File file, String nestedEntryName) throws IOException {
			this.zipContent = ZipContent.open(file.toPath(), nestedEntryName);
		}

		ZipContent zipContent() {
			return this.zipContent;
		}

		@Override
		public void run() {
			IOException exceptionChain = null;
			exceptionChain = releaseAllResources(exceptionChain);
			if (exceptionChain != null) {
				throw new UncheckedIOException(exceptionChain);
			}
		}

		private IOException releaseAllResources(IOException exceptionChain) {
			if (this.zipContent != null) {
				synchronized (this.zipContent) { // FIXME not sure why we sync on this
					try {
						this.zipContent.close();
					}
					catch (IOException ex) {
						if (exceptionChain != null) {
							exceptionChain.addSuppressed(ex);
							return exceptionChain;
						}
						return ex;
					}
				}
			}
			return null;
		}

	}

	class Entry extends java.util.jar.JarEntry {

		private final String name;

		Entry(String name) {
			super(name);
			this.name = name;
		}

		Entry(String realName, String name) {
			super(realName);
			this.name = name;
		}

		@Override
		public Attributes getAttributes() throws IOException {
			// FIXME
			throw new UnsupportedOperationException("Auto-generated method stub");
		}

		@Override
		public Certificate[] getCertificates() {
			// FIXME
			throw new UnsupportedOperationException("Auto-generated method stub");
		}

		@Override
		public CodeSigner[] getCodeSigners() {
			// FIXME
			throw new UnsupportedOperationException("Auto-generated method stub");
		}

		@Override
		public String getRealName() {
			return super.getName();
		}

		@Override
		public String getName() {
			return this.name;
		}

	}

	/**
	 * Versions found under {@code META-INF/versions/}.
	 */
	static class MetaInfVersions {

		private static final String META_INF_VERSIONS = META_INF + "versions/";

		private static final MetaInfVersions NONE = new MetaInfVersions(Collections.emptySet());

		private final int[] versions;

		private final String[] directories;

		MetaInfVersions(Set<Integer> versions) {
			this.versions = versions.stream().mapToInt(Integer::intValue).toArray();
			this.directories = versions.stream()
				.map((version) -> META_INF_VERSIONS + version + "/")
				.toArray(String[]::new);
		}

		int[] versions() {
			return this.versions;
		}

		String[] directories() {
			return this.directories;
		}

		static MetaInfVersions from(ZipContent zipContent) {
			Set<Integer> versions = new TreeSet<>();
			for (ZipContent.Entry entry : zipContent) {
				if (entry.hasNameStartingWith(META_INF_VERSIONS) && !entry.isDirectory()) {
					String name = entry.getName();
					String version = name.substring(META_INF_VERSIONS.length(), name.length() - 1);
					try {
						Integer versionNumber = Integer.valueOf(version);
						if (versionNumber >= BASE_VERSION) {
							versions.add(versionNumber);
						}
					}
					catch (NumberFormatException ex) {
					}
				}
			}
			return (!versions.isEmpty()) ? new MetaInfVersions(versions) : NONE;
		}

	}

}
