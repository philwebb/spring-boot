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

package org.springframework.boot.developertools.restart;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.util.Assert;

/**
 * @author Phillip Webb
 */
public class Restarter {

	// Classloader Files
	// The URLs
	// The scope
	// the restart method

	private static Restarter instance;

	private ClassLoader classLoader;

	private List<URL> urls = new ArrayList<URL>();

	private RestartLauncher launcher;

	protected Restarter(String[] args) {
		this(Thread.currentThread(), args);
	}

	protected Restarter(Thread thread, String[] args) {
		Assert.notNull(thread, "Thread must not be null");
		Assert.notNull(args, "Args must not be null");
		MainMethod mainMethod = new MainMethod(thread);
		SilentUncaughtExceptionHandler.applyTo(thread);
		this.classLoader = thread.getContextClassLoader();
		this.launcher = new RestartLauncher(mainMethod.getDeclaringClassName(), args,
				thread.getUncaughtExceptionHandler());
		// FIXME ifNotAFatJar
		// configure reloadable URLS
		// start the launcher
		// exit the main thread
		// else just wait for a restart
	}

	private void initialize() {
		try {
			ChangeableUrls changeableUrls = ChangeableUrls
					.fromUrlClassLoader((URLClassLoader) this.classLoader);
			this.urls.addAll(changeableUrls.toList());
			start();
			throw new SilentExitException();
		}
		catch (Exception ex) {
			if (ex instanceof SilentExitException) {
				throw (SilentExitException) ex;
			}
			// FIXME log
			ex.printStackTrace();
		}
	}

	public void restart() {
		// Use a non-deamon thread to ensure the the JVM doesn't exit
		Thread restartThread = new Thread() {
			@Override
			public void run() {
				try {
					Restarter.this.stop();
					Restarter.this.start();
					System.gc();
				}
				catch (Exception ex) {
					ex.printStackTrace();
					throw new IllegalStateException(ex);
				}
			}
		};
		restartThread.setDaemon(false);
		restartThread.start();
		try {
			restartThread.join();
		}
		catch (InterruptedException ex) {
		}
	}

	private void start() throws Exception {
		this.launcher.start(this.classLoader, getUrls());
	}

	private void stop() throws Exception {
		Introspector.flushCaches();
		triggerShutdownHooks();
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

	private URL[] getUrls() {
		return new ArrayList<URL>(this.urls).toArray(new URL[this.urls.size()]);
	}

	public void logStartupInformation() {
	}

	/**
	 * Initialize restart support for the current application. Called automatically by
	 * {@link RestartApplicationListener} but can also be called directly if main
	 * application arguments are not the same as those passed to the
	 * {@link SpringApplication}.
	 * @param args main application arguments
	 */
	public static void initialize(String[] args) {
		if (instance == null) {
			synchronized (Restarter.class) {
				instance = new Restarter(args);
			}
			instance.initialize();
		}
	}

	/**
	 * Return the {@link Restarter} instance.
	 * @return the restarter
	 */
	public synchronized static Restarter getInstance() {
		Assert.state(instance != null, "Restarter has not been initialized");
		return instance;
	}

}
