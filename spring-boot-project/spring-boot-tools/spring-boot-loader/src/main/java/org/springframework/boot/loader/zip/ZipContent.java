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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
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

	private static final DebugLogger debug = DebugLogger.get(ZipContent.class);

	private static final Map<Source, ZipContent> cache = new ConcurrentHashMap<>();

	private final FileChannelDataBlock data;

	private final long centralDirectoryPos;

	private final long commentPos;

	private final long commentLength;

	private final int[] nameHash;

	private final int[] relativeCentralDirectoryOffset;

	private final int[] position;

	private final Map<Class<?>, Object> info = new HashMap<>();

	/**
	 * If not {@code null} only items set in the filter should be included.
	 */
	private final BitSet filter;

	private final String namePrefix;

	private final Map<String, Split> splitCache = new ConcurrentHashMap<>();

	private SoftReference<DataBlock> virtualData;

	private ZipContent(FileChannelDataBlock data, long centralDirectoryPos, long commentPos, long commentLength,
			int[] nameHash, int[] relativeCentralDirectoryOffset, int[] position) {
		this.data = data;
		this.centralDirectoryPos = centralDirectoryPos;
		this.commentPos = commentPos;
		this.commentLength = commentLength;
		this.nameHash = nameHash;
		this.relativeCentralDirectoryOffset = relativeCentralDirectoryOffset;
		this.position = position;
		this.filter = null;
		this.namePrefix = null;
	}

	private ZipContent(ZipContent zipContent, BitSet filter, String namePrefix) throws IOException {
		this.data = zipContent.data;
		this.centralDirectoryPos = zipContent.centralDirectoryPos;
		this.commentPos = zipContent.commentPos;
		this.commentLength = zipContent.commentLength;
		this.nameHash = zipContent.nameHash;
		this.relativeCentralDirectoryOffset = zipContent.relativeCentralDirectoryOffset;
		this.position = zipContent.position;
		this.filter = filter;
		this.namePrefix = namePrefix;
		this.data.open();
	}

	/**
	 * Split the zip based based the named directory entry. This method will return two
	 * new {@link ZipContent} instances, one containing the included entries and one
	 * containing the remaining entries. Included entries will named with the directory
	 * prefix removed. The caller is responsible for {@link #close() closing} the returned
	 * instance, this {@link ZipContent} instance will be closed after split and should
	 * not be used again.
	 * @param directoryName the name of the directory that should be split off
	 * @return the split zip content
	 * @throws IOException on I/O error
	 */
	public Split split(String directoryName) throws IOException {
		Entry directoryEntry = getEntry(directoryName);
		if (directoryEntry == null || !directoryEntry.isDirectory()) {
			throw new IllegalStateException("No directory entry '%s' found".formatted(directoryName));
		}
		String namePrefix = directoryEntry.getName();
		Split split = this.splitCache.get(namePrefix);
		if (split != null) {
			debug.log("Opening existing cached split zip for %s", namePrefix);
			split.open();
			close();
			return split;
		}
		debug.log("Splitting zip content by %s", namePrefix);
		int size = size();
		BitSet includedFilter = new BitSet(size);
		BitSet remainderFilter = new BitSet(size);
		for (int i = 0; i < this.nameHash.length; i++) {
			if (i != directoryEntry.getIndex()) {
				long pos = getCentralDirectoryFileHeaderRecordPos(i);
				CentralDirectoryFileHeaderRecord centralRecord = CentralDirectoryFileHeaderRecord.load(this.data, pos);
				boolean underDirectory = ZipString.startsWith(this.data,
						pos + CentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET, centralRecord.fileNameLength(),
						namePrefix) != -1;
				includedFilter.set(i, underDirectory);
				remainderFilter.set(i, !underDirectory);
			}
		}
		ZipContent included = new ZipContent(this, includedFilter, namePrefix);
		ZipContent remainder = new ZipContent(this, remainderFilter, null);
		split = new Split(included, remainder);
		Split previouslySplit = this.splitCache.putIfAbsent(namePrefix, split);
		if (previouslySplit != null) {
			debug.log("Closing split zip content from %s since cache was populated from another thread", namePrefix);
			split.close();
			previouslySplit.open();
			close();
			return previouslySplit;
		}
		close();
		return split;
	}

	/**
	 * Return the data block containing the zip data. For container zip files, this may be
	 * smaller than the original file since additional bytes are permitted at the front of
	 * a zip file. For nested zip files, this will be only the contents of the nest zip.
	 * <p>
	 * For {@link #split(String) split} zip files, a virtual data block will be created
	 * containing only the split content.
	 * <p>
	 * Data contents must not be accessed after calling {@link ZipContent#close()} .
	 * @return the zip data
	 * @throws IOException on I/O error
	 */
	public DataBlock getData() throws IOException {
		return (this.filter != null) ? getVirtualData() : this.data;
	}

	private DataBlock getVirtualData() throws IOException {
		DataBlock virtualData = (this.virtualData != null) ? this.virtualData.get() : null;
		if (virtualData != null) {
			return virtualData;
		}
		virtualData = createVirtualData();
		this.virtualData = new SoftReference<>(virtualData);
		return virtualData;
	}

	private DataBlock createVirtualData() throws IOException {
		CentralDirectoryFileHeaderRecord[] centralRecords = new CentralDirectoryFileHeaderRecord[size()];
		long[] centralRecordPositions = new long[centralRecords.length];
		int i = 0;
		for (Entry entry : this) {
			long pos = getCentralDirectoryFileHeaderRecordPos(entry.getIndex());
			centralRecordPositions[i] = pos;
			centralRecords[i] = CentralDirectoryFileHeaderRecord.load(this.data, pos);
			i++;
		}
		return new VirtualZipDataBlock(this.data, this.namePrefix, centralRecords, centralRecordPositions);
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
		ensureOpen();
		return new EntryIterator();
	}

	/**
	 * Returns the number of entries in the ZIP file.
	 * @return the number of entries
	 */
	public int size() {
		ensureOpen();
		return (this.filter != null) ? this.filter.cardinality() : this.nameHash.length;
	}

	/**
	 * Close this jar file, releasing the underlying file if this was the last reference.
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		ensureOpen();
		this.data.close();
	}

	/**
	 * Return the zip comment, if any.
	 * @return the comment or {@code null}
	 */
	public String getComment() {
		ensureOpen();
		return ZipString.readString(this.data, this.commentPos, this.commentLength);
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
		ensureOpen();
		int nameHash = nameHash(namePrefix, name);
		int index = getFirstIndex(nameHash);
		while (index >= 0 && index < this.nameHash.length && this.nameHash[index] == nameHash) {
			if (!isFiltered(index)) {
				long pos = getCentralDirectoryFileHeaderRecordPos(index);
				CentralDirectoryFileHeaderRecord centralRecord = CentralDirectoryFileHeaderRecord
					.loadUnchecked(this.data, pos);
				if (hasName(centralRecord, pos, namePrefix, name)) {
					return new Entry(index, centralRecord);
				}
			}
			index++;
		}
		return null;
	}

	private int nameHash(CharSequence namePrefix, CharSequence name) {
		int nameHash = 0;
		nameHash = (this.namePrefix != null) ? ZipString.hash(nameHash, this.namePrefix, false) : nameHash;
		nameHash = (namePrefix != null) ? ZipString.hash(nameHash, namePrefix, false) : nameHash;
		nameHash = ZipString.hash(nameHash, name, true);
		return nameHash;
	}

	private int getFirstIndex(int nameHash) {
		int index = Arrays.binarySearch(this.nameHash, 0, this.nameHash.length, nameHash);
		if (index < 0) {
			return -1;
		}
		while (index > 0 && this.nameHash[index - 1] == nameHash) {
			index--;
		}
		return index;
	}

	private boolean isFiltered(int index) {
		return this.filter != null && !this.filter.get(index);
	}

	private long getCentralDirectoryFileHeaderRecordPos(int index) {
		return this.centralDirectoryPos + this.relativeCentralDirectoryOffset[index];
	}

	private boolean hasName(CentralDirectoryFileHeaderRecord centralRecord, long pos, CharSequence namePrefix,
			CharSequence name) {
		pos += CentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET;
		short len = centralRecord.fileNameLength();
		for (int i = 0; i < 2; i++) {
			CharSequence prefixToCheck = (i == 0) ? this.namePrefix : namePrefix;
			if (prefixToCheck != null) {
				int startsWith = ZipString.startsWith(this.data, pos, len, prefixToCheck);
				if (startsWith == -1) {
					return false;
				}
				pos += startsWith;
				len -= startsWith;
			}
		}
		return ZipString.matches(this.data, pos, len, name, true);
	}

	/**
	 * Get or compute information based on the {@link ZipContent}.
	 * @param <T> the type to get or compute
	 * @param type the type to get or compute
	 * @param function the function used to compute the information
	 * @return the computed or existing information
	 */
	@SuppressWarnings("unchecked")
	public <T> T getOrCompute(Class<T> type, Function<ZipContent, T> function) {
		return (T) this.info.computeIfAbsent(type, (key) -> function.apply(this));
	}

	private void ensureOpen() {
		this.data.ensureOpen(() -> new IllegalStateException("Zip content has been closed"));
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
	 * Iterator for entries.
	 */
	private final class EntryIterator implements Iterator<Entry> {

		private int cursor = nextCursor(-1);

		@Override
		public boolean hasNext() {
			return this.cursor < ZipContent.this.position.length;
		}

		@Override
		public Entry next() {
			ensureOpen();
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			int index = ZipContent.this.position[this.cursor];
			long pos = getCentralDirectoryFileHeaderRecordPos(index);
			this.cursor = nextCursor(this.cursor);
			return new Entry(index, CentralDirectoryFileHeaderRecord.loadUnchecked(ZipContent.this.data, pos));
		}

		private int nextCursor(int cursor) {
			while (true) {
				cursor++;
				if (cursor >= ZipContent.this.position.length || !isFiltered(ZipContent.this.position[cursor])) {
					return cursor;
				}
			}
		}

	}

	/**
	 * Internal class used to load the zip content create a new {@link ZipContent}
	 * instance.
	 */
	private static class Loader {

		private final FileChannelDataBlock data;

		private final long centralDirectoryPos;

		private int[] nameHash;

		private int[] relativeCentralDirectoryOffset;

		private int[] index;

		private int cursor;

		private Loader(FileChannelDataBlock data, long centralDirectoryPos, int numberOfEntries) {
			this.data = data;
			this.centralDirectoryPos = centralDirectoryPos;
			this.nameHash = new int[numberOfEntries];
			this.relativeCentralDirectoryOffset = new int[numberOfEntries];
			this.index = new int[numberOfEntries];
		}

		private void add(CentralDirectoryFileHeaderRecord centralRecord, long pos) throws IOException {
			int hash = ZipString.hash(this.data, pos + CentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET,
					centralRecord.fileNameLength(), true);
			this.nameHash[this.cursor] = hash;
			this.relativeCentralDirectoryOffset[this.cursor] = (int) ((pos - this.centralDirectoryPos) & 0xFFFFFFFF);
			this.index[this.cursor] = this.cursor;
			this.cursor++;
		}

		private ZipContent finish(long commentPos, long commentLength) {
			int size = this.nameHash.length;
			if (this.cursor != size) {
				throw new IllegalStateException(
						"Missing zip entries (loaded %s, expected %)".formatted(this.cursor, size));
			}
			sort(0, size - 1);
			int[] position = new int[size];
			for (int i = 0; i < size; i++) {
				position[this.index[i]] = i;
			}
			return new ZipContent(this.data, this.centralDirectoryPos, commentPos, commentLength, this.nameHash,
					this.relativeCentralDirectoryOffset, position);
		}

		private void sort(int left, int right) {
			// Quick sort algorithm, uses nameHashCode as the source but sorts all
			// arrays
			if (left < right) {
				int pivot = this.nameHash[left + (right - left) / 2];
				int i = left;
				int j = right;
				while (i <= j) {
					while (this.nameHash[i] < pivot) {
						i++;
					}
					while (this.nameHash[j] > pivot) {
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
			swap(this.nameHash, i, j);
			swap(this.relativeCentralDirectoryOffset, i, j);
			swap(this.index, i, j);
		}

		protected static void swap(int[] array, int i, int j) {
			int temp = array[i];
			array[i] = array[j];
			array[j] = temp;
		}

		static ZipContent load(Source source) throws IOException {
			FileChannelDataBlock data = openDataBlock(source);
			try {
				return load(source, data);
			}
			catch (IOException | RuntimeException ex) {
				data.close();
				throw ex;
			}
		}

		private static ZipContent load(Source source, FileChannelDataBlock data) throws IOException {
			EndOfCentralDirectoryRecord.Located locatedEocd = EndOfCentralDirectoryRecord.load(data);
			EndOfCentralDirectoryRecord eocd = locatedEocd.endOfCentralDirectoryRecord();
			long eocdPos = locatedEocd.pos();
			Zip64EndOfCentralDirectoryLocator zip64Locator = Zip64EndOfCentralDirectoryLocator.find(data, eocdPos);
			Zip64EndOfCentralDirectoryRecord zip64Eocd = Zip64EndOfCentralDirectoryRecord.load(data, zip64Locator);
			data = data.removeFrontMatter(getStartOfZipContent(data, eocd, zip64Eocd));
			long centralDirectoryPos = (zip64Eocd != null) ? zip64Eocd.offsetToStartOfCentralDirectory()
					: eocd.offsetToStartOfCentralDirectory();
			long numberOfEntries = (zip64Eocd != null) ? zip64Eocd.totalNumberOfCentralDirectoryEntries()
					: eocd.totalNumberOfCentralDirectoryEntries();
			if (numberOfEntries > 0xFFFFFFFFL) {
				throw new IllegalStateException("Too many zip entries in " + source);
			}
			Loader loader = new Loader(data, centralDirectoryPos, (int) numberOfEntries & 0xFFFFFFFF);
			long pos = centralDirectoryPos;
			for (int i = 0; i < numberOfEntries; i++) {
				CentralDirectoryFileHeaderRecord centralRecord = CentralDirectoryFileHeaderRecord.load(data, pos);
				loader.add(centralRecord, pos);
				pos += centralRecord.size();
			}
			long commentPos = locatedEocd.pos() + EndOfCentralDirectoryRecord.COMMENT_OFFSET;
			return loader.finish(commentPos, eocd.commentLength());
		}

		/**
		 * Returns the location in the data that the archive actually starts. For most
		 * files the archive data will start at 0, however, it is possible to have
		 * prefixed bytes (often used for startup scripts) at the beginning of the data.
		 * @param data the source data
		 * @return the offset within the data where the archive begins
		 * @throws IOException
		 */
		private static long getStartOfZipContent(FileChannelDataBlock data, EndOfCentralDirectoryRecord eocd,
				Zip64EndOfCentralDirectoryRecord zip64Eocd) throws IOException {
			long specifiedOffsetToStartOfCentralDirectory = (zip64Eocd != null)
					? zip64Eocd.offsetToStartOfCentralDirectory() : eocd.offsetToStartOfCentralDirectory();
			long sizeOfCentralDirectoryAndEndRecords = getSizeOfCentralDirectoryAndEndRecords(eocd, zip64Eocd);
			long actualOffsetToStartOfCentralDirectory = data.size() - sizeOfCentralDirectoryAndEndRecords;
			return actualOffsetToStartOfCentralDirectory - specifiedOffsetToStartOfCentralDirectory;
		}

		private static long getSizeOfCentralDirectoryAndEndRecords(EndOfCentralDirectoryRecord eocd,
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

		private static FileChannelDataBlock openDataBlock(Source source) throws IOException {
			if (source.isNested()) {
				try (ZipContent container = open(source.path())) {
					Entry entry = container.getEntry(source.nestedEntryName());
					if (entry == null) {
						throw new IOException("Nested entry '%s' not found in container zip '%s'"
							.formatted(source.nestedEntryName(), source.path()));
					}
					if (entry.getName().endsWith("/")) {
						throw new IllegalStateException(
								"Nested entry '%s' in container zip '%s' must not be a directory"
									.formatted(source.nestedEntryName(), source.path()));
					}
					if (entry.centralRecord.compressionMethod() != ZipEntry.STORED) {
						throw new IOException("Nested entry '%s' in container zip '%s' must not be compressed"
							.formatted(source.nestedEntryName(), source.path()));
					}
					return entry.openSlice();
				}
			}
			return FileChannelDataBlock.open(source.path());
		}

	}

	/**
	 * A single zip content entry.
	 */
	public class Entry {

		private final int index;

		private final CentralDirectoryFileHeaderRecord centralRecord;

		private String name;

		/**
		 * Create a new {@link Entry} instance.
		 * @param index the index of the entry
		 * @param centralRecord the {@link CentralDirectoryFileHeaderRecord} for the entry
		 */
		Entry(int index, CentralDirectoryFileHeaderRecord centralRecord) {
			this.index = index;
			this.centralRecord = centralRecord;
		}

		/**
		 * Return the index of the entry.
		 * @return the entry index
		 */
		int getIndex() {
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
					+ CentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET;
			return ZipString.startsWith(ZipContent.this.data, pos, this.centralRecord.fileNameLength(), prefix) != -1;
		}

		/**
		 * Return the name of this entry.
		 * @return the entry name
		 */
		public String getName() {
			String name = this.name;
			if (name == null) {
				long pos = getCentralDirectoryFileHeaderRecordPos(this.index)
						+ CentralDirectoryFileHeaderRecord.FILE_NAME_OFFSET;
				name = ZipString.readString(ZipContent.this.data, pos, this.centralRecord.fileNameLength());
				if (ZipContent.this.namePrefix != null) {
					name = name.substring(ZipContent.this.namePrefix.length());
				}
				this.name = name;
			}
			return name;
		}

		/**
		 * Open a new {@link DataBlock} providing access to raw contents of the entry.
		 * <p>
		 * To release resources, the {@link #close()} method of the data block should be
		 * called explicitly or by try-with-resources.
		 * @return the contents of the entry
		 * @throws IOException on I/O error
		 */
		public CloseableDataBlock openContent() throws IOException {
			return openSlice();
		}

		/**
		 * Open a new {@link FileChannelDataBlock} slice providing access to the raw
		 * contents of the entry. The caller is expected to {@link #clone()} the block
		 * when finished.
		 * @return a {@link FileChannelDataBlock} slice
		 * @throws IOException on I/O error
		 */
		FileChannelDataBlock openSlice() throws IOException {
			int pos = this.centralRecord.offsetToLocalHeader();
			checkNotZip64Extended(pos);
			LocalFileHeaderRecord localHeader = LocalFileHeaderRecord.load(ZipContent.this.data, pos);
			int size = this.centralRecord.compressedSize();
			checkNotZip64Extended(size);
			return ZipContent.this.data.openSlice(pos + localHeader.size(), size);
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
			try {
				E result = factory.apply(getName());
				long pos = getCentralDirectoryFileHeaderRecordPos(this.index);
				this.centralRecord.copyTo(ZipContent.this.data, pos, result);
				return result;
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

	}

	/**
	 * Split {@link ZipContent}.
	 *
	 * @param included the subcontent split from the zip
	 * @param remainder the remaining zip content after the subcontent has been filtered
	 */
	public static record Split(ZipContent included, ZipContent remainder) implements Closeable {

		void open() throws IOException {
			this.included.data.open();
			this.remainder.data.open();
		}

		@Override
		public void close() throws IOException {
			this.included.close();
			this.remainder.close();
		}

	}

}
