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
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.developertools.restart.classloader.RestartClassLoader;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * @author Phillip Webb
 */
public class Restarter {

	private static Restarter instance;

	private final ClassLoader applicationClassLoader;

	private final String mainClassName;

	private final String[] args;

	private UncaughtExceptionHandler exceptionHandler;

	private List<URL> urls = new ArrayList<URL>();

	private ActionThead actionThead;

	protected Restarter(String[] args) {
		this(Thread.currentThread(), args);
	}

	protected Restarter(Thread thread, String[] args) {
		Assert.notNull(thread, "Thread must not be null");
		Assert.notNull(args, "Args must not be null");
		SilentUncaughtExceptionHandler.applyTo(thread);
		this.applicationClassLoader = thread.getContextClassLoader();
		this.mainClassName = new MainMethod(thread).getDeclaringClassName();
		this.args = args;
		this.exceptionHandler = thread.getUncaughtExceptionHandler();
		this.actionThead = new ActionThead();
	}

	private void initialize() {
		System.out.println("Initialize Restarter");
		try {
			ChangeableUrls changeableUrls = ChangeableUrls
					.fromUrlClassLoader((URLClassLoader) this.applicationClassLoader);
			this.urls.addAll(changeableUrls.toList());
			runAction(new Action() {
				@Override
				public void run() throws Exception {
					start();
				}
			});
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
		runAction(new Action() {
			@Override
			public void run() throws Exception {
				Restarter.this.stop();
				Restarter.this.start();
			}
		});
	}

	private void start() throws Exception {
		RestartClassLoader classLoader = new RestartClassLoader(
				this.applicationClassLoader, getUrls());
		RestartLauncher launcher = new RestartLauncher(classLoader, this.mainClassName,
				this.args, this.exceptionHandler);
		launcher.start();
		System.gc();
		System.runFinalization();
		launcher.join();
	}

	private void stop() throws Exception {
		triggerShutdownHooks();
		cleanupCaches();
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

	private void cleanupCaches() throws Exception {
		Introspector.flushCaches();
		cleanupSoftReferenceCaches();
	}

	private void cleanupSoftReferenceCaches() throws Exception {
		// Whilst not strictly necessary it helps to cleanup soft reference caches
		// early rather than waiting for memory limits to be reached
		clear(ResolvableType.class, "cache");
		clear(CachedIntrospectionResults.class, "acceptedClassLoaders");
		clear(CachedIntrospectionResults.class, "strongClassCache");
		clear(CachedIntrospectionResults.class, "softClassCache");
		clear(ReflectionUtils.class, "declaredFieldsCache");
		clear(ReflectionUtils.class, "declaredMethodsCache");
		clear(AnnotationUtils.class, "findAnnotationCache");
		clear(AnnotationUtils.class, "annotatedInterfaceCache");
	}

	private void clear(Class<?> type, String fieldName) throws Exception {
		Field field = type.getDeclaredField(fieldName);
		field.setAccessible(true);
		Object instance = field.get(null);
		if (instance instanceof Set) {
			((Set<?>) instance).clear();
		}
		if (instance instanceof Map) {
			((Map<?, ?>) instance).clear();
		}
	}

	private URL[] getUrls() {
		return new ArrayList<URL>(this.urls).toArray(new URL[this.urls.size()]);
	}

	public void logStartupInformation() {
	}

	private synchronized void runAction(Action action) {
		this.actionThead.run(action);
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

	private static interface Action {

		void run() throws Exception;

	}

	private class ActionThead extends Thread {

		private Action action;

		public ActionThead() {
			setDaemon(false);
		}

		@Override
		public void run() {
			try {
				this.action.run();
				Restarter.this.actionThead = new ActionThead();
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		public void run(Action action) {
			this.action = action;
			start();
			try {
				join();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}

	}

}
