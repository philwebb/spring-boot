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

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner.Cleanable;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;

import org.springframework.boot.loader.log.DebugLogger;

/**
 * Provides raw access to content from a regular or nested zip file. This class performs
 * the low level parsing of a zip file and provide access to raw entry data that it
 * contains. Unlike {@link java.util.zip.ZipFile}, this implementation can load content
 * from a zip file nested inside another file as long as the entry is not compressed.
 * <p>
 * In order to reduce memory consumption, this implementation stores only the the hash of
 * the entry names, the central directory offsets and the original positions. Entries are
 * stored internally in {@code hashCode} order so that a binary search can be used to
 * quickly find an entry by name or determine if the zip file doesn't have a given entry.
 * <p>
 * {@link ZipContent} for a typical Spring Boot application JAR will have somewhere in the
 * region of 10,500 entries which should consume about 122K.
 * <p>
 * {@link ZipContent} results are cached and it is assumed that zip content will not
 * change once loaded. Entries and Strings are not cached and will be recreated on each
 * access which may produce a lot of garbage.
 * <p>
 * This implementation does not use {@link Cleanable} so care must be taken to release
 * {@link ZipContent} resources. The {@link #close()} method should be called explicitly
 * or by try-with-resources. Care must be take to only call close once.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 3.2.0
 */
public final class ZipContent implements Iterable<ZipContent.Entry>, Closeable {

	private static int ADDITIONAL_SPLITERATOR_CHARACTERISTICS = Spliterator.ORDERED | Spliterator.DISTINCT
			| Spliterator.IMMUTABLE | Spliterator.NONNULL;

	private static final String META_INF = "META-INF/";

	private static byte[] SIGNATURE_SUFFIX = ".DSA".getBytes(StandardCharsets.UTF_8);

	private static final DebugLogger debug = DebugLogger.get(ZipContent.class);

	private static final Map<Source, ZipContent> cache = new ConcurrentHashMap<>();

	private final Source source;

	private final FileChannelDataBlock data;

	private final long centralDirectoryPos;

	private final long commentPos;

	private final long commentLength;

	private final NameOffsets nameOffsets;

	private final int[] nameHashes;

	private final int[] relativeCentralDirectoryOffsets;

	private final int[] orderIndexes;

	private final boolean hasJarSignatureFile;

	private SoftReference<CloseableDataBlock> virtualData;

	private SoftReference<Map<Class<?>, Object>> info;

	private ZipContent(Source source, FileChannelDataBlock data, long centralDirectoryPos, long commentPos,
			long commentLength, NameOffsets nameOffsets, int[] nameHashes, int[] relativeCentralDirectoryOffsets,
			int[] orderIndexes, boolean hasJarSignatureFile) {
		this.source = source;
		this.data = data;
		this.centralDirectoryPos = centralDirectoryPos;
		this.commentPos = commentPos;
		this.commentLength = commentLength;
		this.nameOffsets = nameOffsets;
		this.nameHashes = nameHashes;
		this.relativeCentralDirectoryOffsets = relativeCentralDirectoryOffsets;
		this.orderIndexes = orderIndexes;
		this.hasJarSignatureFile = hasJarSignatureFile;
	}

	/**
	 * Open a {@link DataBlock} containing the raw zip data. For container zip files, this
	 * may be smaller than the original file since additional bytes are permitted at the
	 * front of a zip file. For nested zip files, this will be only the contents of the
	 * nest zip.
	 * <p>
	 * For nested directory zip files, a virtual data block will be created containing
	 * only the relevant content.
	 * <p>
	 * To release resources, the {@link #close()} method of the data block should be
	 * called explicitly or by try-with-resources.
	 * <p>
	 * The returned data block should not be accessed once {@link #close()} has been
	 * called.
	 * @return the zip data
	 * @throws IOException on I/O error
	 */
	public CloseableDataBlock openRawZipData() throws IOException {
		if (this.nameOffsets.hasAnyEnabled()) {
			return getVirtualData();
		}
		this.data.open();
		return this.data;
	}

	private CloseableDataBlock getVirtualData() throws IOException {
		CloseableDataBlock virtualData = (this.virtualData != null) ? this.virtualData.get() : null;
		if (virtualData != null) {
			return virtualData;
		}
		virtualData = createVirtualData();
		this.virtualData = new SoftReference<>(virtualData);
		return virtualData;
	}

	private CloseableDataBlock createVirtualData() throws IOException {
		NameOffsets nameOffsets = this.nameOffsets.emptyCopy();
		ZipCentralDirectoryFileHeaderRecord[] centralRecords = new ZipCentralDirectoryFileHeaderRecord[size()];
		long[] centralRecordPositions = new long[centralRecords.length];
		for (int i = 0; i < this.orderIndexes.length; i++) {
			int index = ZipContent.this.orderIndexes[i];
			nameOffsets.enable(i, this.nameOffsets.isEnabled(index));
			long pos = getCentralDirectoryFileHeaderRecordPos(index);
			centralRecordPositions[i] = pos;
			centralRecords[i] = ZipCentralDirectoryFileHeaderRecord.load(this.data, pos);
		}
		return new VirtualZipDataBlock(this.data, nameOffsets, centralRecords, centralRecordPositions);
	}

	/**
	 * Return a {@link Stream} of all the {@link Entry entries}.
	 * @return a stream of entries
	 */
	public Stream<Entry> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	/**
	 * Iterate entries in the order that they were written to the zip file.
	 * @see Iterable#iterator()
	 */
	@Override
	public Iterator<Entry> iterator() {
		return new EntryIterator();
	}

	@Override
	public Spliterator<Entry> spliterator() {
		return Spliterators.spliterator(new EntryIterator(), this.nameHashes.length,
				ADDITIONAL_SPLITERATOR_CHARACTERISTICS);
	}

	/**
	 * Returns the number of entries in the ZIP file.
	 * @return the number of entries
	 */
	public int size() {
		return this.nameHashes.length;
	}

	/**
	 * Return the zip comment, if any.
	 * @return the comment or {@code null}
	 */
	public String getComment() {
		try {
			return ZipString.readString(this.data, this.commentPos, this.commentLength);
		}
		catch (UncheckedIOException ex) {
			if (ex.getCause() instanceof ClosedChannelException) {
				throw new IllegalStateException("Zip content closed", ex);
			}
			throw ex;
		}
	}

	/**
	 * Return the entry with the given name, if any.
	 * @param name the name of the entry to find
	 * @return the entry or {@code null}
	 */
	public Entry getEntry(CharSequence name) {
		return getEntry(null, name);
	}

	/**
	 * Return the entry with the given name, if any.
	 * @param namePrefix an optional prefix for the name
	 * @param name the name of the entry to find
	 * @return the entry or {@code null}
	 */
	public Entry getEntry(CharSequence namePrefix, CharSequence name) {
		int nameHash = nameHash(namePrefix, name);
		int index = getFirstIndex(nameHash);
		while (index >= 0 && index < this.nameHashes.length && this.nameHashes[index] == nameHash) {
			long pos = getCentralDirectoryFileHeaderRecordPos(index);
			ZipCentralDirectoryFileHeaderRecord centralRecord = loadZipCentralDirectoryFileHeaderRecord(pos);
			if (hasName(index, centralRecord, pos, namePrefix, name)) {
				return new Entry(index, centralRecord);
			}
			index++;
		}
		return null;
	}

	private ZipCentralDirectoryFileHeaderRecord loadZipCentralDirectoryFileHeaderRecord(long pos) {
		try {
			return ZipCentralDirectoryFileHeaderRecord.load(this.data, pos);
		}
		catch (IOException ex) {
			if (ex instanceof ClosedChannelException) {
				throw new IllegalStateException("Zip content closed", ex);
			}
			throw new UncheckedIOException(ex);
		}

	}

	private int nameHash(CharSequence namePrefix, CharSequence name) {
		int nameHash = 0;
		nameHash = (namePrefix != null) ? ZipString.hash(nameHash, namePrefix, false) : nameHash;
		nameHash = ZipString.hash(nameHash, name, true);
		return nameHash;
	}

	private int getFirstIndex(int nameHash) {
		int index = Arrays.binarySearch(this.nameHashes, 0, this.nameHashes.length, nameHash);
		if (index < 0) {
			return -1;
		}
		while (index > 0 && this.nameHashes[index - 1] == nameHash) {
			index--;
		}
		return index;
	}

	private long getCentralDirectoryFileHeaderRecordPos(int index) {
		return this.centralDirectoryPos + this.relativeCentralDirectoryOffsets[index];
	}

	private boolean hasName(int index, ZipCentralDirectoryFileHeaderRecord centralRecord, long pos,
			CharSequence namePrefix, CharSequence name) {
		int offset = this.nameOffsets.get(index);
		pos += ZipCentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET + offset;
		int len = centralRecord.fileNameLength() - offset;
		if (namePrefix != null) {
			int startsWithNamePrefix = ZipString.startsWith(this.data, pos, len, namePrefix);
			if (startsWithNamePrefix == -1) {
				return false;
			}
			pos += startsWithNamePrefix;
			len -= startsWithNamePrefix;
		}
		return ZipString.matches(this.data, pos, len, name, true);
	}

	/**
	 * Get or compute information based on the {@link ZipContent}.
	 * @param <I> the info type to get or compute
	 * @param type the info type to get or compute
	 * @param function the function used to compute the information
	 * @return the computed or existing information
	 */
	@SuppressWarnings("unchecked")
	public <I> I getInfo(Class<I> type, Function<ZipContent, I> function) {
		Map<Class<?>, Object> info = (this.info != null) ? this.info.get() : null;
		if (info == null) {
			info = new ConcurrentHashMap<>();
			this.info = new SoftReference<>(info);
		}
		return (I) info.computeIfAbsent(type, (key) -> {
			debug.log("Getting %s from zip '%s'", type.getName(), this);
			return function.apply(this);
		});
	}

	/**
	 * Returns {@code true} if this zip file contains a {@code META-INF/*.DSA} file.
	 * @return if the zip contains a jar signature file
	 */
	public boolean hasJarSignatureFile() {
		return this.hasJarSignatureFile;
	}

	/**
	 * Close this jar file, releasing the underlying file if this was the last reference.
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		this.data.close();
	}

	@Override
	public String toString() {
		return (!this.source.isNested()) ? "%s[%s]".formatted(this.source.path(), this.source.nestedEntryName())
				: this.source.path().toString();
	}

	/**
	 * Open {@link ZipContent} from the specified path. The resulting {@link ZipContent}
	 * <em>must</em> be {@link #close() closed} by the caller.
	 * @param zip the zip path
	 * @return a {@link ZipContent} instance
	 * @throws IOException on I/O error
	 */
	public static ZipContent open(Path zip) throws IOException {
		return open(new Source(zip.toAbsolutePath(), null));
	}

	/**
	 * Open nested {@link ZipContent} from the specified path. The resulting
	 * {@link ZipContent} <em>must</em> be {@link #close() closed} by the caller.
	 * @param zip the zip path
	 * @param nestedEntryName the nested entry name to open
	 * @return a {@link ZipContent} instance
	 * @throws IOException on I/O error
	 */
	public static ZipContent open(Path zip, String nestedEntryName) throws IOException {
		return open(new Source(zip.toAbsolutePath(), nestedEntryName));
	}

	private static ZipContent open(Source source) throws IOException {
		ZipContent zipContent = cache.get(source);
		if (zipContent != null) {
			debug.log("Opening existing cached zip content for %s", zipContent);
			zipContent.data.open();
			return zipContent;
		}
		debug.log("Loading zip content from %s", source);
		zipContent = Loader.load(source);
		ZipContent previouslyCached = cache.putIfAbsent(source, zipContent);
		if (previouslyCached != null) {
			debug.log("Closing zip content from %s since cache was populated from another thread", source);
			zipContent.close();
			previouslyCached.data.open();
			return previouslyCached;
		}
		return zipContent;
	}

	/**
	 * The source of {@link ZipContent}. Used as a cache key.
	 *
	 * @param path the path of the zip or container zip
	 * @param nestedEntryName the name of the nested entry to use or {@code null}
	 */
	private static record Source(Path path, String nestedEntryName) {

		/**
		 * Return if this is the source of a nested zip.
		 * @return if this is for a nested zip
		 */
		boolean isNested() {
			return this.nestedEntryName != null;
		}

	}

	/**
	 * {@link Iterator} for entries.
	 */
	private final class EntryIterator implements Iterator<Entry> {

		private int cursor = 0;

		@Override
		public boolean hasNext() {
			return this.cursor < ZipContent.this.orderIndexes.length;
		}

		@Override
		public Entry next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			int index = ZipContent.this.orderIndexes[this.cursor];
			long pos = getCentralDirectoryFileHeaderRecordPos(index);
			this.cursor++;
			return new Entry(index, loadZipCentralDirectoryFileHeaderRecord(pos));
		}

	}

	/**
	 * Internal class used to load the zip content create a new {@link ZipContent}
	 * instance.
	 */
	private static class Loader {

		private final Source source;

		private final FileChannelDataBlock data;

		private final long centralDirectoryPos;

		private final NameOffsets nameOffsets;

		private int[] nameHashes;

		private int[] relativeCentralDirectoryOffsets;

		private int[] index;

		private int cursor;

		private Loader(Source source, Entry directoryEntry, FileChannelDataBlock data, long centralDirectoryPos,
				int maxSize) {
			this.source = source;
			this.data = data;
			this.centralDirectoryPos = centralDirectoryPos;
			this.nameHashes = new int[maxSize];
			this.nameOffsets = (directoryEntry != null) ? new NameOffsets(directoryEntry.getName().length(), maxSize)
					: NameOffsets.NONE;
			this.relativeCentralDirectoryOffsets = new int[maxSize];
			this.index = new int[maxSize];
		}

		private void add(ZipCentralDirectoryFileHeaderRecord centralRecord, long pos, boolean enableNameOffset)
				throws IOException {
			int nameOffset = this.nameOffsets.enable(this.cursor, enableNameOffset);
			int hash = ZipString.hash(this.data,
					pos + ZipCentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET + nameOffset,
					centralRecord.fileNameLength() - nameOffset, true);
			this.nameHashes[this.cursor] = hash;
			this.relativeCentralDirectoryOffsets[this.cursor] = (int) ((pos - this.centralDirectoryPos) & 0xFFFFFFFF);
			this.index[this.cursor] = this.cursor;
			this.cursor++;
		}

		private ZipContent finish(long commentPos, long commentLength, boolean hasJarSignatureFile) {
			if (this.cursor != this.nameHashes.length) {
				this.nameHashes = Arrays.copyOf(this.nameHashes, this.cursor);
				this.relativeCentralDirectoryOffsets = Arrays.copyOf(this.relativeCentralDirectoryOffsets, this.cursor);
			}
			int size = this.nameHashes.length;
			sort(0, size - 1);
			int[] orderIndexes = new int[size];
			for (int i = 0; i < size; i++) {
				orderIndexes[this.index[i]] = i;
			}
			return new ZipContent(this.source, this.data, this.centralDirectoryPos, commentPos, commentLength,
					this.nameOffsets, this.nameHashes, this.relativeCentralDirectoryOffsets, orderIndexes,
					hasJarSignatureFile);
		}

		private void sort(int left, int right) {
			// Quick sort algorithm, uses nameHashCode as the source but sorts all
			// arrays
			if (left < right) {
				int pivot = this.nameHashes[left + (right - left) / 2];
				int i = left;
				int j = right;
				while (i <= j) {
					while (this.nameHashes[i] < pivot) {
						i++;
					}
					while (this.nameHashes[j] > pivot) {
						j--;
					}
					if (i <= j) {
						swap(i, j);
						i++;
						j--;
					}
				}
				if (left < j) {
					sort(left, j);
				}
				if (right > i) {
					sort(i, right);
				}
			}
		}

		private void swap(int i, int j) {
			this.nameOffsets.swap(i, j);
			swap(this.nameHashes, i, j);
			swap(this.relativeCentralDirectoryOffsets, i, j);
			swap(this.index, i, j);
		}

		protected static void swap(int[] array, int i, int j) {
			int temp = array[i];
			array[i] = array[j];
			array[j] = temp;
		}

		static ZipContent load(Source source) throws IOException {
			if (!source.isNested()) {
				return loadNonNested(source);
			}
			try (ZipContent zip = open(source.path())) {
				Entry entry = zip.getEntry(source.nestedEntryName());
				if (entry == null) {
					throw new IOException("Nested entry '%s' not found in container zip '%s'"
						.formatted(source.nestedEntryName(), source.path()));
				}
				return (!entry.isDirectory()) ? loadNestedZip(source, entry) : loadNestedDirectory(source, zip, entry);
			}
		}

		private static ZipContent loadNonNested(Source source) throws IOException {
			debug.log("Loading non-nested zip '%s'", source.path());
			return openAndLoad(source, new FileChannelDataBlock(source.path()));
		}

		private static ZipContent loadNestedZip(Source source, Entry entry) throws IOException {
			if (entry.centralRecord.compressionMethod() != ZipEntry.STORED) {
				throw new IOException("Nested entry '%s' in container zip '%s' must not be compressed"
					.formatted(source.nestedEntryName(), source.path()));
			}
			debug.log("Loading nested zip entry '%s' from '%s'", source.nestedEntryName(), source.path());
			return openAndLoad(source, entry.getContent());
		}

		private static ZipContent openAndLoad(Source source, FileChannelDataBlock data) throws IOException {
			try {
				data.open();
				return loadContent(source, data);
			}
			catch (IOException | RuntimeException ex) {
				data.close();
				throw ex;
			}
		}

		private static ZipContent loadContent(Source source, FileChannelDataBlock data) throws IOException {
			ZipEndOfCentralDirectoryRecord.Located locatedEocd = ZipEndOfCentralDirectoryRecord.load(data);
			ZipEndOfCentralDirectoryRecord eocd = locatedEocd.endOfCentralDirectoryRecord();
			long eocdPos = locatedEocd.pos();
			Zip64EndOfCentralDirectoryLocator zip64Locator = Zip64EndOfCentralDirectoryLocator.find(data, eocdPos);
			Zip64EndOfCentralDirectoryRecord zip64Eocd = Zip64EndOfCentralDirectoryRecord.load(data, zip64Locator);
			data = data.slice(getStartOfZipContent(data, eocd, zip64Eocd));
			long centralDirectoryPos = (zip64Eocd != null) ? zip64Eocd.offsetToStartOfCentralDirectory()
					: eocd.offsetToStartOfCentralDirectory();
			long numberOfEntries = (zip64Eocd != null) ? zip64Eocd.totalNumberOfCentralDirectoryEntries()
					: eocd.totalNumberOfCentralDirectoryEntries();
			if (numberOfEntries > 0xFFFFFFFFL) {
				throw new IllegalStateException("Too many zip entries in " + source);
			}
			Loader loader = new Loader(source, null, data, centralDirectoryPos, (int) numberOfEntries & 0xFFFFFFFF);
			ByteBuffer signatureNameSuffixBuffer = ByteBuffer.allocate(SIGNATURE_SUFFIX.length);
			boolean hasJarSignatureFile = false;
			long pos = centralDirectoryPos;
			for (int i = 0; i < numberOfEntries; i++) {
				ZipCentralDirectoryFileHeaderRecord centralRecord = ZipCentralDirectoryFileHeaderRecord.load(data, pos);
				if (!hasJarSignatureFile) {
					long filenamePos = pos + ZipCentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET;
					if (centralRecord.fileNameLength() > SIGNATURE_SUFFIX.length
							&& ZipString.startsWith(data, filenamePos, centralRecord.fileNameLength(), META_INF) >= 0) {
						signatureNameSuffixBuffer.clear();
						data.readFully(signatureNameSuffixBuffer,
								filenamePos + centralRecord.fileNameLength() - SIGNATURE_SUFFIX.length);
						hasJarSignatureFile = Arrays.equals(SIGNATURE_SUFFIX, signatureNameSuffixBuffer.array());
					}
				}
				loader.add(centralRecord, pos, false);
				pos += centralRecord.size();
			}
			long commentPos = locatedEocd.pos() + ZipEndOfCentralDirectoryRecord.COMMENT_OFFSET;
			return loader.finish(commentPos, eocd.commentLength(), hasJarSignatureFile);
		}

		/**
		 * Returns the location in the data that the archive actually starts. For most
		 * files the archive data will start at 0, however, it is possible to have
		 * prefixed bytes (often used for startup scripts) at the beginning of the data.
		 * @param data the source data
		 * @return the offset within the data where the archive begins
		 * @throws IOException
		 */
		private static long getStartOfZipContent(FileChannelDataBlock data, ZipEndOfCentralDirectoryRecord eocd,
				Zip64EndOfCentralDirectoryRecord zip64Eocd) throws IOException {
			long specifiedOffsetToStartOfCentralDirectory = (zip64Eocd != null)
					? zip64Eocd.offsetToStartOfCentralDirectory() : eocd.offsetToStartOfCentralDirectory();
			long sizeOfCentralDirectoryAndEndRecords = getSizeOfCentralDirectoryAndEndRecords(eocd, zip64Eocd);
			long actualOffsetToStartOfCentralDirectory = data.size() - sizeOfCentralDirectoryAndEndRecords;
			return actualOffsetToStartOfCentralDirectory - specifiedOffsetToStartOfCentralDirectory;
		}

		private static long getSizeOfCentralDirectoryAndEndRecords(ZipEndOfCentralDirectoryRecord eocd,
				Zip64EndOfCentralDirectoryRecord zip64Eocd) {
			long result = 0;
			result += eocd.size();
			if (zip64Eocd != null) {
				result += Zip64EndOfCentralDirectoryLocator.SIZE;
				result += zip64Eocd.size();
			}
			result += (zip64Eocd != null) ? zip64Eocd.sizeOfCentralDirectory() : eocd.sizeOfCentralDirectory();
			return result;
		}

		private static ZipContent loadNestedDirectory(Source source, ZipContent zip, Entry directoryEntry)
				throws IOException {
			debug.log("Loading nested directry entry '%s' from '%s'", source.nestedEntryName(), source.path());
			if (!source.nestedEntryName().endsWith("/")) {
				throw new IllegalArgumentException("Nested entry name must end with '/'");
			}
			String directoryName = directoryEntry.getName();
			zip.data.open();
			try {
				Loader loader = new Loader(source, directoryEntry, zip.data, zip.centralDirectoryPos, zip.size());
				for (int cursor = 0; cursor < zip.size(); cursor++) {
					int index = zip.orderIndexes[cursor];
					if (index != directoryEntry.getIndex()) {
						long pos = zip.getCentralDirectoryFileHeaderRecordPos(index);
						ZipCentralDirectoryFileHeaderRecord centralRecord = ZipCentralDirectoryFileHeaderRecord
							.load(zip.data, pos);
						long namePos = pos + ZipCentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET;
						short nameLen = centralRecord.fileNameLength();
						if (ZipString.startsWith(zip.data, namePos, nameLen, META_INF) != -1) {
							loader.add(centralRecord, pos, false);
						}
						else if (ZipString.startsWith(zip.data, namePos, nameLen, directoryName) != -1) {
							loader.add(centralRecord, pos, true);
						}
					}
				}
				return loader.finish(zip.commentPos, zip.commentLength, zip.hasJarSignatureFile);
			}
			catch (IOException | RuntimeException ex) {
				zip.data.close();
				throw ex;
			}
		}

	}

	/**
	 * A single zip content entry.
	 */
	public class Entry {

		private final int index;

		private final ZipCentralDirectoryFileHeaderRecord centralRecord;

		private volatile String name;

		private volatile FileChannelDataBlock content;

		/**
		 * Create a new {@link Entry} instance.
		 * @param index the index of the entry
		 * @param centralRecord the {@link ZipCentralDirectoryFileHeaderRecord} for the
		 * entry
		 */
		Entry(int index, ZipCentralDirectoryFileHeaderRecord centralRecord) {
			this.index = index;
			this.centralRecord = centralRecord;
		}

		/**
		 * Return the index of the entry.
		 * @return the entry index
		 */
		public int getIndex() {
			return this.index;
		}

		/**
		 * Return {@code true} if this is a directory entry.
		 * @return if the entry is a directory
		 */
		public boolean isDirectory() {
			return getName().endsWith("/");
		}

		/**
		 * Returns {@code true} if this entry has a name starting with the given prefix.
		 * @param prefix the required prefix
		 * @return if the entry name starts with the prefix
		 */
		public boolean hasNameStartingWith(CharSequence prefix) {
			String name = this.name;
			if (name != null) {
				return name.startsWith(prefix.toString());
			}
			long pos = getCentralDirectoryFileHeaderRecordPos(this.index)
					+ ZipCentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET;
			return ZipString.startsWith(ZipContent.this.data, pos, this.centralRecord.fileNameLength(), prefix) != -1;
		}

		/**
		 * Return the name of this entry.
		 * @return the entry name
		 */
		public String getName() {
			String name = this.name;
			if (name == null) {
				int offset = ZipContent.this.nameOffsets.get(this.index);
				long pos = getCentralDirectoryFileHeaderRecordPos(this.index)
						+ ZipCentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET + offset;
				name = ZipString.readString(ZipContent.this.data, pos, this.centralRecord.fileNameLength() - offset);
				this.name = name;
			}
			return name;
		}

		/**
		 * Return the compression method for this entry.
		 * @return the compression method
		 * @see ZipEntry#STORED
		 * @see ZipEntry#DEFLATED
		 */
		public int getCompressionMethod() {
			return this.centralRecord.compressionMethod();
		}

		/**
		 * Return the uncompressed size of this entry.
		 * @return the uncompressed size
		 */
		public int getUncompressedSize() {
			return this.centralRecord.uncompressedSize();
		}

		/**
		 * Open a {@link DataBlock} providing access to raw contents of the entry (not
		 * including the local file header).
		 * <p>
		 * To release resources, the {@link #close()} method of the data block should be
		 * called explicitly or by try-with-resources.
		 * @return the contents of the entry
		 * @throws IOException on I/O error
		 */
		public CloseableDataBlock openContent() throws IOException {
			FileChannelDataBlock content = getContent();
			content.open();
			return content;
		}

		private FileChannelDataBlock getContent() throws IOException {
			FileChannelDataBlock content = this.content;
			if (content == null) {
				int pos = this.centralRecord.offsetToLocalHeader();
				checkNotZip64Extended(pos);
				ZipLocalFileHeaderRecord localHeader = ZipLocalFileHeaderRecord.load(ZipContent.this.data, pos);
				int size = this.centralRecord.compressedSize();
				checkNotZip64Extended(size);
				content = ZipContent.this.data.slice(pos + localHeader.size(), size);
				this.content = content;
			}
			return content;
		}

		private void checkNotZip64Extended(int value) throws IOException {
			if (value == 0xFFFFFFFF) {
				throw new IOException("Zip64 extended information extra fields are not supported");
			}
		}

		/**
		 * Adapt the raw entry into a {@link ZipEntry} or {@link ZipEntry} subclass.
		 * @param <E> the entry type
		 * @param factory the factory used to create the {@link ZipEntry}
		 * @return a fully populated zip entry
		 */
		public <E extends ZipEntry> E as(Function<String, E> factory) {
			return as((entry, name) -> factory.apply(name));
		}

		/**
		 * Adapt the raw entry into a {@link ZipEntry} or {@link ZipEntry} subclass.
		 * @param <E> the entry type
		 * @param factory the factory used to create the {@link ZipEntry}
		 * @return a fully populated zip entry
		 */
		public <E extends ZipEntry> E as(BiFunction<Entry, String, E> factory) {
			try {
				E result = factory.apply(this, getName());
				long pos = getCentralDirectoryFileHeaderRecordPos(this.index);
				this.centralRecord.copyTo(ZipContent.this.data, pos, result);
				return result;
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

	}

}
