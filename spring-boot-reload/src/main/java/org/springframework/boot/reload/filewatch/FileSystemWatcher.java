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

package org.springframework.boot.reload.filewatch;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.reload.log.Log;

/**
 * Watches specific folders for file changes.
 *
 * @author Andy Clement
 * @author Phillip Webb
 */
public class FileSystemWatcher {

	// Timings should account for the fact that some file systems only support second
	// granularity

	private static final long IDLE_TIME = 1000;

	private static final long QUIET_TIME = 200;

	private static final Set<String> DOT_FOLDERS = Collections
			.unmodifiableSet(new HashSet<String>(Arrays.asList(".", "..")));

	private final List<FileChangeListener> listeners = new ArrayList<FileChangeListener>();

	private final Set<File> sourceFolders = new LinkedHashSet<File>();

	private long modifiedAfterTime;

	/**
	 * Create a new {@link FileSystemWatcher} instance.
	 * @param listeners {@link FileChangeListener}s to be added
	 */
	public FileSystemWatcher(FileChangeListener... listeners) {
		this(true, listeners);
	}

	/**
	 * Create a new {@link FileSystemWatcher} instance.
	 * @param daemon if a daemon thread should be used to watch for changes
	 * @param listeners {@link FileChangeListener}s to be added
	 */
	public FileSystemWatcher(boolean daemon, FileChangeListener... listeners) {
		this(System.currentTimeMillis() + 800, daemon, listeners);
	}

	/**
	 * Create a new {@link FileSystemWatcher} for testing.
	 * @param daemon if a daemon thread should be used to watch for changes
	 * @param modifiedAfterTime the time that files must have been modified after
	 * @param listeners {@link FileChangeListener}s to be added
	 */
	protected FileSystemWatcher(long modifiedAfterTime, boolean daemon,
			FileChangeListener... listeners) {
		this.modifiedAfterTime = modifiedAfterTime;
		for (FileChangeListener listener : listeners) {
			addListener(listener);
		}
		Thread watchThread = new Thread() {
			@Override
			public void run() {
				while (true) {
					try {
						scan();
					}
					catch (InterruptedException ex) {
					}
				}
			}
		};
		watchThread.setDaemon(daemon);
		watchThread.setName("File Watcher");
		watchThread.start();
	}

	private void scan() throws InterruptedException {
		Thread.sleep(IDLE_TIME - QUIET_TIME);
		Scanner scanner = new Scanner();
		synchronized (this) {
			final long after = this.modifiedAfterTime;
			boolean found;
			do {
				Thread.sleep(QUIET_TIME);
				final long before = System.currentTimeMillis();
				found = false;
				for (File sourceFolder : this.sourceFolders) {
					found |= scanner.scan(sourceFolder, new FileFilter() {
						@Override
						public boolean accept(File file) {
							return (file.isFile() && ((file.lastModified() > after) && (file
									.lastModified() <= before)));
						}
					});
				}
				if (!found && scanner.hasChanges()) {
					this.modifiedAfterTime = before;
				}
			}
			while (found);
			scanner.fireListeners(this.listeners);
		}
	};

	/**
	 * Add a source folder to be monitored.
	 * @param sourceFolder the folder to monitor
	 */
	public synchronized void addSourceFolder(File sourceFolder) {
		if (sourceFolder == null || !sourceFolder.isDirectory()) {
			throw new IllegalArgumentException("source must be a folder");
		}
		Log.debug("Watching " + sourceFolder + " for changes");
		this.sourceFolders.add(sourceFolder);
	}

	/**
	 * Add a {@link FileChangeListener} that will be informed when files change
	 * @param listener the {@link FileChangeListener}
	 */
	public void addListener(FileChangeListener listener) {
		this.listeners.add(listener);
	}

	/**
	 * Tracks files that have changed.
	 */
	private static class Scanner {

		private final Map<File, Set<File>> changed = new LinkedHashMap<File, Set<File>>();

		public boolean scan(File sourceFolder, FileFilter filter) {
			Set<File> files = this.changed.get(sourceFolder);
			if (files == null) {
				files = new LinkedHashSet<File>();
				this.changed.put(sourceFolder, files);
			}
			return collectFilesRecursively(sourceFolder, filter, files);
		}

		private boolean collectFilesRecursively(File folder, FileFilter filter,
				Set<File> files) {
			boolean result = false;
			File[] children = folder.listFiles();
			if (children != null) {
				for (File child : children) {
					if (filter.accept(child)) {
						result |= files.add(child);
					}
					if (child.isDirectory() && !DOT_FOLDERS.contains(child.getName())) {
						result |= collectFilesRecursively(child, filter, files);
					}
				}
			}
			return result;
		}

		public void fireListeners(List<FileChangeListener> listeners) {
			if (hasChanges()) {
				Set<ChangedFiles> changeSet = new LinkedHashSet<ChangedFiles>();
				for (final Map.Entry<File, Set<File>> entry : this.changed.entrySet()) {
					if (!entry.getValue().isEmpty()) {
						Log.debug("Detected changes in " + entry.getKey() + " : "
								+ entry.getValue());
						changeSet.add(new ChangedFiles(entry.getKey(), entry.getValue()));
					}
				}
				for (FileChangeListener listener : listeners) {
					listener.onChange(changeSet);
				}
			}
		}

		private boolean hasChanges() {
			for (Set<File> files : this.changed.values()) {
				if (!files.isEmpty()) {
					return true;
				}
			}
			return false;
		}

	}

}
