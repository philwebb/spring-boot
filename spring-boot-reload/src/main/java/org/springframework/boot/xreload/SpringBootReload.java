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

package org.springframework.boot.xreload;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.util.Arrays;

import org.springframework.boot.reload.ReloadClassLoader;
import org.springframework.boot.xreload.watchx.FileChangeListener;
import org.springframework.boot.xreload.watchx.FileSystemWatcher;

/**
 * @author Phillip Webb
 */
public class SpringBootReload {

	private Thread thread;

	private Launcher launcher;

	private String[] args;

	private SpringBootReload(Thread thread, String[] args) {
		this.thread = thread;
		this.args = args;
	}

	private void apply() {
		try {
			assertIsMainThread();
			Method mainMethod = findMainMethod();
			ClassLoader classLoader = this.thread.getContextClassLoader();
			ClassLoaderUrls urls = new ClassLoaderUrls((URLClassLoader) classLoader);
			// Create a file system watcher and register the URLs of interest
			FileSystemWatcher fsw = new FileSystemWatcher(new ChangeListener());
			fsw.register(urls.getReloadable());
			this.launcher = new Launcher(classLoader, urls.getReloadable(), mainMethod,
					this.args, this.thread.getUncaughtExceptionHandler());
			this.launcher.launch();
			exitThread();
		}
		catch (Exception ex) {
			if (ex instanceof SilentExitException) {
				throw (SilentExitException) ex;
			}
			new ReloadUnavailableException(ex).printStackTrace();
		}
	}

	private class ChangeListener implements FileChangeListener {
		@Override
		public void filesChanged(File... files) {
			System.out.println("Changes: " + Arrays.toString(files));
			// Perhaps make different decisions depending on what was changed (e.g.
			// javascript resources vs class files)
			SpringBootReload.this.launcher.restart();
		}
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
				try {
					String className = element.getClassName();
					Method method = Class.forName(className).getDeclaredMethod("main",
							String[].class);
					if (Modifier.isStatic(method.getModifiers())) {
						return method;
					}
				}
				catch (Exception ex) {
					// Ignore and carry on
				}
			}
		}
		throw new IllegalStateException("Reload must be applied to the main method");
	}

	private void exitThread() {
		// TODO When running on CF, I seem to have issues with exiting the thread, so this
		// horrible block just keeps it around doing nothing...
		for (int i = 0; i < 100; i++) {
			try {
				Thread.sleep(10000);
			}
			catch (Exception e) {
			}
		}
		this.thread.setUncaughtExceptionHandler(new SilentUncaughtExceptionHandler(
				this.thread.getUncaughtExceptionHandler()));
		throw new SilentExitException();
	}

	public static void apply(String[] args) {
		Thread thread = Thread.currentThread();
		if (thread.getContextClassLoader() instanceof ReloadClassLoader) {
			// Reload support has already been applied
			return;
		}
		new SpringBootReload(thread, args).apply();
	}

}
