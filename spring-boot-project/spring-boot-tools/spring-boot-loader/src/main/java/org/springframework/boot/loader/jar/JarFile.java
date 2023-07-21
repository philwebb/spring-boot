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
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import org.springframework.boot.loader.ref.Cleaner;
import org.springframework.boot.loader.zip.CloseableDataBlock;
import org.springframework.boot.loader.zip.ZipContent;

/**
 * Extended variant of {@link java.util.jar.JarFile} that behaves in the same way but can
 * open nested jars.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.0.0
 */
public class JarFile extends java.util.jar.JarFile {

	private static final int DECIMAL = 10;

	private static final String META_INF = "META-INF/";

	private static final String META_INF_VERSIONS = META_INF + "versions/";

	private static final int BASE_VERSION = baseVersion().feature();

	private final Resources resources;

	private final Cleanable cleanup;

	private final String name;

	private final int version;

	private String lastEntryPrefix;

	private String lastEntryName;

	private ZipContent.Entry lastEntry;

	private volatile boolean closing;

	/**
	 * Creates a new {@link JarFile} instance to read from the specific {@code File}.
	 * @param file the jar file to be opened for reading
	 * @throws IOException on I/O error
	 */
	public JarFile(File file) throws IOException {
		this(file, null);
	}

	/**
	 * Creates a new {@link JarFile} instance to read from the specific {@code File}.
	 * @param file the jar file to be opened for reading
	 * @param nestedEntryName the nested entry name to open or {@code null}
	 * @throws IOException on I/O error
	 */
	public JarFile(File file, String nestedEntryName) throws IOException {
		this(file, nestedEntryName, baseVersion());
	}

	/**
	 * Creates a new {@link JarFile} instance to read from the specific {@code File}.
	 * @param file the jar file to be opened for reading
	 * @param nestedEntryName the nested entry name to open or {@code null}
	 * @param version the release version to use when opening a multi-release jar
	 * @throws IOException on I/O error
	 */
	public JarFile(File file, String nestedEntryName, Runtime.Version version) throws IOException {
		super(file);
		this.resources = new Resources(file, nestedEntryName);
		this.cleanup = Cleaner.register(this, this.resources);
		this.name = file.getPath() + ((nestedEntryName != null) ? "[" + nestedEntryName + "]" : "");
		this.version = version.feature();
	}

	@Override
	public Manifest getManifest() throws IOException {
		try {
			return this.resources.zipContent.getInfo(ManifestInfo.class, this::computeManifestInfo).getManifest();
		}
		catch (UncheckedIOException ex) {
			throw ex.getCause();
		}
	}

	@Override
	public Enumeration<JarEntry> entries() {
		synchronized (this) {
			ensureOpen();
			return new JarEntryEnumeration(this.resources.zipContent().iterator());
		}
	}

	@Override
	public Stream<JarEntry> stream() {
		synchronized (this) {
			ensureOpen();
			return this.resources.zipContent()
				.stream()
				.map((contentEntry) -> contentEntry.as((realName) -> new Entry(contentEntry, realName)));
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
			return contentEntry.as((realName) -> new Entry(contentEntry, realName));
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
		return contentEntry.as((realName) -> new Entry(contentEntry, baseName));
	}

	public static <T, K> Predicate<T> nonNullDistinct(Function<T, K> extractor) {
		Set<K> seen = ConcurrentHashMap.newKeySet();
		return (entry) -> entry != null && seen.add(extractor.apply(entry));
	}

	@Override
	public JarEntry getJarEntry(String name) {
		return getEntry(name);
	}

	@Override
	public JarEntry getEntry(String name) {
		Objects.requireNonNull(name, "name");
		Entry entry = getVersionedEntry(name);
		return (entry != null) ? entry : getEntry(null, name);
	}

	private Entry getVersionedEntry(String name) {
		// NOTE: we can't call isMultiRelease() directly because it's a final method and
		// it inspects the container jar. We use ManifestInfo instead.
		ManifestInfo manifestInfo = this.resources.zipContent().getInfo(ManifestInfo.class, this::computeManifestInfo);
		if (!manifestInfo.isMultiRelease() || name.startsWith(META_INF) || BASE_VERSION < this.version) {
			return null;
		}
		MetaInfVersionsInfo info = this.resources.zipContent()
			.getInfo(MetaInfVersionsInfo.class, MetaInfVersionsInfo::compute);
		int[] versions = info.versions();
		String[] directories = info.directories();
		for (int i = versions.length - 1; i >= 0; i--) {
			if (versions[i] <= this.version) {
				Entry entry = getEntry(directories[i], name);
				if (entry != null) {
					return entry;
				}
			}
		}
		return null;
	}

	private Entry getEntry(String namePrefix, String name) {
		ZipContent.Entry contentEntry = getContentEntry(namePrefix, name);
		return (contentEntry != null) ? contentEntry.as((realName) -> new Entry(contentEntry, name)) : null;
	}

	private ZipContent.Entry getContentEntry(String namePrefix, String name) {
		synchronized (this) {
			ensureOpen();
			if (Objects.equals(namePrefix, this.lastEntryPrefix) && Objects.equals(name, this.lastEntryName)) {
				return this.lastEntry;
			}
			ZipContent.Entry entry = this.resources.zipContent().getEntry(namePrefix, name);
			this.lastEntryName = name;
			this.lastEntry = entry;
			return entry;
		}
	}

	private ManifestInfo computeManifestInfo(ZipContent zipContent) {
		ZipContent.Entry manifestEntry = zipContent.getEntry(MANIFEST_NAME);
		if (manifestEntry == null) {
			return ManifestInfo.NONE;
		}
		try {
			try (InputStream inputStream = getInputStream(manifestEntry)) {
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
		if (!(entry instanceof Entry)) {
			entry = getEntry(entry.getName());
		}
		return getInputStream(((Entry) entry).contentEntry());
	}

	private InputStream getInputStream(ZipContent.Entry entry) throws IOException {
		int compression = entry.getCompressionMethod();
		if (compression != ZipEntry.STORED && compression != ZipEntry.DEFLATED) {
			throw new ZipException("invalid compression method");
		}
		Set<InputStream> inputStreams = this.resources.inputStreams();
		synchronized (this) {
			ensureOpen();
			InputStream inputStream = new EntryInputStream(entry);
			try {
				if (compression == ZipEntry.DEFLATED) {
					inputStream = new InflaterEntryInputStream((EntryInputStream) inputStream, this.resources);
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
	 * Resources created managed and cleaned by a {@link JarFile} instance.
	 */
	private static class Resources implements Runnable {

		private static final int INFLATER_CACHE_LIMIT = 20;

		private ZipContent zipContent;

		private final Set<InputStream> inputStreams = Collections.newSetFromMap(new WeakHashMap<>());

		private Deque<Inflater> inflaterCache = new ArrayDeque<>();

		Resources(File file, String nestedEntryName) throws IOException {
			this.zipContent = ZipContent.open(file.toPath(), nestedEntryName);
		}

		ZipContent zipContent() {
			return this.zipContent;
		}

		Set<InputStream> inputStreams() {
			return this.inputStreams;
		}

		@Override
		public void run() {
			IOException exceptionChain = null;
			exceptionChain = releaseInflators(exceptionChain);
			exceptionChain = releaseInputStreams(exceptionChain);
			exceptionChain = releaseZipContent(exceptionChain);
			if (exceptionChain != null) {
				throw new UncheckedIOException(exceptionChain);
			}
		}

		private IOException releaseInflators(IOException exceptionChain) {
			Deque<Inflater> inflaterCache = this.inflaterCache;
			if (inflaterCache != null) {
				synchronized (inflaterCache) {
					inflaterCache.stream().forEach(Inflater::end);
				}
				this.inflaterCache = null;
			}
			return exceptionChain;
		}

		private IOException releaseInputStreams(IOException exceptionChain) {
			synchronized (this.inputStreams) {
				for (InputStream inputStream : this.inputStreams) {
					try {
						inputStream.close();
					}
					catch (IOException ex) {
						exceptionChain = addToExceptionChain(exceptionChain, ex);
					}
					this.inputStreams.clear();
				}
			}
			return exceptionChain;
		}

		private IOException releaseZipContent(IOException exceptionChain) {
			if (this.zipContent != null) {
				try {
					this.zipContent.close();
				}
				catch (IOException ex) {
					exceptionChain = addToExceptionChain(exceptionChain, ex);
				}
			}
			return exceptionChain;
		}

		private IOException addToExceptionChain(IOException exceptionChain, IOException ex) {
			if (exceptionChain != null) {
				exceptionChain.addSuppressed(ex);
				return exceptionChain;
			}
			return ex;
		}

		Runnable createInflatorCleanupAction(Inflater inflater) {
			return () -> endOrCacheInflater(inflater);
		}

		Inflater getOrCreateInflater() {
			Deque<Inflater> inflaterCache = this.inflaterCache;
			if (inflaterCache != null) {
				synchronized (inflaterCache) {
					Inflater inflater = this.inflaterCache.poll();
					if (inflater != null) {
						return inflater;
					}
				}
			}
			return new Inflater(true);
		}

		private void endOrCacheInflater(Inflater inflater) {
			Deque<Inflater> inflaterCache = this.inflaterCache;
			if (inflaterCache != null) {
				synchronized (inflaterCache) {
					if (this.inflaterCache == inflaterCache && inflaterCache.size() < INFLATER_CACHE_LIMIT) {
						inflater.reset();
						this.inflaterCache.add(inflater);
						return;
					}
				}
			}
			inflater.end();
		}

	}

	/**
	 * {@link Enumeration} of {@link JarEntry} instances.
	 */
	private class JarEntryEnumeration implements Enumeration<JarEntry> {

		private Iterator<ZipContent.Entry> iterator;

		JarEntryEnumeration(Iterator<ZipContent.Entry> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasMoreElements() {
			return this.iterator.hasNext();
		}

		@Override
		public JarEntry nextElement() {
			ZipContent.Entry next = this.iterator.next();
			return next.as((realName) -> new Entry(next, realName));
		}

	}

	/**
	 * An individual entry from this jar file.
	 */
	private class Entry extends java.util.jar.JarEntry {

		private final ZipContent.Entry contentEntry;

		private final String name;

		Entry(ZipContent.Entry contentEntry, String name) {
			super(contentEntry.getName());
			this.contentEntry = contentEntry;
			this.name = name;
		}

		ZipContent.Entry contentEntry() {
			return this.contentEntry;
		}

		@Override
		public Attributes getAttributes() throws IOException {
			Manifest manifest = getManifest();
			return (manifest != null) ? manifest.getAttributes(getName()) : null;
		}

		@Override
		public Certificate[] getCertificates() {
			return getCertification().getCertificates();
		}

		@Override
		public CodeSigner[] getCodeSigners() {
			return getCertification().getCodeSigners();
		}

		private JarEntryCertification getCertification() {
			// @formatter:off
//			if (!this.jarFile.isSigned()) {
//				return JarEntryCertification.NONE;
//			}
//			JarEntryCertification certification = this.certification;
//			if (certification == null) {
//				certification = this.jarFile.getCertification(this);
//				this.certification = certification;
//			}
//			return certification;
			// @formatter:on
			return null;
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
	 * {@link InputStream} to read entry content.
	 */
	private class EntryInputStream extends InputStream {

		private final int uncompressedSize;

		private final CloseableDataBlock content;

		private long pos;

		private long remaining;

		private volatile boolean closing;

		EntryInputStream(ZipContent.Entry entry) throws IOException {
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
			synchronized (JarFile.this) {
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
			synchronized (JarFile.this) {
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
			if (JarFile.this.closing || this.closing) {
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
			Set<InputStream> inputStreams = JarFile.this.resources.inputStreams();
			synchronized (inputStreams) {
				inputStreams.remove(this);
			}
		}

		int getUncompressedSize() {
			return this.uncompressedSize;
		}

	}

	/**
	 * {@link ZipInflaterInputStream} to read entry content.
	 */
	private class InflaterEntryInputStream extends ZipInflaterInputStream {

		private final Cleanable cleanup;

		private volatile boolean closing;

		InflaterEntryInputStream(EntryInputStream inputStream, Resources resources) {
			this(inputStream, resources, resources.getOrCreateInflater());
		}

		private InflaterEntryInputStream(EntryInputStream inputStream, Resources resources, Inflater inflater) {
			super(inputStream, inflater, inputStream.getUncompressedSize());
			this.cleanup = Cleaner.register(this, resources.createInflatorCleanupAction(inflater));
		}

		@Override
		public void close() throws IOException {
			if (this.closing) {
				return;
			}
			this.closing = true;
			super.close();
			synchronized (JarFile.this.resources.inputStreams()) {
				JarFile.this.resources.inputStreams().remove(this);
			}
			this.cleanup.clean();
		}

	}

	/**
	 * Info related to the directories listed under {@code META-INF/versions/}.
	 */
	private static class MetaInfVersionsInfo {

		private static final MetaInfVersionsInfo NONE = new MetaInfVersionsInfo(Collections.emptySet());

		private final int[] versions;

		private final String[] directories;

		MetaInfVersionsInfo(Set<Integer> versions) {
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

		static MetaInfVersionsInfo compute(ZipContent zipContent) {
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
			return (!versions.isEmpty()) ? new MetaInfVersionsInfo(versions) : NONE;
		}

	}

	/**
	 * Info related to the {@link Manifest}.
	 */
	private static class ManifestInfo {

		private static final Name MULTI_RELEASE = new Name("Multi-Release");

		static final ManifestInfo NONE = new ManifestInfo(null, false);

		private final Manifest manifest;

		private Boolean multiRelease;

		private ManifestInfo(Manifest manifest, Boolean multiRelease) {
			this.manifest = manifest;
		}

		Manifest getManifest() {
			return this.manifest;
		}

		boolean isMultiRelease() {
			if (this.manifest == null) {
				this.multiRelease = false;
			}
			Boolean multiRelease = this.multiRelease;
			if (multiRelease != null) {
				return multiRelease;
			}
			Attributes attributes = this.manifest.getMainAttributes();
			multiRelease = attributes.containsKey(MULTI_RELEASE);
			this.multiRelease = multiRelease;
			return multiRelease;
		}

	}

}
