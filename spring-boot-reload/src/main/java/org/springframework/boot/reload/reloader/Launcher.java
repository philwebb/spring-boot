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
package org.springframework.boot.reload.reloader;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;

/**
 * Launcher to start the application and possibly restart it later.
 *
 * @author Phillip Webb
 * @author Andy Clement
 */
class Launcher {

	private final ClassLoader applicationClassLoader;

	private final ReloadableUrls reloadableUrls;

	private final String mainClassName;

	private final UncaughtExceptionHandler exceptionHandler;

	private String[] args;

	public Launcher(ClassLoader applicationClassLoader, ReloadableUrls reloadableUrls,
			Method mainMethod, String[] args, UncaughtExceptionHandler exceptionHandler) {
		this.applicationClassLoader = applicationClassLoader;
		this.reloadableUrls = reloadableUrls;
		this.mainClassName = mainMethod.getDeclaringClass().getName();
		this.args = args;
		this.exceptionHandler = exceptionHandler;
	}

	public void restart() throws InterruptedException {
		// Use a non-deamon thread to ensure the the JVM doesn't exit
		Thread restartThread = new Thread() {
			@Override
			public void run() {
				try {
					triggerShutdownHooks();
					Launcher.this.start();
				}
				catch (Exception ex) {
					ex.printStackTrace();
					throw new IllegalStateException(ex);
				}
			}
		};
		restartThread.setUncaughtExceptionHandler(this.exceptionHandler);
		restartThread.setDaemon(false);
		restartThread.start();
		restartThread.join();
	}

	public void start() throws InterruptedException {
		ReloadClassLoader classLoader = new ReloadClassLoader(this.reloadableUrls,
				this.applicationClassLoader);
		LaunchThread launchThread = new LaunchThread(this.mainClassName, this.args);
		launchThread.setDaemon(false);
		launchThread.setContextClassLoader(classLoader);
		launchThread.setUncaughtExceptionHandler(this.exceptionHandler);
		launchThread.setName("main (reloadable)");
		launchThread.start();
		launchThread.join();
	}

	@SuppressWarnings("rawtypes")
	private void triggerShutdownHooks() throws Exception {
		Class<?> hooksClass = Class.forName("java.lang.ApplicationShutdownHooks");
		Method runHooks = hooksClass.getDeclaredMethod("runHooks");
		runHooks.setAccessible(true);
		runHooks.invoke(null);
		Field field = hooksClass.getDeclaredField("hooks");
		field.setAccessible(true);
		field.set(null, new IdentityHashMap());
	}

	private static class LaunchThread extends Thread {

		private final String mainClassName;

		private final String[] args;

		public LaunchThread(String mainClassName, String[] args) {
			this.mainClassName = mainClassName;
			this.args = args;
		}

		@Override
		public void run() {
			try {
				Class<?> mainClass = getContextClassLoader()
						.loadClass(this.mainClassName);
				Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);
				mainMethod.invoke(null, new Object[] { this.args });
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

}
