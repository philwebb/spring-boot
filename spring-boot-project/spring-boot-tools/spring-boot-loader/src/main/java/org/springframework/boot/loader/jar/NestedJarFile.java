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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.springframework.boot.loader.log.DebugLogger;
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

	private static final DebugLogger debug = DebugLogger.get(NestedJarFile.class);

	private final Cleaner cleaner;

	private final NestedJarFileResources resources;

	private final Cleanable cleanup;

	private final String name;

	private final int version;

	private String lastNamePrefix;

	private String lastName;

	private ZipContent.Entry lastContentEntry;

	private volatile boolean closed;

	private volatile ManifestInfo manifestInfo;

	private volatile MetaInfVersionsInfo metaInfVersionsInfo;

	/**
	 * Creates a new {@link NestedJarFile} instance to read from the specific
	 * {@code File}.
	 * @param file the jar file to be opened for reading
	 * @throws IOException on I/O error
	 */
	NestedJarFile(File file) throws IOException {
		this(file, null, null, false, Cleaner.instance);
	}

	/**
	 * Creates a new {@link NestedJarFile} instance to read from the specific
	 * {@code File}.
	 * @param file the jar file to be opened for reading
	 * @param nestedEntryName the nested entry name to open or {@code null}
	 * @throws IOException on I/O error
	 */
	public NestedJarFile(File file, String nestedEntryName) throws IOException {
		this(file, nestedEntryName, null, true, Cleaner.instance);
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
		this(file, nestedEntryName, version, true, Cleaner.instance);
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
		debug.log("Created nested jar file (%s, %s, %s)", file, nestedEntryName, version);
		this.cleaner = cleaner;
		this.resources = new NestedJarFileResources(file, nestedEntryName);
		this.cleanup = cleaner.register(this, this.resources);
		this.name = file.getPath() + ((nestedEntryName != null) ? "!/" + nestedEntryName : "");
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
			return new JarEntriesEnumeration(this.resources.zipContent());
		}
	}

	@Override
	public Stream<JarEntry> stream() {
		synchronized (this) {
			ensureOpen();
			return streamContentEntries().map((contentEntry) -> contentEntry.as(NestedJarEntry::new));
		}
	}

	@Override
	public Stream<JarEntry> versionedStream() {
		synchronized (this) {
			ensureOpen();
			return streamContentEntries().map(this::getBaseName)
				.filter(Objects::nonNull)
				.distinct()
				.map(this::getJarEntry)
				.filter(Objects::nonNull);
		}
	}

	private Stream<ZipContent.Entry> streamContentEntries() {
		ZipContentEntriesSpliterator spliterator = new ZipContentEntriesSpliterator(this.resources.zipContent());
		return StreamSupport.stream(spliterator, false);
	}

	private String getBaseName(ZipContent.Entry contentEntry) {
		String name = contentEntry.getName();
		if (!name.startsWith(META_INF_VERSIONS)) {
			return name;
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
		return name.substring(versionNumberEndIndex + 1);
	}

	@Override
	public JarEntry getJarEntry(String name) {
		return getNestedJarEntry(name);
	}

	@Override
	public JarEntry getEntry(String name) {
		return getNestedJarEntry(name);
	}

	private NestedJarEntry getNestedJarEntry(String name) {
		Objects.requireNonNull(name, "name");
		ZipContent.Entry entry = getVersionedContentEntry(name);
		entry = (entry != null) ? entry : getContentEntry(null, name);
		return (entry != null) ? entry.as((realEntry, realName) -> new NestedJarEntry(realEntry, name)) : null;
	}

	private ZipContent.Entry getVersionedContentEntry(String name) {
		// NOTE: we can't call isMultiRelease() directly because it's a final method and
		// it inspects the container jar. We use ManifestInfo instead.
		if (!getManifestInfo().isMultiRelease() || name.startsWith(META_INF) || BASE_VERSION >= this.version) {
			return null;
		}
		MetaInfVersionsInfo metaInfVersionsInfo = getMetaInfVersionsInfo();
		int[] versions = metaInfVersionsInfo.versions();
		String[] directories = metaInfVersionsInfo.directories();
		for (int i = versions.length - 1; i >= 0; i--) {
			if (versions[i] <= this.version) {
				ZipContent.Entry entry = getContentEntry(directories[i], name);
				if (entry != null) {
					return entry;
				}
			}
		}
		return null;
	}

	private ZipContent.Entry getContentEntry(String namePrefix, String name) {
		synchronized (this) {
			ensureOpen();
			if (Objects.equals(namePrefix, this.lastNamePrefix) && Objects.equals(name, this.lastName)) {
				return this.lastContentEntry;
			}
			ZipContent.Entry contentEntry = this.resources.zipContent().getEntry(namePrefix, name);
			this.lastNamePrefix = namePrefix;
			this.lastName = name;
			this.lastContentEntry = contentEntry;
			return contentEntry;
		}
	}

	private ManifestInfo getManifestInfo() {
		ManifestInfo manifestInfo = this.manifestInfo;
		if (manifestInfo != null) {
			return manifestInfo;
		}
		synchronized (this) {
			ensureOpen();
			manifestInfo = this.resources.zipContent().getInfo(ManifestInfo.class, this::getManifestInfo);
		}
		this.manifestInfo = manifestInfo;
		return manifestInfo;
	}

	private ManifestInfo getManifestInfo(ZipContent zipContent) {
		ZipContent.Entry contentEntry = zipContent.getEntry(MANIFEST_NAME);
		if (contentEntry == null) {
			return ManifestInfo.NONE;
		}
		try {
			try (InputStream inputStream = getInputStream(contentEntry)) {
				Manifest manifest = new Manifest(inputStream);
				return new ManifestInfo(manifest);
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private MetaInfVersionsInfo getMetaInfVersionsInfo() {
		MetaInfVersionsInfo metaInfVersionsInfo = this.metaInfVersionsInfo;
		if (metaInfVersionsInfo != null) {
			return metaInfVersionsInfo;
		}
		synchronized (this) {
			ensureOpen();
			metaInfVersionsInfo = this.resources.zipContent()
				.getInfo(MetaInfVersionsInfo.class, MetaInfVersionsInfo::get);
		}
		this.metaInfVersionsInfo = metaInfVersionsInfo;
		return metaInfVersionsInfo;
	}

	@Override
	public InputStream getInputStream(ZipEntry entry) throws IOException {
		Objects.requireNonNull(entry, "entry");
		if (entry instanceof NestedJarEntry nestedJarEntry && nestedJarEntry.isOwnedBy(this)) {
			return getInputStream(nestedJarEntry.contentEntry());
		}
		return getInputStream(getNestedJarEntry(entry.getName()).contentEntry());
	}

	private InputStream getInputStream(ZipContent.Entry contentEntry) throws IOException {
		int compression = contentEntry.getCompressionMethod();
		if (compression != ZipEntry.STORED && compression != ZipEntry.DEFLATED) {
			throw new ZipException("invalid compression method");
		}
		synchronized (this) {
			ensureOpen();
			InputStream inputStream = new JarEntryInputStream(contentEntry);
			try {
				if (compression == ZipEntry.DEFLATED) {
					inputStream = new JarEntryInflaterInputStream((JarEntryInputStream) inputStream, this.resources);
				}
				this.resources.addInputStream(inputStream);
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
		if (this.closed) {
			return;
		}
		this.closed = true;
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
		if (this.closed) {
			throw new IllegalStateException("Zip file closed");
		}
		if (this.resources.zipContent() == null) {
			throw new IllegalStateException("The object is not initialized.");
		}
	}

	/**
	 * Clear any internal caches.
	 */
	public void clearCache() {
		synchronized (this) {
			this.lastNamePrefix = null;
			this.lastName = null;
			this.lastContentEntry = null;
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

		boolean isOwnedBy(NestedJarFile nestedJarFile) {
			return NestedJarFile.this == nestedJarFile;
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

		private final ZipContent zipContent;

		private int cursor;

		JarEntriesEnumeration(ZipContent zipContent) {
			this.zipContent = zipContent;
		}

		@Override
		public boolean hasMoreElements() {
			return this.cursor < this.zipContent.size();
		}

		@Override
		public NestedJarEntry nextElement() {
			if (!hasMoreElements()) {
				throw new NoSuchElementException();
			}
			synchronized (NestedJarFile.this) {
				ensureOpen();
				return this.zipContent.getEntry(this.cursor++).as(NestedJarEntry::new);
			}
		}

	}

	/**
	 * {@link Spliterator} for {@link ZipContent.Entry} instances.
	 */
	private class ZipContentEntriesSpliterator extends AbstractSpliterator<ZipContent.Entry> {

		private static final int ADDITIONAL_CHARACTERISTICS = Spliterator.ORDERED | Spliterator.DISTINCT
				| Spliterator.IMMUTABLE | Spliterator.NONNULL;

		private final ZipContent zipContent;

		private int cursor;

		ZipContentEntriesSpliterator(ZipContent zipContent) {
			super(zipContent.size(), ADDITIONAL_CHARACTERISTICS);
			this.zipContent = zipContent;
		}

		@Override
		public boolean tryAdvance(Consumer<? super ZipContent.Entry> action) {
			if (this.cursor < this.zipContent.size()) {
				synchronized (NestedJarFile.this) {
					ensureOpen();
					action.accept(this.zipContent.getEntry(this.cursor++));
				}
				return true;
			}
			return false;
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

		private volatile boolean closed;

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
			if (NestedJarFile.this.closed || this.closed) {
				throw new ZipException("ZipFile closed");
			}
		}

		@Override
		public void close() throws IOException {
			if (this.closed) {
				return;
			}
			this.closed = true;
			this.content.close();
			NestedJarFile.this.resources.removeInputStream(this);
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

		private volatile boolean closed;

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
			if (this.closed) {
				return;
			}
			this.closed = true;
			super.close();
			NestedJarFile.this.resources.removeInputStream(this);
			this.cleanup.clean();
		}

	}

}
