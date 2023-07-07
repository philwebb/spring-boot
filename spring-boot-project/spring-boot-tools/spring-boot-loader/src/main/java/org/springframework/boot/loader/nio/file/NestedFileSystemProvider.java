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

package org.springframework.boot.loader.nio.file;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * @author pwebb
 */
public class NestedFileSystemProvider extends FileSystemProvider {

	@Override
	public FileSystem newFileSystem(Path path, Map<String, ?> env) throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public AsynchronousFileChannel newAsynchronousFileChannel(Path path, Set<? extends OpenOption> options,
			ExecutorService executor, FileAttribute<?>... attrs) throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public String getScheme() {
		return "nested";
	}

	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
		// open connection and get the nested entry
		// get or open zip for entries
		// either point at correct part of file or create a virtual FS
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public FileSystem getFileSystem(URI uri) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Path getPath(URI uri) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public void delete(Path path) throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public void copy(Path source, Path target, CopyOption... options) throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public boolean isSameFile(Path path, Path path2) throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public boolean isHidden(Path path) throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public FileStore getFileStore(Path path) throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
			throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	// FIXME will need to create a virtual zip file for folders and a file slice for
	// nested uncompressed jars

}
