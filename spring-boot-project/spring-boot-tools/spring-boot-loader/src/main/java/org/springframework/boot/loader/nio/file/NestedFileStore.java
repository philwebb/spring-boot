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
import java.nio.file.Files;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

import org.springframework.boot.loader.net.protocol.nested.NestedLocation;

/**
 * {@link FileStore} implementation for {@link NestedLocation nested} jar files.
 *
 * @author Phillip Webb
 * @see NestedFileSystemProvider
 */
class NestedFileStore extends FileStore {

	private final NestedFileSystem fileSystem;

	private FileStore jarPathFileStore;

	NestedFileStore(NestedFileSystem fileSystem) throws IOException {
		this.fileSystem = fileSystem;
		this.jarPathFileStore = Files.getFileStore(fileSystem.getJarPath());
	}

	@Override
	public String name() {
		return this.fileSystem.toString();
	}

	@Override
	public String type() {
		return "nestedfs";
	}

	@Override
	public boolean isReadOnly() {
		return this.fileSystem.isReadOnly();
	}

	@Override
	public long getTotalSpace() throws IOException {
		return 0;
	}

	@Override
	public long getUsableSpace() throws IOException {
		return 0;
	}

	@Override
	public long getUnallocatedSpace() throws IOException {
		return 0;
	}

	@Override
	public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
		return this.jarPathFileStore.supportsFileAttributeView(type);
	}

	@Override
	public boolean supportsFileAttributeView(String name) {
		return this.jarPathFileStore.supportsFileAttributeView(name);
	}

	@Override
	public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
		return this.jarPathFileStore.getFileStoreAttributeView(type);
	}

	@Override
	public Object getAttribute(String attribute) throws IOException {
		return this.jarPathFileStore.getAttribute(attribute);
	}

}
