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

package org.springframework.boot.autoconfigure.ssl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.time.Instant;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Helper used to return a mock {@link Path}.
 *
 * @author Phillip Webb
 */
final class MockPath {

	private MockPath() {
	}

	static Path create(String name) {
		return create(name, Instant.now());
	}

	static Path create(String name, Instant creationTime) {
		try {
			Path path = mock(Path.class);
			FileSystem fileSystem = mock(FileSystem.class);
			FileSystemProvider provider = mock(FileSystemProvider.class);
			BasicFileAttributes attributes = mock(BasicFileAttributes.class);
			given(path.toString()).willReturn(name);
			given(path.getFileSystem()).willReturn(fileSystem);
			given(path.getFileName()).willReturn(path);
			given(fileSystem.provider()).willReturn(provider);
			given(provider.readAttributes(path, BasicFileAttributes.class)).willReturn(attributes);
			given(attributes.creationTime()).willReturn(FileTime.from(creationTime));
			given(attributes.isRegularFile()).willReturn(true);
			return path;
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
