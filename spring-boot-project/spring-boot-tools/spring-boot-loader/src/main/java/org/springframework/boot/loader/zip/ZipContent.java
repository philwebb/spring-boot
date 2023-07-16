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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.BitSet;
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
 * or by try-with-resources.
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

	private ZipContent(FileChannelDataBlock data, long centralDirectoryPos, long commentPos, long commentLength,
			int[] nameHash, int[] relativeCentralDirectoryOffset, int[] position) {
		this.data = data;
		this.centralDirectoryPos = centralDirectoryPos;
		this.commentPos = commentPos;
		this.commentLength = commentLength;
		this.nameHash = nameHash;
		this.relativeCentralDirectoryOffset = relativeCentralDirectoryOffset;
		this.position = position;
	}

	/**
	 * Split the zip based based the named directory entry. This method will return two
	 * new {@link ZipContent} instances, one containing the included entries and one
	 * containing the remaining entries. Included entries will named with the directory
	 * prefix removed. The caller is responsible for {@link #close() closing} the returned
	 * instance, this {@link ZipContent} will be closed after split and should not be used
	 * again.
	 * @param directoryName the name of the directory that should be split off
	 * @return the split zip content
	 * @throws IOException on I/O error
	 */
	public Split split(String directoryName) throws IOException {
		Entry entry = getEntry(directoryName);
		if (entry == null || !entry.isDirectory()) {
			throw new IllegalStateException("No directory entry '%s' found".formatted(directoryName));
		}
		BitSet included = new BitSet(size());
		BitSet remainder = new BitSet(size());
		for (int i = 0; i < this.nameHash.length; i++) {
			CentralDirectoryFileHeaderRecord headerRecord = loadCentralDirectoryFileHeaderRecord(i);
			int startsWith = ZipString.startsWith(this.data, headerRecord.fileNamePos(), headerRecord.fileNameLength(),
					entry.getName());

		}
		return null;
	}

	/**
	 * Return the data block containing the zip data. For container zip files, this may be
	 * smaller than the original file since additional bytes are permitted at the front of
	 * a zip file. For nested zip files, this will be only the contents of the nest zip.
	 * <p>
	 * Data contents must not be accessed after calling {@link ZipContent#close()} .
	 * @return the zip data
	 */
	public DataBlock getData() {
		return this.data;
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
		return this.nameHash.length;
	}

	/**
	 * Open another connection to the underling data block.
	 */
	private void open() throws IOException {
		this.data.open();
	}

	/**
	 * Close this jar file, releasing the underlying file if this was the last reference.
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
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
	 * Return the entry with the given name.
	 * @param name the name of the entry to find
	 * @return the entry or {@code null}
	 */
	Entry getEntry(CharSequence name) {
		ensureOpen();
		int nameHash = ZipString.hash(name, true);
		int index = getFirstIndex(nameHash);
		while (index >= 0 && index < this.nameHash.length && this.nameHash[index] == nameHash) {
			CentralDirectoryFileHeaderRecord candidate = loadCentralDirectoryFileHeaderRecord(index);
			if (hasName(candidate, name)) {
				return new Entry(candidate);
			}
			index++;
		}
		return null;
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

	private CentralDirectoryFileHeaderRecord loadCentralDirectoryFileHeaderRecord(int index) {
		try {
			long pos = this.centralDirectoryPos + this.relativeCentralDirectoryOffset[index];
			return CentralDirectoryFileHeaderRecord.load(this.data, pos);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private boolean hasName(CentralDirectoryFileHeaderRecord centralDirectoryFileHeaderRecord, CharSequence name) {
		try {
			long pos = centralDirectoryFileHeaderRecord.fileNamePos();
			short size = centralDirectoryFileHeaderRecord.fileNameLength();
			return ZipString.matches(this.data, pos, size, name, true);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private void ensureOpen() {
		this.data.ensureOpen(() -> new IllegalStateException("Zip content has been closed"));
	}

	/**
	 * Open {@link ZipContent} from the specified path. The resulting {@link ZipContent}
	 * <em>must</em> be {@link #close() closed} by the called.
	 * @param zip the zip path
	 * @return a {@link ZipContent} instance
	 * @throws IOException on I/O error
	 */
	public static ZipContent open(Path zip) throws IOException {
		return open(new Source(zip.toAbsolutePath(), null));
	}

	/**
	 * Open nested {@link ZipContent} from the specified path. The resulting
	 * {@link ZipContent} <em>must</em> be {@link #close() closed} by the called.
	 * @param containerZip the container zip path
	 * @param nestedEntryName the nested entry name to open
	 * @return a {@link ZipContent} instance
	 * @throws IOException on I/O error
	 */
	public static ZipContent open(Path containerZip, String nestedEntryName) throws IOException {
		return open(new Source(containerZip.toAbsolutePath(), nestedEntryName));
	}

	private static ZipContent open(Source source) throws IOException {
		ZipContent zipContent = cache.get(source);
		if (zipContent != null) {
			debug.log("Opening existing cached zip content for %s", zipContent);
			zipContent.open();
			return zipContent;
		}
		debug.log("Loading zip content from %s", source);
		zipContent = Loader.load(source);
		ZipContent previouslyCached = cache.putIfAbsent(source, zipContent);
		if (previouslyCached != null) {
			debug.log("Closing and zip content from %s since cache was populated from another thread", source);
			zipContent.close();
			previouslyCached.open();
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

		boolean isNested() {
			return this.nestedEntryName != null;
		}

	}

	/**
	 * Iterator for entries.
	 */
	private final class EntryIterator implements Iterator<Entry> {

		private int cursor = 0;

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
			this.cursor++;
			return new Entry(loadCentralDirectoryFileHeaderRecord(index));
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

		private void add(CentralDirectoryFileHeaderRecord record) throws IOException {
			this.nameHash[this.cursor] = ZipString.hash(this.data, record.fileNamePos(), record.fileNameLength(), true);
			this.relativeCentralDirectoryOffset[this.cursor] = (int) (record.pos() - this.centralDirectoryPos);
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
			int[] positions = new int[size];
			for (int i = 0; i < size; i++) {
				positions[this.index[i]] = i;
			}
			return new ZipContent(this.data, this.centralDirectoryPos, commentPos, commentLength, this.nameHash,
					this.relativeCentralDirectoryOffset, positions);
		}

		private void sort(int left, int right) {
			// Quick sort algorithm, uses nameHashCode as the source but sorts all arrays
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
				EndOfCentralDirectoryRecord zipEocd = EndOfCentralDirectoryRecord.load(data);
				Zip64EndOfCentralDirectoryLocator zip64Locator = Zip64EndOfCentralDirectoryLocator.find(data, zipEocd);
				Zip64EndOfCentralDirectoryRecord zip64Eocd = Zip64EndOfCentralDirectoryRecord.load(data, zip64Locator);
				data = data.removeFrontMatter(getStartOfZipContent(data, zipEocd, zip64Eocd));
				long centralDirectoryPos = (zip64Eocd != null) ? zip64Eocd.offsetToStartOfCentralDirectory()
						: zipEocd.offsetToStartOfCentralDirectory();
				long numberOfEntries = (zip64Eocd != null) ? zip64Eocd.totalNumberOfCentralDirectoryEntries()
						: zipEocd.totalNumberOfCentralDirectoryEntries();
				if (numberOfEntries > Integer.MAX_VALUE) {
					throw new IllegalStateException("Too many zip entries in " + source);
				}
				Loader loader = new Loader(data, centralDirectoryPos, (int) numberOfEntries);
				long pos = centralDirectoryPos;
				for (int i = 0; i < numberOfEntries; i++) {
					CentralDirectoryFileHeaderRecord record = CentralDirectoryFileHeaderRecord.load(data, pos);
					loader.add(record);
					pos += record.size();
				}
				return loader.finish(zipEocd.commentPos(), zipEocd.commentLength());
			}
			catch (IOException | RuntimeException ex) {
				data.close();
				throw ex;
			}
		}

		/**
		 * Returns the location in the data that the archive actually starts. For most
		 * files the archive data will start at 0, however, it is possible to have
		 * prefixed bytes (often used for startup scripts) at the beginning of the data.
		 * @param data the source data
		 * @return the offset within the data where the archive begins
		 * @throws IOException
		 */
		private static long getStartOfZipContent(FileChannelDataBlock data, EndOfCentralDirectoryRecord zipEocd,
				Zip64EndOfCentralDirectoryRecord zip64Eocd) throws IOException {
			long specifiedOffsetToStartOfCentralDirectory = (zip64Eocd != null)
					? zip64Eocd.offsetToStartOfCentralDirectory() : zipEocd.offsetToStartOfCentralDirectory();
			long sizeOfCentralDirectoryAndEndRecords = getSizeOfCentralDirectoryAndEndRecords(zipEocd, zip64Eocd);
			long actualOffsetToStartOfCentralDirectory = data.size() - sizeOfCentralDirectoryAndEndRecords;
			return actualOffsetToStartOfCentralDirectory - specifiedOffsetToStartOfCentralDirectory;
		}

		private static long getSizeOfCentralDirectoryAndEndRecords(EndOfCentralDirectoryRecord zipEocd,
				Zip64EndOfCentralDirectoryRecord zip64Eocd) {
			long result = 0;
			result += zipEocd.size();
			if (zip64Eocd != null) {
				result += Zip64EndOfCentralDirectoryLocator.SIZE;
				result += zip64Eocd.size();
			}
			result += (zip64Eocd != null) ? zip64Eocd.sizeOfCentralDirectory() : zipEocd.sizeOfCentralDirectory();
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
						throw new IllegalStateException("Not yet implemented");
					}
					if (entry.record.compressionMethod() != ZipEntry.STORED) {
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

		private final CentralDirectoryFileHeaderRecord record;

		private String name;

		Entry(CentralDirectoryFileHeaderRecord record) {
			this.record = record;
		}

		/**
		 * Return {@code true} if this is a directory entry.
		 * @return if the entry is a directory
		 */
		public boolean isDirectory() {
			return getName().endsWith("/");
		}

		/**
		 * Return the name of this entry.
		 * @return the entry name
		 */
		public String getName() {
			String name = this.name;
			if (name == null) {
				name = ZipString.readString(ZipContent.this.data, this.record.fileNamePos(),
						this.record.fileNameLength());
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
			int localHeaderPos = this.record.offsetToLocalHeader();
			checkNotZip64Extended(localHeaderPos);
			LocalFileHeaderRecord localHeader = LocalFileHeaderRecord.load(ZipContent.this.data, localHeaderPos);
			int size = this.record.compressedSize();
			checkNotZip64Extended(size);
			return ZipContent.this.data.openSlice(localHeaderPos + localHeader.size(), size);
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
				this.record.copyTo(ZipContent.this.data, result);
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

		@Override
		public void close() throws IOException {
			this.included.close();
			this.remainder.close();
		}

	}

}
