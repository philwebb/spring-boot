/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.developertools.filewatch;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;

/**
 * Watches specific folders for file changes.
 *
 * @author Andy Clement
 * @author Phillip Webb
 * @see FileChangeListener
 * @since 1.3.0
 */
public class FileSystemWatcher {

	private static final long DEFAULT_IDLE_TIME = 1000;

	private static final long DEFAULT_QUIET_TIME = 200;

	private List<FileChangeListener> listeners = new ArrayList<FileChangeListener>();

	private final boolean daemon;

	private final long idleTime;

	private final long quietTime;

	private Thread watchThread;

	private volatile boolean running = true;

	private Map<File, FolderSnapshot> folders = new LinkedHashMap<File, FolderSnapshot>();

	public FileSystemWatcher() {
		this(true, DEFAULT_IDLE_TIME, DEFAULT_QUIET_TIME);
	}

	public FileSystemWatcher(boolean daemon, long idleTime, long quietTime) {
		this.daemon = daemon;
		this.idleTime = idleTime;
		this.quietTime = quietTime;
	}

	public synchronized void addListener(FileChangeListener fileChangeListener) {
		Assert.notNull(fileChangeListener, "FileChangeListener must not be null");
		checkNotStarted();
		this.listeners.add(fileChangeListener);
	}

	public synchronized void addSourceFolder(File folder) {
		Assert.notNull(folder, "Folder must not be null");
		Assert.isTrue(folder.isDirectory(), "Folder must not be a file");
		checkNotStarted();
		this.folders.put(folder, null);
	}

	private void checkNotStarted() {
		Assert.state(this.watchThread == null, "FileSystemWatcher already started");
	}

	public synchronized void start() {
		saveInitalSnapshots();
		if (this.watchThread == null) {
			this.watchThread = new Thread() {
				@Override
				public void run() {
					while (FileSystemWatcher.this.running) {
						try {
							scan();
						}
						catch (InterruptedException ex) {
						}
					}
				};
			};
			this.watchThread.setName("File Watcher");
			this.watchThread.setDaemon(this.daemon);
			this.running = true;
			this.watchThread.start();
		}
	}

	private void saveInitalSnapshots() {
		for (File folder : this.folders.keySet()) {
			this.folders.put(folder, new FolderSnapshot(folder));
		}
	}

	private void scan() throws InterruptedException {
		Thread.sleep(this.idleTime - this.quietTime);
		Set<FolderSnapshot> previous;
		Set<FolderSnapshot> current = new HashSet<FolderSnapshot>(this.folders.values());
		do {
			previous = current;
			current = getCurrentSnapshots();
			Thread.sleep(this.quietTime);
		}
		while (!previous.equals(current));
		updateSnapshots(current);
	}

	private Set<FolderSnapshot> getCurrentSnapshots() {
		Set<FolderSnapshot> snapshots = new LinkedHashSet<FolderSnapshot>();
		for (File folder : this.folders.keySet()) {
			snapshots.add(new FolderSnapshot(folder));
		}
		return snapshots;
	}

	private void updateSnapshots(Set<FolderSnapshot> snapshots) {
		Map<File, FolderSnapshot> updated = new LinkedHashMap<File, FolderSnapshot>();
		Set<ChangedFiles> changeSet = new LinkedHashSet<ChangedFiles>();
		for (FolderSnapshot snapshot : snapshots) {
			FolderSnapshot previous = this.folders.get(snapshot.getFolder());
			updated.put(snapshot.getFolder(), snapshot);
			ChangedFiles changedFiles = previous.getChangedFiles(snapshot);
			if (!changedFiles.getFiles().isEmpty()) {
				changeSet.add(changedFiles);
			}
		}
		fireListeners(changeSet);
		this.folders = updated;
	}

	private void fireListeners(Set<ChangedFiles> changeSet) {
		for (FileChangeListener listener : this.listeners) {
			listener.onChange(changeSet);
		}
	}

	public synchronized void stop() {
		Thread thread = this.watchThread;
		if (thread != null) {
			this.running = false;
			try {
				thread.join();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			this.watchThread = null;
		}
	}
}
