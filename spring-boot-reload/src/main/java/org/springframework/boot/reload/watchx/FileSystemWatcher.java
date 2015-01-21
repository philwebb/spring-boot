/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.boot.reload.watchx;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO needs smacking with a refactoring hammer, definitely not optimal
public class FileSystemWatcher {

	// the thread being managed
	private Thread thread;

	// whether the thread is running
	private boolean threadRunning = false;

	// The Watcher running inside the thread
	private Watcher watchThread;

	public FileSystemWatcher(FileChangeListener listener) {
		this.watchThread = new Watcher(this, listener);
	}

	private void ensureWatchThreadRunning() {
		if (!this.threadRunning) {
			this.thread = new Thread(this.watchThread);
			// Not currently a daemon thread as something needs to keep running whilst we
			// restart the system
			// this.thread.setDaemon(true);
			this.thread.start();
			this.watchThread.setThread(this.thread);
			this.watchThread.updateName();
			this.threadRunning = true;
		}
	}

	public void shutdown() {
		if (this.threadRunning) {
			this.watchThread.stop();
		}
	}

	public void register(URL[] urls) {
		for (URL url : urls) {
			try {
				System.out.println("Registering URL for watching: " + url);
				register(new File(url.toURI()));
			}
			catch (Exception e) {
				System.out.println("Can't watch: " + url + "\n" + e.toString());
			}
		}
	}

	public void register(String pathOrFile) {
		register(new File(pathOrFile));
	}

	public void register(File toMonitor) {
		if (toMonitor.isDirectory()) {
			watchResource(toMonitor); // Watch for directory contents changing
			File[] files = toMonitor.listFiles();
			for (File f : files) {
				if (f.isDirectory()) {
					register(f);
				}
				else {
					watchResource(f);
				}
			}
		}
		else {
			watchResource(toMonitor);
		}
	}

	private void watchResource(File toMonitor) {
		// For now don't watch java resources
		// if (toMonitor.toString().endsWith(".java")) {
		// return;
		// }
		if (this.watchThread.addResource(toMonitor)) {
			ensureWatchThreadRunning();
			this.watchThread.updateName();
		}
	}

	public void setPaused(boolean shouldBePaused) {
		this.watchThread.paused = shouldBePaused;
	}
}

class Watcher implements Runnable {

	long lastScanTime;

	private static boolean DEBUG = true;

	// TODO configurable scan interval?
	private static long interval = 1100;// ms
	Map<File, Set<String>> watchListDirContents = new HashMap<File, Set<String>>();
	List<File> watchListFiles = new ArrayList<File>();
	List<Long> watchListLMTs = new ArrayList<Long>();
	FileChangeListener listener;
	FileSystemWatcher fsw;
	private boolean timeToStop = false;
	public boolean paused = false;
	private Thread thread = null;

	public Watcher(FileSystemWatcher fsw, FileChangeListener listener) {
		this.listener = new BatchingFileListener(listener);
		this.fsw = fsw;
	}

	public void setThread(Thread thread) {
		this.thread = thread;
	}

	/**
	 * Add a new File that the thread should start watching.
	 *
	 * @param fileToWatch the new file to watch
	 * @return true if the file is now being watched, false otherwise
	 */
	public boolean addResource(File fileToWatch) {
		if (!fileToWatch.exists()) {
			return false;
		}
		synchronized (this) {
			if (DEBUG) {
				System.out.println("Now watching " + fileToWatch);
			}
			int insertionPos = findPosition(fileToWatch);
			if (insertionPos == -1) {
				this.watchListFiles.add(fileToWatch);
				this.watchListLMTs.add(fileToWatch.lastModified());
			}
			else {
				this.watchListFiles.add(insertionPos, fileToWatch);
				this.watchListLMTs.add(insertionPos, fileToWatch.lastModified());
			}
			if (fileToWatch.isDirectory()) {
				// Contents may change, keep an eye on them!
				Set<String> dirContents = new LinkedHashSet<String>();
				File[] dirContentsArray = fileToWatch.listFiles();
				if (dirContentsArray != null) {
					for (File f : dirContentsArray) {
						dirContents.add(f.getName());
					}
				}
				System.out.println("Watching directory " + fileToWatch
						+ " with contents: " + dirContents);
				this.watchListDirContents.put(fileToWatch, dirContents);
			}
			return true;
		}
	}

	public void updateName() {
		if (this.thread != null) {
			this.thread
					.setName("FileSystemWatcher: files=#" + this.watchListFiles.size());
		}
	}

	private int findPosition(File file) {
		String filename = file.getName();
		int len = this.watchListFiles.size();
		if (len == 0) {
			return 0;
		}
		for (int f = 0; f < len; f++) {
			File file2 = this.watchListFiles.get(f);
			int cmp = file2.getName().compareTo(filename);
			// as we are using 'names' we are only considering the last part, so
			// foo/bar/Goo.class and foo/Goo.class look the same
			// and will return cmp==0. Not really sure it matters about using fq names
			if (cmp > 0) {
				return f;
			}
			// else if (GlobalConfiguration.assertsMode && cmp == 0) {
			// // Are we watching the same file twice, that is bad!
			// if
			// (file2.getAbsoluteFile().toString().equals(file.getAbsoluteFile().toString()))
			// {
			// log.severe("Watching the same file twice: "+file.getAbsoluteFile().toString());
			// }
			// }
		}
		return -1;
	}

	@Override
	public void run() {
		while (!this.timeToStop) {
			try {
				Thread.sleep(interval);
			}
			catch (Exception e) {
			}
			if (!this.paused) {
				List<File> changedFiles = new ArrayList<File>();
				synchronized (this) {
					int len = this.watchListFiles.size();
					for (int f = 0; f < len; f++) {
						File file = this.watchListFiles.get(f);
						long lastModTime = file.lastModified();
						if (lastModTime > this.watchListLMTs.get(f)) {
							if (DEBUG) {
								System.out
										.println("Observed last modification time change for "
												+ file
												+ " (lastScanTime="
												+ this.lastScanTime + ")");
							}
							this.watchListLMTs.set(f, lastModTime);
							changedFiles.add(file);
						}
					}
					this.lastScanTime = System.currentTimeMillis();
				}
				for (File changedFile : changedFiles) {
					determineChangesSince(changedFile, this.lastScanTime);
				}
			}
		}
	}

	private void determineChangesSince(File file, long lastScanTime) {
		try {
			this.listener.filesChanged(file);
			if (file.isDirectory()) {
				File[] filesOfInterest = file.listFiles(new RecentChangeFilter(file
						.lastModified() - 1));
				Set<String> existingContents = this.watchListDirContents.get(file);
				for (File f : filesOfInterest) {
					String name = f.getName();
					if (!existingContents.contains(name)) {
						// new file!
						existingContents.add(name);
						this.fsw.register(f);
					}
					this.listener.filesChanged(f);
				}
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * This intermediate listener will batch up changes that happen close together.
	 *
	 */
	static class BatchingFileListener implements FileChangeListener {

		private FileChangeListener delegate;
		private List<File> batchedChanges = new ArrayList<File>();
		WaitingThread wt = new WaitingThread();

		class WaitingThread extends Thread {
			// At 'endtime' the changed file list will be sent to the delegate
			long endtime;

			@Override
			public void run() {
				// System.out.println("batch thread running... #"
				// + BatchingFileListener.this.batchedChanges.size());
				// Wait for 300ms of 'quiet time' before sending over the list of changes
				this.endtime = System.currentTimeMillis() + 300;
				do {
					long currentTime = System.currentTimeMillis();
					if (currentTime > this.endtime) {
						synchronized (BatchingFileListener.this.batchedChanges) {
							BatchingFileListener.this.delegate
									.filesChanged(BatchingFileListener.this.batchedChanges
											.toArray(new File[BatchingFileListener.this.batchedChanges
													.size()]));
							BatchingFileListener.this.batchedChanges.clear();
						}
						break;
					}
					try {
						Thread.sleep(50);
					}
					catch (Exception e) {
					}
				}
				while (true);
				// System.out.println("batch thread stopping...");
			}

			// Tell this thread to wait a little longer for a 'quiet period'
			public void delay() {
				this.endtime = System.currentTimeMillis() + 300;
			}

		}

		BatchingFileListener(FileChangeListener delegate) {
			this.delegate = delegate;
		}

		@Override
		public void filesChanged(File... files) {
			synchronized (this.batchedChanges) {
				for (File f : files) {
					this.batchedChanges.add(f);
				}
			}
			if (!this.wt.isAlive()) {
				this.wt = new WaitingThread();
				this.wt.start();
			}
			else {
				this.wt.delay();
			}
		}

	}

	static class RecentChangeFilter implements FileFilter {

		private long lastScanTime;

		public RecentChangeFilter(long lastScanTime) {
			this.lastScanTime = lastScanTime;
		}

		@Override
		public boolean accept(File pathname) {
			return (pathname.lastModified() > this.lastScanTime);
		}

	}

	public void stop() {
		this.timeToStop = true;
	}

}
