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

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.utils.collections.ConcurrentHashSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.autoconfigure.ssl.FileWatcher.Callback;
import org.springframework.boot.autoconfigure.ssl.FileWatcher.Change;
import org.springframework.boot.autoconfigure.ssl.FileWatcher.Changes;
import org.springframework.boot.autoconfigure.ssl.FileWatcher.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link FileWatcher}.
 *
 * @author Moritz Halbritter
 */
class FileWatcherTests {

	private FileWatcher fileWatcher;

	@BeforeEach
	void setUp() {
		this.fileWatcher = new FileWatcher("filewatcher-test-", Duration.ofMillis(10));
	}

	@AfterEach
	void tearDown() {
		this.fileWatcher.close();
	}

	@Test
	void shouldTriggerOnFileCreation(@TempDir Path tempDir) throws Exception {
		Path newFile = tempDir.resolve("new-file.txt");
		WaitingCallback callback = new WaitingCallback();
		this.fileWatcher.watch(Set.of(tempDir), callback);
		Files.createFile(newFile);
		Set<Change> changes = callback.waitForChanges();
		assertThatHasChanges(changes, new Change(newFile, Type.CREATE));
	}

	@Test
	void shouldTriggerOnFileDeletion(@TempDir Path tempDir) throws Exception {
		Path deletedFile = tempDir.resolve("deleted-file.txt");
		Files.createFile(deletedFile);
		WaitingCallback callback = new WaitingCallback();
		this.fileWatcher.watch(Set.of(tempDir), callback);
		Files.delete(deletedFile);
		Set<Change> changes = callback.waitForChanges();
		assertThatHasChanges(changes, new Change(deletedFile, Type.DELETE));
	}

	@Test
	void shouldTriggerOnFileModification(@TempDir Path tempDir) throws Exception {
		Path deletedFile = tempDir.resolve("modified-file.txt");
		Files.createFile(deletedFile);
		WaitingCallback callback = new WaitingCallback();
		this.fileWatcher.watch(Set.of(tempDir), callback);
		Files.writeString(deletedFile, "Some content");
		Set<Change> changes = callback.waitForChanges();
		assertThatHasChanges(changes, new Change(deletedFile, Type.MODIFY));
	}

	@Test
	void shouldWatchFile(@TempDir Path tempDir) throws Exception {
		Path watchedFile = tempDir.resolve("watched.txt");
		Files.createFile(watchedFile);
		WaitingCallback callback = new WaitingCallback();
		this.fileWatcher.watch(Set.of(watchedFile), callback);
		Files.writeString(watchedFile, "Some content");
		Set<Change> changes = callback.waitForChanges();
		assertThatHasChanges(changes, new Change(watchedFile, Type.MODIFY));
	}

	@Test
	void shouldIgnoreNotWatchedFiles(@TempDir Path tempDir) throws Exception {
		Path watchedFile = tempDir.resolve("watched.txt");
		Path notWatchedFile = tempDir.resolve("not-watched.txt");
		Files.createFile(watchedFile);
		Files.createFile(notWatchedFile);
		WaitingCallback callback = new WaitingCallback();
		this.fileWatcher.watch(Set.of(watchedFile), callback);
		Files.writeString(notWatchedFile, "Some content");
		callback.expectNoChanges();
	}

	@Test
	void shouldFailIfDirectoryOrFileDoesntExist(@TempDir Path tempDir) {
		Path directory = tempDir.resolve("dir1");
		assertThatThrownBy(() -> this.fileWatcher.watch(Set.of(directory), new WaitingCallback()))
			.isInstanceOf(UncheckedIOException.class)
			.hasMessageMatching("Failed to register paths for watching: \\[.+/dir1]");
	}

	@Test
	void shouldNotFailIfDirectoryIsRegisteredMultipleTimes(@TempDir Path tempDir) {
		WaitingCallback callback = new WaitingCallback();
		assertThatCode(() -> {
			this.fileWatcher.watch(Set.of(tempDir), callback);
			this.fileWatcher.watch(Set.of(tempDir), callback);
		}).doesNotThrowAnyException();
	}

	@Test
	void shouldNotFailIfStoppedMultipleTimes(@TempDir Path tempDir) {
		WaitingCallback callback = new WaitingCallback();
		this.fileWatcher.watch(Set.of(tempDir), callback);
		assertThatCode(() -> {
			this.fileWatcher.stop();
			this.fileWatcher.stop();
		}).doesNotThrowAnyException();
	}

	private void assertThatHasChanges(Set<Change> candidates, Change... changes) {
		assertThat(candidates).containsAll(Arrays.asList(changes));
	}

	private static class WaitingCallback implements Callback {

		private final CountDownLatch latch = new CountDownLatch(1);

		private final Set<Change> changes = new ConcurrentHashSet<>();

		@Override
		public void onChange(Changes changes) {
			for (Change change : changes) {
				this.changes.add(change);
			}
			this.latch.countDown();
		}

		Set<Change> waitForChanges() throws InterruptedException {
			if (!this.latch.await(10, TimeUnit.SECONDS)) {
				fail("Timeout while waiting for changes");
			}
			return this.changes;
		}

		void expectNoChanges() throws InterruptedException {
			if (!this.latch.await(100, TimeUnit.MILLISECONDS)) {
				return;
			}
			assertThat(this.changes).isEmpty();
		}

	}

}
