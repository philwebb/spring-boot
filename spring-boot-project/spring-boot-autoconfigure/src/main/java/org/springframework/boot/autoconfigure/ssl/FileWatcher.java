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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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

	private static final Kind<?>[] WATCHED_EVENTS = new Kind<?>[] { StandardWatchEventKinds.ENTRY_CREATE,
			StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE };

	private final Duration quietPeriod;

	private final Object lock = new Object();

	private volatile WatcherThread thread;

	FileWatcher(Duration quietPeriod) {
		Assert.notNull(quietPeriod, "QuietPeriod must not be null");
		this.quietPeriod = quietPeriod;
	}

	void watch(Set<Path> paths, Runnable action) {
		Assert.notNull(paths, "Paths must not be null");
		Assert.notNull(action, "Action must not be null");
		if (paths.isEmpty()) {
			return;
		}
		synchronized (this.lock) {
			if (this.thread == null) {
				this.thread = new WatcherThread();
				this.thread.start();
			}
			try {
				this.thread.register(new Registration(paths, action));
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Failed to register paths for watching: " + paths, ex);
			}
		}
	}

	@Override
	public void close() throws Exception {
		synchronized (this.lock) {
			if (this.thread != null) {
				this.thread.interrupt();
				try {
					this.thread.join();
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				this.thread = null;
			}
		}
	}

	private class WatcherThread extends Thread {

		private final Map<WatchKey, List<Registration>> registrations = new ConcurrentHashMap<>();

		private void register(Registration registration) throws IOException {
			for (Path path : registration.paths()) {
				if (!Files.isRegularFile(path) && !Files.isDirectory(path)) {
					throw new IOException("'%s' is neither a file nor a directory".formatted(path));
				}
				Path directory = Files.isDirectory(path) ? path : path.getParent();
				WatchKey watchKey = register(directory);
				this.registrations.computeIfAbsent(watchKey, (key) -> new CopyOnWriteArrayList<>()).add(registration);
			}
		}

		private WatchKey register(Path directory) throws IOException {
			logger.debug(LogMessage.format("Registering '%s'", directory));
			return directory.register(this.watchService, WATCHED_EVENTS);
		}

	}

	private record Registration(Set<Path> paths, Runnable action) {

		boolean affectsFile(Path file) {
			return this.paths.contains(file) || isInDirectories(file);
		}

		private boolean isInDirectories(Path file) {
			for (Path path : this.paths) {
				if (Files.isDirectory(path) && file.startsWith(path)) {
					return true;
				}
			}
			return false;
		}
	}

	// @formatter:off

//
//	private volatile WatchService watchService;
//
//	private Thread thread;
//
//	private boolean running = false;
//
//	FileWatcher(Duration quietPeriod) {
//		Assert.notNull(quietPeriod, "QuietPeriod must not be null");
//		this.quietPeriod = quietPeriod;
//	}
//
//	void watch(Set<Path> paths, Runnable callback) {
//		Assert.notNull(paths, "Paths must not be null");
//		Assert.notNull(callback, "Callback must not be null");
//		if (paths.isEmpty()) {
//			return;
//		}
//		startIfNecessary();
//		try {
//			registerWatchables(new Registration(paths, callback));
//		}
//		catch (IOException ex) {
//			throw new UncheckedIOException("Failed to register paths for watching: " + paths, ex);
//		}
//	}
//
//	private void startIfNecessary() {
//		synchronized (this.lifecycleLock) {
//			if (this.running) {
//				return;
//			}
//			CountDownLatch started = new CountDownLatch(1);
//			this.thread = new Thread(() -> this.threadMain(started));
//			this.thread.setName("ssl-bundle-watcher");
//			this.thread.setDaemon(true);
//			this.thread.setUncaughtExceptionHandler(this::onThreadException);
//			this.running = true;
//			this.thread.start();
//			try {
//				started.await();
//			}
//			catch (InterruptedException ex) {
//				Thread.currentThread().interrupt();
//			}
//		}
//	}
//
//
//	private WatchKey register(Path directory) throws IOException {
//		logger.debug(LogMessage.format("Registering '%s'", directory));
//		return directory.register(this.watchService, WATCHED_EVENTS);
//	}
//
//	private void threadMain(CountDownLatch started) {
//		logger.debug("Watch thread started");
//		try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
//			this.watchService = watcher;
//			started.countDown();
//			Map<Registration, List<Change>> accumulatedChanges = new HashMap<>();
//			while (this.running) {
//				try {
//					long timeout = this.quietPeriod.toMillis();
//					WatchKey key = watcher.poll(timeout, TimeUnit.MILLISECONDS);
//					if (key == null) {
//						// WatchService returned without any changes
//						if (!accumulatedChanges.isEmpty()) {
//							// We have queued changes, that means there were no changes
//							// since the quiet period
//							fireCallback(accumulatedChanges);
//							accumulatedChanges.clear();
//						}
//					}
//					else {
//						accumulateChanges(key, accumulatedChanges);
//					}
//				}
//				catch (InterruptedException ex) {
//					Thread.currentThread().interrupt();
//				}
//			}
//			logger.debug("Watch thread stopped");
//		}
//		catch (IOException ex) {
//			throw new UncheckedIOException(ex);
//		}
//	}
//
//	private void accumulateChanges(WatchKey key, Map<Registration, List<Change>> accumulatedChanges) {
//		List<Registration> registrations = this.registrations.get(key);
//		Path directory = (Path) key.watchable();
//		for (WatchEvent<?> event : key.pollEvents()) {
//			Path file = directory.resolve((Path) event.context());
//			for (Registration registration : registrations) {
//				if (registration.affectsFile(file)) {
//					accumulatedChanges.computeIfAbsent(registration, (ignore) -> new ArrayList<>())
//						.add(new Change(file, event.kind()));
//				}
//			}
//		}
//		key.reset();
//	}
//
//	private void fireCallback(Map<Registration, List<Change>> accumulatedChanges) {
//		for (Entry<Registration, List<Change>> entry : accumulatedChanges.entrySet()) {
//			if (!entry.getValue().isEmpty()) {
//				entry.getKey().callback().run();
//			}
//		}
//	}
//
//	private void onThreadException(Thread thread, Throwable throwable) {
//		logger.error("Uncaught exception in file watcher thread", throwable);
//	}
//
//	@Override
//	public void close() {
//		synchronized (this.lifecycleLock) {
//			if (!this.running) {
//				return;
//			}
//			this.running = false;
//			this.thread.interrupt();
//			try {
//				this.thread.join();
//			}
//			catch (InterruptedException ex) {
//				Thread.currentThread().interrupt();
//			}
//			this.thread = null;
//			this.watchService = null;
//			this.registrations.clear();
//		}
//	}
//
//	private record Registration(Set<Path> paths, Runnable callback) {
//
//		boolean affectsFile(Path file) {
//			return this.paths.contains(file) || isInDirectories(file);
//		}
//
//		private boolean isInDirectories(Path file) {
//			for (Path path : this.paths) {
//				if (Files.isDirectory(path) && file.startsWith(path)) {
//					return true;
//				}
//			}
//			return false;
//		}
//	}
//
//	record Change(Path path, Kind<?> kind) {
//	}

	// @formatter:on

}
