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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;

/**
 * Watches files and directories and triggers a callback on change.
 *
 * @author Moritz Halbritter
 */
class FileWatcher implements AutoCloseable {

	private static final Log logger = LogFactory.getLog(FileWatcher.class);

	private final String threadName;

	private final Duration quietPeriod;

	private final Object lifecycleLock = new Object();

	private final Map<WatchKey, List<Registration>> registrations = new ConcurrentHashMap<>();

	private volatile WatchService watchService;

	private Thread thread;

	private boolean running = false;

	FileWatcher(String threadName, Duration quietPeriod) {
		Assert.notNull(threadName, "threadName must not be null");
		Assert.notNull(quietPeriod, "quietPeriod must not be null");
		this.threadName = threadName;
		this.quietPeriod = quietPeriod;
	}

	void watch(Set<Path> paths, Callback callback) {
		Assert.notNull(callback, "callback must not be null");
		Assert.notNull(paths, "paths must not be null");
		if (paths.isEmpty()) {
			return;
		}
		startIfNecessary();
		try {
			registerWatchables(callback, paths, this.watchService);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to register paths for watching: " + paths, ex);
		}
	}

	void stop() {
		synchronized (this.lifecycleLock) {
			if (!this.running) {
				return;
			}
			this.running = false;
			this.thread.interrupt();
			try {
				this.thread.join();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			this.thread = null;
			this.watchService = null;
			this.registrations.clear();
		}
	}

	private void startIfNecessary() {
		synchronized (this.lifecycleLock) {
			if (this.running) {
				return;
			}
			CountDownLatch started = new CountDownLatch(1);
			this.thread = new Thread(() -> this.threadMain(started));
			this.thread.setName(this.threadName);
			this.thread.setDaemon(true);
			this.thread.setUncaughtExceptionHandler(this::onThreadException);
			this.running = true;
			this.thread.start();
			try {
				started.await();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void threadMain(CountDownLatch started) {
		logger.debug("Watch thread started");
		try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
			this.watchService = watcher;
			started.countDown();
			Map<Registration, List<Change>> accumulatedChanges = new HashMap<>();
			while (this.running) {
				try {
					WatchKey key = watcher.poll(this.quietPeriod.toMillis(), TimeUnit.MILLISECONDS);
					if (key == null) {
						// WatchService returned without any changes
						if (!accumulatedChanges.isEmpty()) {
							// We have queued changes, that means there were no changes
							// since the quiet period
							fireCallback(accumulatedChanges);
							accumulatedChanges.clear();
						}
					}
					else {
						accumulateChanges(key, accumulatedChanges);
					}
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
			logger.debug("Watch thread stopped");
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private void accumulateChanges(WatchKey key, Map<Registration, List<Change>> accumulatedChanges)
			throws IOException {
		List<Registration> registrations = this.registrations.get(key);
		Path directory = (Path) key.watchable();
		for (WatchEvent<?> event : key.pollEvents()) {
			Path file = directory.resolve((Path) event.context());
			for (Registration registration : registrations) {
				if (registration.affectsFile(file)) {
					accumulatedChanges.computeIfAbsent(registration, (ignore) -> new ArrayList<>())
						.add(new Change(file, Type.from(event.kind())));
				}
			}
		}
		key.reset();
	}

	private void fireCallback(Map<Registration, List<Change>> accumulatedChanges) {
		for (Entry<Registration, List<Change>> entry : accumulatedChanges.entrySet()) {
			Changes changes = new Changes(entry.getValue());
			if (!changes.isEmpty()) {
				entry.getKey().callback().onChange(changes);
			}
		}
	}

	private void onThreadException(Thread thread, Throwable throwable) {
		logger.error("Uncaught exception in file watcher thread", throwable);
	}

	private void registerWatchables(Callback callback, Set<Path> paths, WatchService watchService) throws IOException {
		Set<WatchKey> watchKeys = new HashSet<>();
		Set<Path> directories = new HashSet<>();
		Set<Path> files = new HashSet<>();
		for (Path path : paths) {
			Path realPath = path.toRealPath();
			if (Files.isDirectory(realPath)) {
				directories.add(realPath);
				watchKeys.add(registerDirectory(realPath, watchService));
			}
			else if (Files.isRegularFile(realPath)) {
				files.add(realPath);
				watchKeys.add(registerFile(realPath, watchService));
			}
			else {
				throw new IOException("'%s' is neither a file nor a directory".formatted(realPath));
			}
		}
		Registration registration = new Registration(callback, directories, files);
		for (WatchKey watchKey : watchKeys) {
			this.registrations.computeIfAbsent(watchKey, (ignore) -> new CopyOnWriteArrayList<>()).add(registration);
		}
	}

	private WatchKey registerFile(Path file, WatchService watchService) throws IOException {
		return register(file.getParent(), watchService);
	}

	private WatchKey registerDirectory(Path directory, WatchService watchService) throws IOException {
		return register(directory, watchService);
	}

	private WatchKey register(Path directory, WatchService watchService) throws IOException {
		logger.debug(LogMessage.format("Registering '%s'", directory));
		return directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
				StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
	}

	@Override
	public void close() {
		stop();
	}

	private record Registration(Callback callback, Set<Path> directories, Set<Path> files) {
		boolean affectsFile(Path file) {
			return this.files.contains(file) || isInDirectories(file);
		}

		private boolean isInDirectories(Path file) {
			for (Path directory : this.directories) {
				if (file.startsWith(directory)) {
					return true;
				}
			}
			return false;
		}
	}

	enum Type {

		CREATE, MODIFY, DELETE;

		private static Type from(Kind<?> kind) {
			if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
				return CREATE;
			}
			if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
				return DELETE;
			}
			if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
				return MODIFY;
			}
			throw new IllegalArgumentException("Unknown kind: " + kind);
		}

	}

	record Change(Path path, Type type) {
	}

	static class Changes implements Iterable<Change> {

		private final List<Change> changes;

		Changes(List<Change> changes) {
			this.changes = changes;
		}

		@Override
		public Iterator<Change> iterator() {
			return this.changes.iterator();
		}

		boolean isEmpty() {
			return this.changes.isEmpty();
		}

	}

	@FunctionalInterface
	interface Callback {

		void onChange(Changes changes);

	}

}
