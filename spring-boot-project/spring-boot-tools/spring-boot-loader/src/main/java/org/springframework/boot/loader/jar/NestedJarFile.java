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
import java.nio.ByteBuffer;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.springframework.boot.loader.ref.Cleaner;
import org.springframework.boot.loader.zip.CloseableDataBlock;
import org.springframework.boot.loader.zip.ZipContent;

/**
 * Extended variant of {@link JarFile} that behaves in the same way but can open nested
 * jars.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 3.2.0
 */
public class NestedJarFile extends JarFile {

	private static final int DECIMAL = 10;

	private static final String META_INF = "META-INF/";

	static final String META_INF_VERSIONS = META_INF + "versions/";

	static final int BASE_VERSION = baseVersion().feature();

	private final Cleaner cleaner;

	private final NestedJarFileResources resources;

	private final Cleanable cleanup;

	private final String name;

	private final int version;

	private String lastEntryPrefix;

	private String lastEntryName;

	private ZipContent.Entry lastContentEntry;

	private volatile boolean closing;

	/**
	 * Creates a new {@link NestedJarFile} instance to read from the specific
	 * {@code File}.
	 * @param file the jar file to be opened for reading
	 * @throws IOException on I/O error
	 */
	NestedJarFile(File file) throws IOException {
		this(file, null, null, false, null);
	}

	/**
	 * Creates a new {@link NestedJarFile} instance to read from the specific
	 * {@code File}.
	 * @param file the jar file to be opened for reading
	 * @param nestedEntryName the nested entry name to open or {@code null}
	 * @throws IOException on I/O error
	 */
	public NestedJarFile(File file, String nestedEntryName) throws IOException {
		this(file, nestedEntryName, null, true, null);
	}

	/**
	 * Creates a new {@link NestedJarFile} instance to read from the specific
	 * {@code File}.
	 * @param file the jar file to be opened for reading
	 * @param nestedEntryName the nested entry name to open or {@code null}
	 * @param version the release version to use when opening a multi-release jar
	 * @throws IOException on I/O error
	 */
	public NestedJarFile(File file, String nestedEntryName, Runtime.Version version) throws IOException {
		this(file, nestedEntryName, version, true, null);
	}

	/**
	 * Creates a new {@link NestedJarFile} instance to read from the specific
	 * {@code File}.
	 * @param file the jar file to be opened for reading
	 * @param nestedEntryName the nested entry name to open or {@code null}
	 * @param version the release version to use when opening a multi-release jar
	 * @param onlyNestedJars if <em>only</em> nested jars should be opened
	 * @param cleaner the cleaner used to release resources
	 * @throws IOException on I/O error
	 */
	NestedJarFile(File file, String nestedEntryName, Runtime.Version version, boolean onlyNestedJars, Cleaner cleaner)
			throws IOException {
		super(file);
		if (onlyNestedJars && (nestedEntryName == null || nestedEntryName.isEmpty())) {
			throw new IllegalArgumentException("nestedEntryName must not be empty");
		}
		this.cleaner = (cleaner != null) ? cleaner : Cleaner.instance;
		this.resources = new NestedJarFileResources(file, nestedEntryName);
		this.cleanup = this.cleaner.register(this, this.resources);
		this.name = file.getPath() + ((nestedEntryName != null) ? "[" + nestedEntryName + "]" : "");
		this.version = (version != null) ? version.feature() : baseVersion().feature();
	}

	@Override
	public Manifest getManifest() throws IOException {
		try {
			return this.resources.zipContent().getInfo(ManifestInfo.class, this::getManifestInfo).getManifest();
		}
		catch (UncheckedIOException ex) {
			throw ex.getCause();
		}
	}

	@Override
	public Enumeration<JarEntry> entries() {
		synchronized (this) {
			ensureOpen();
			return new JarEntriesEnumeration(this.resources.zipContent().iterator());
		}
	}

	@Override
	public Stream<JarEntry> stream() {
		synchronized (this) {
			ensureOpen();
			return this.resources.zipContent().stream().map((entry) -> entry.as(NestedJarEntry::new));
		}
	}

	@Override
	public Stream<JarEntry> versionedStream() {
		synchronized (this) {
			ensureOpen();
			return this.resources.zipContent()
				.stream()
				.map(this::asVersionedEntry)
				.filter(nonNullDistinct(JarEntry::getName));
		}
	}

	private JarEntry asVersionedEntry(ZipContent.Entry contentEntry) {
		String name = contentEntry.getName();
		if (!name.startsWith(META_INF_VERSIONS)) {
			return contentEntry.as(NestedJarEntry::new);
		}
		int versionNumberStartIndex = META_INF_VERSIONS.length();
		int versionNumberEndIndex = name.indexOf('/', versionNumberStartIndex);
		if (versionNumberEndIndex == -1 && versionNumberEndIndex == (name.length() - 1)) {
			return null;
		}
		try {
			int versionNumber = Integer.parseInt(name, versionNumberStartIndex, versionNumberEndIndex, DECIMAL);
			if (versionNumber > this.version) {
				return null;
			}
		}
		catch (NumberFormatException ex) {
			return null;
		}
		String baseName = name.substring(versionNumberEndIndex + 1);
		return contentEntry.as((realName) -> new NestedJarEntry(contentEntry, baseName));
	}

	public static <T, K> Predicate<T> nonNullDistinct(Function<T, K> extractor) {
		Set<K> seen = ConcurrentHashMap.newKeySet();
		return (entry) -> entry != null && seen.add(extractor.apply(entry));
	}

	@Override
	public NestedJarEntry getJarEntry(String name) {
		return getEntry(name);
	}

	@Override
	public NestedJarEntry getEntry(String name) {
		Objects.requireNonNull(name, "name");
		NestedJarEntry entry = getVersionedEntry(name);
		return (entry != null) ? entry : getEntry(null, name);
	}

	private NestedJarEntry getVersionedEntry(String name) {
		// NOTE: we can't call isMultiRelease() directly because it's a final method and
		// it inspects the container jar. We use ManifestInfo instead.
		ManifestInfo manifestInfo = this.resources.zipContent().getInfo(ManifestInfo.class, this::getManifestInfo);
		if (!manifestInfo.isMultiRelease() || name.startsWith(META_INF) || BASE_VERSION < this.version) {
			return null;
		}
		MetaInfVersionsInfo versionsInfo = this.resources.zipContent()
			.getInfo(MetaInfVersionsInfo.class, MetaInfVersionsInfo::get);
		int[] versions = versionsInfo.versions();
		String[] directories = versionsInfo.directories();
		for (int i = versions.length - 1; i >= 0; i--) {
			if (versions[i] <= this.version) {
				NestedJarEntry entry = getEntry(directories[i], name);
				if (entry != null) {
					return entry;
				}
			}
		}
		return null;
	}

	private NestedJarEntry getEntry(String namePrefix, String name) {
		ZipContent.Entry contentEntry = getContentEntry(namePrefix, name);
		return (contentEntry != null) ? contentEntry.as(NestedJarEntry::new) : null;
	}

	private ZipContent.Entry getContentEntry(String namePrefix, String name) {
		synchronized (this) {
			ensureOpen();
			if (Objects.equals(namePrefix, this.lastEntryPrefix) && Objects.equals(name, this.lastEntryName)) {
				return this.lastContentEntry;
			}
			ZipContent.Entry contentEntry = this.resources.zipContent().getEntry(namePrefix, name);
			this.lastEntryName = name;
			this.lastContentEntry = contentEntry;
			return contentEntry;
		}
	}

	private ManifestInfo getManifestInfo(ZipContent zipContent) {
		ZipContent.Entry contentEntry = zipContent.getEntry(MANIFEST_NAME);
		if (contentEntry == null) {
			return ManifestInfo.NONE;
		}
		try {
			try (InputStream inputStream = getInputStream(contentEntry)) {
				Manifest manifest = new Manifest(inputStream);
				return new ManifestInfo(manifest, null);
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Override
	public InputStream getInputStream(ZipEntry entry) throws IOException {
		Objects.requireNonNull(entry, "entry");
		if (!(entry instanceof NestedJarEntry)) {
			entry = getEntry(entry.getName());
		}
		return getInputStream(((NestedJarEntry) entry).contentEntry());
	}

	private InputStream getInputStream(ZipContent.Entry contentEntry) throws IOException {
		int compression = contentEntry.getCompressionMethod();
		if (compression != ZipEntry.STORED && compression != ZipEntry.DEFLATED) {
			throw new ZipException("invalid compression method");
		}
		Set<InputStream> inputStreams = this.resources.inputStreams();
		synchronized (this) {
			ensureOpen();
			InputStream inputStream = new JarEntryInputStream(contentEntry);
			try {
				if (compression == ZipEntry.DEFLATED) {
					inputStream = new JarEntryInflaterInputStream((JarEntryInputStream) inputStream, this.resources);
				}
				synchronized (inputStreams) {
					inputStreams.add(inputStream);
				}
				return inputStream;
			}
			catch (RuntimeException ex) {
				inputStream.close();
				throw ex;
			}
		}
	}

	@Override
	public String getComment() {
		synchronized (this) {
			ensureOpen();
			return this.resources.zipContent().getComment();
		}
	}

	@Override
	public int size() {
		synchronized (this) {
			ensureOpen();
			return this.resources.zipContent().size();
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
		if (this.resources.zipContent() == null) {
			throw new IllegalStateException("The object is not initialized.");
		}
	}

	/**
	 * An individual entry from a {@link NestedJarFile}.
	 */
	private class NestedJarEntry extends java.util.jar.JarEntry {

		private final ZipContent.Entry contentEntry;

		private final String name;

		NestedJarEntry(ZipContent.Entry contentEntry, String name) {
			super(contentEntry.getName());
			this.contentEntry = contentEntry;
			this.name = name;
		}

		@Override
		public String getRealName() {
			return super.getName();
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public Attributes getAttributes() throws IOException {
			Manifest manifest = getManifest();
			return (manifest != null) ? manifest.getAttributes(getName()) : null;
		}

		@Override
		public Certificate[] getCertificates() {
			return getSecurityInfo().getCertificates(contentEntry());
		}

		@Override
		public CodeSigner[] getCodeSigners() {
			return getSecurityInfo().getCodeSigners(contentEntry());
		}

		private SecurityInfo getSecurityInfo() {
			return NestedJarFile.this.resources.zipContent().getInfo(SecurityInfo.class, SecurityInfo::get);
		}

		ZipContent.Entry contentEntry() {
			return this.contentEntry;
		}

	}

	/**
	 * {@link Enumeration} of {@link NestedJarEntry} instances.
	 */
	private class JarEntriesEnumeration implements Enumeration<JarEntry> {

		private Iterator<ZipContent.Entry> iterator;

		JarEntriesEnumeration(Iterator<ZipContent.Entry> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasMoreElements() {
			return this.iterator.hasNext();
		}

		@Override
		public NestedJarEntry nextElement() {
			ZipContent.Entry next = this.iterator.next();
			return next.as(NestedJarEntry::new);
		}

	}

	/**
	 * {@link InputStream} to read jar entry content.
	 */
	private class JarEntryInputStream extends InputStream {

		private final int uncompressedSize;

		private final CloseableDataBlock content;

		private long pos;

		private long remaining;

		private volatile boolean closing;

		JarEntryInputStream(ZipContent.Entry entry) throws IOException {
			this.uncompressedSize = entry.getUncompressedSize();
			this.content = entry.openContent();
		}

		@Override
		public int read() throws IOException {
			byte[] b = new byte[1];
			return (read(b, 0, 1) == 1) ? b[0] & 0xFF : -1;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int result;
			synchronized (NestedJarFile.this) {
				ensureOpen();
				ByteBuffer dst = ByteBuffer.wrap(b, off, len);
				int count = this.content.read(dst, this.pos);
				if (count > 0) {
					this.pos += count;
					this.remaining -= count;
				}
				result = count;
			}
			if (this.remaining == 0) {
				close();
			}
			return result;
		}

		@Override
		public long skip(long n) throws IOException {
			long result;
			synchronized (NestedJarFile.this) {
				result = (n > 0) ? maxForwardSkip(n) : maxBackwardSkip(n);
				this.pos += result;
				this.remaining -= result;
			}
			if (this.remaining == 0) {
				close();
			}
			return result;
		}

		private long maxForwardSkip(long n) {
			boolean willCauseOverflow = (this.pos + n) < 0;
			return (willCauseOverflow || n > this.remaining) ? this.remaining : n;
		}

		private long maxBackwardSkip(long n) {
			return Math.max(-this.pos, n);
		}

		@Override
		public int available() throws IOException {
			return (this.remaining < Integer.MAX_VALUE) ? (int) this.remaining : Integer.MAX_VALUE;
		}

		private void ensureOpen() throws ZipException {
			if (NestedJarFile.this.closing || this.closing) {
				throw new ZipException("ZipFile closed");
			}
		}

		@Override
		public void close() throws IOException {
			if (this.closing) {
				return;
			}
			this.closing = true;
			this.content.close();
			Set<InputStream> inputStreams = NestedJarFile.this.resources.inputStreams();
			synchronized (inputStreams) {
				inputStreams.remove(this);
			}
		}

		int getUncompressedSize() {
			return this.uncompressedSize;
		}

	}

	/**
	 * {@link ZipInflaterInputStream} to read and inflate jar entry content.
	 */
	private class JarEntryInflaterInputStream extends ZipInflaterInputStream {

		private final Cleanable cleanup;

		private volatile boolean closing;

		JarEntryInflaterInputStream(JarEntryInputStream inputStream, NestedJarFileResources resources) {
			this(inputStream, resources, resources.getOrCreateInflater());
		}

		private JarEntryInflaterInputStream(JarEntryInputStream inputStream, NestedJarFileResources resources,
				Inflater inflater) {
			super(inputStream, inflater, inputStream.getUncompressedSize());
			this.cleanup = NestedJarFile.this.cleaner.register(this, resources.createInflatorCleanupAction(inflater));
		}

		@Override
		public void close() throws IOException {
			if (this.closing) {
				return;
			}
			this.closing = true;
			super.close();
			synchronized (NestedJarFile.this.resources.inputStreams()) {
				NestedJarFile.this.resources.inputStreams().remove(this);
			}
			this.cleanup.clean();
		}

	}

}
