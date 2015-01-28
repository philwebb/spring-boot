/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.reload.reloader;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

import org.springframework.boot.Reloader;
import org.springframework.boot.reload.filewatch.ChangedFiles;
import org.springframework.boot.reload.filewatch.FileChangeListener;
import org.springframework.boot.reload.filewatch.FileSystemWatcher;
import org.springframework.boot.reload.livereload.LiveReloadServer;
import org.springframework.boot.reload.log.Log;

/**
 * Default {@link Reloader} for use with Spring Boot applications.
 *
 * @author Phillip Webb
 */
public class SpringBootReloader extends Reloader {

	private final Thread thread;

	private final ClassLoader classLoader;

	private final ReloadProperties properties;

	private LiveReloadServer liveReloadServer;

	private Launcher launcher;

	public SpringBootReloader() {
		this.thread = Thread.currentThread();
		this.classLoader = this.thread.getContextClassLoader();
		this.properties = new ReloadProperties();
	}

	@Override
	protected void start(String[] args) {
		if (this.classLoader instanceof ReloadClassLoader) {
			Log.debug("Reload support is already active");
			return;
		}
		Log.setEnabled(this.properties.isDebug());
		try {
			doStart(args);
		}
		catch (SilentExitException ex) {
			throw ex;
		}
		catch (Exception ex) {
			Log.debug("Unable to start SpringBootReloader", ex);
		}
	}

	private void doStart(String[] args) throws Exception {
		Log.debug("Starting SpringBootReloader");
		assertIsMainThread();
		Method mainMethod = findMainMethod();
		ReloadableUrls reloadableUrls = ReloadableUrls
				.fromUrlClassLoader((URLClassLoader) this.classLoader);
		startLiveReloadServer();
		startFileWatcher(reloadableUrls);
		this.launcher = new Launcher(this.classLoader, reloadableUrls, mainMethod, args,
				this.thread.getUncaughtExceptionHandler());
		if (this.properties.isShowBanner()) {
			ReloadBanner.print();
		}
		this.launcher.start();
		exitMainThread();
	}

	private void assertIsMainThread() {
		if (!"main".equals(this.thread.getName())) {
			throw new IllegalStateException("Thread must be named 'main'");
		}
		String classLoaderName = this.thread.getContextClassLoader().getClass().getName();
		if (!classLoaderName.contains("AppClassLoader")
				&& !classLoaderName.contains("LaunchedURLClassLoader")) {
			throw new IllegalStateException(
					"Thread must use an AppClassLoader/LaunchedURLClassLoader, not a: "
							+ classLoaderName);
		}
	}

	private Method findMainMethod() {
		for (StackTraceElement element : this.thread.getStackTrace()) {
			if ("main".equals(element.getMethodName())) {
				Method method = getMainMethod(element);
				if (method != null) {
					return method;
				}
			}
		}
		throw new IllegalStateException("Reload must be applied to the main method");
	}

	private Method getMainMethod(StackTraceElement element) {
		try {
			Class<?> elementClass = Class.forName(element.getClassName());
			Method method = elementClass.getDeclaredMethod("main", String[].class);
			if (Modifier.isStatic(method.getModifiers())) {
				return method;
			}
		}
		catch (Exception ex) {
			// Ignore
		}
		return null;
	}

	private void startLiveReloadServer() {
		if (this.properties.isLiveReload()) {
			try {
				this.liveReloadServer = new LiveReloadServer();
				this.liveReloadServer.start();
			}
			catch (Exception ex) {
				Log.debug("Unable to start LiveReload server", ex);
			}
		}
	}

	private void startFileWatcher(ReloadableUrls reloadableUrls)
			throws URISyntaxException {
		FileSystemWatcher fileSystemWatcher = new FileSystemWatcher(
				!this.properties.isKeepAlive());
		for (URL url : reloadableUrls) {
			fileSystemWatcher.addSourceFolder(new File(url.toURI()));
		}
		fileSystemWatcher.addListener(new FileChangeListener() {
			@Override
			public void onChange(Set<ChangedFiles> changeSet) {
				SpringBootReloader.this.onChange(changeSet);
			}
		});
	}

	private void onChange(Set<ChangedFiles> changeSet) {
		try {
			if (isRestartRequired(changeSet)) {
				this.launcher.restart();
			}
			triggerLiveReload();
		}
		catch (Exception ex) {
			Log.debug("Error restarting ", ex);
		}
	}

	private boolean isRestartRequired(Set<ChangedFiles> changeSet) {
		for (ChangedFiles changedFiles : changeSet) {
			for (String file : changedFiles.getRelativeFileNames()) {
				if (!isNonRestarting(file)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isNonRestarting(String file) {
		for (String folder : this.properties.getNonRestartingFolders()) {
			folder = (folder.endsWith("/") ? folder : folder + "/");
			if (file.startsWith(folder)) {
				return true;
			}
		}
		return false;
	}

	private void triggerLiveReload() {
		try {
			if (this.liveReloadServer != null) {
				this.liveReloadServer.triggerReload();
			}
		}
		catch (Exception ex) {
			Log.debug("Error triggering live reload ", ex);
		}
	}

	private void exitMainThread() {
		Log.debug("Exiting original main thread");
		this.thread.setUncaughtExceptionHandler(new SilentUncaughtExceptionHandler(
				this.thread.getUncaughtExceptionHandler()));
		throw new SilentExitException();
	}

}
