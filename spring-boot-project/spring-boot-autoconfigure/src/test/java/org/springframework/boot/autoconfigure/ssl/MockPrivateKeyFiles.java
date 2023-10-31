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

import java.security.PrivateKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.util.function.ThrowingFunction;

import static org.mockito.Mockito.mock;

/**
 * Helper used to build {@link PrivateKeyFile} instances with mock content.
 *
 * @author Phillip Webb
 */
public class MockPrivateKeyFiles {

	private final Instant now;

	private final List<File> files = new ArrayList<>();

	private MockPrivateKeyFiles(Instant now) {
		this.now = now;
	}

	File add(String filename) {
		File file = new File(filename);
		this.files.add(file);
		return file;
	}

	private List<PrivateKeyFile> create() {
		return this.files.stream().map(ThrowingFunction.of(File::create)).toList();
	}

	static List<PrivateKeyFile> create(Consumer<MockPrivateKeyFiles> files) {
		return create(Instant.now(), files);
	}

	static List<PrivateKeyFile> create(Instant now, Consumer<MockPrivateKeyFiles> files) {
		MockPrivateKeyFiles mockPrivateKeyFiles = new MockPrivateKeyFiles(now);
		files.accept(mockPrivateKeyFiles);
		return mockPrivateKeyFiles.create();
	}

	static List<PrivateKeyFile> createFromPrivateKeys(List<PrivateKey> privateKeys) {
		return createFromPrivateKeys(Instant.now(), privateKeys);
	}

	static List<PrivateKeyFile> createFromPrivateKeys(Instant now, List<PrivateKey> privateKeys) {
		List<PrivateKeyFile> privateKeyFiles = new ArrayList<>();
		for (int i = 0; i < privateKeys.size(); i++) {
			privateKeyFiles.add(new PrivateKeyFile(MockPath.create("pk" + i, now), privateKeys.get(i)));
		}
		return List.copyOf(privateKeyFiles);
	}

	class File {

		private final String name;

		private Instant creationTime;

		private File(String name) {
			this.name = name;
			this.creationTime = MockPrivateKeyFiles.this.now;
		}

		File withCreationTimeOffset(int creationTimeOffset) {
			this.creationTime = MockPrivateKeyFiles.this.now.plusSeconds(creationTimeOffset);
			return this;
		}

		private PrivateKeyFile create() {
			return new PrivateKeyFile(MockPath.create(this.name, this.creationTime), createPrivateKey());
		}

		private PrivateKey createPrivateKey() {
			return mock(PrivateKey.class);
		}

	}

}
