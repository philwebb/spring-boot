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
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Set;

/**
 * @author Phillip Webb
 * @see NestedFileSystemProvider
 */
class NestedFileSystem extends FileSystem {

	private static final Set<String> SUPPORTED_FILE_ATTRIBUTE_VIEWS = Set.of("basic");

	private final NestedFileSystemProvider provider;

	private final Path jarPath;

	private volatile boolean closed;

	NestedFileSystem(NestedFileSystemProvider provider, Path jarPath) {
		this.provider = provider;
		this.jarPath = jarPath;
	}

	@Override
	public FileSystemProvider provider() {
		return this.provider;
	}

	Path jarPath() {
		return this.jarPath;
	}

	@Override
	public void close() throws IOException {
		if (this.closed) {
			return;
		}
		this.closed = true;
	}

	@Override
	public boolean isOpen() {
		return !this.closed;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public String getSeparator() {
		return "";
	}

	@Override
	public Iterable<Path> getRootDirectories() {
		return Collections.emptySet();
	}

	@Override
	public Iterable<FileStore> getFileStores() {
		return Collections.emptySet();
	}

	@Override
	public Set<String> supportedFileAttributeViews() {
		return SUPPORTED_FILE_ATTRIBUTE_VIEWS;
	}

	@Override
	public Path getPath(String first, String... more) {
		if (first == null || first.isBlank() || more.length != 0) {
			throw new IllegalArgumentException("Nested paths must contain a single element");
		}
		return new NestedPath(this, first);
	}

	@Override
	public PathMatcher getPathMatcher(String syntaxAndPattern) {
		throw new UnsupportedOperationException("Nested paths do not support path matchers");
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		throw new UnsupportedOperationException("Nested paths do not have a user principal lookup service");
	}

	@Override
	public WatchService newWatchService() throws IOException {
		throw new UnsupportedOperationException("Nested paths do not support the WacherService");
	}

}
