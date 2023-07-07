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
import java.util.Set;

/**
 * @author pwebb
 */
public class NestedFileSystem extends FileSystem {

	@Override
	public FileSystemProvider provider() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public void close() throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public boolean isOpen() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public boolean isReadOnly() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public String getSeparator() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Iterable<Path> getRootDirectories() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Iterable<FileStore> getFileStores() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Set<String> supportedFileAttributeViews() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Path getPath(String first, String... more) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public PathMatcher getPathMatcher(String syntaxAndPattern) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public WatchService newWatchService() throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
