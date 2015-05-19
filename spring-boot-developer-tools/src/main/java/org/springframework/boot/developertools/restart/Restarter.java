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
import java.io.Closeable;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.developertools.restart.classloader.RestartClassLoader;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.cglib.core.ClassNameReader;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Allows a running application to be restarted with an updated classpath. The restarter
 * works by creating a new application ClassLoader that is split into two parts. The top
 * part contains static URLs that don't change (for example 3rd party libraries and Spring
 * Boot itself) and the bottom part contains URLs where classes and resources might be
 * updated.
 * <p>
 * The Restarter should be {@link #initialize(String[]) initialized} early to ensure that
 * classes are loaded multiple times. Mostly the {@link RestartApplicationListener} can be
 * relied upon to perform initialization, however, you may need to call
 * {@link #initialize(String[])} directly if your SpringApplication arguments are not
 * identical to your main method arguments.
 * <p>
 * By default, application running in an IDE (i.e. those not packaged as "fat jars") will
 * automatically detect URLs that can change. It's also possible to manually configure
 * URLs or class file updates for remote restart scenarios.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see RestartApplicationListener
 * @see #initialize(String[])
 * @see #getInstance()
 * @see #restart()
 */
public class Restarter {

	// FIXME DC links when URLs and classfiles can be configured

	private static Restarter instance;

	private Log logger = new DeferredLog();

	private final boolean forceReferenceCleanup;

	private final ClassLoader applicationClassLoader;

	private final String mainClassName;

	private final String[] args;

	private final UncaughtExceptionHandler exceptionHandler;

	private List<URL> urls = new ArrayList<URL>();

	private ActionThead actionThead;

	/**
	 * Internal constructor to create a new {@link Restarter} instance.
	 * @param args the application arguments
	 * @param forceReferenceCleanup if soft/weak reference cleanup should be forced
	 * @see #initialize(String[])
	 */
	protected Restarter(String[] args, boolean forceReferenceCleanup) {
		this(Thread.currentThread(), args, forceReferenceCleanup);
	}

	/**
	 * Internal constructor to create a new {@link Restarter} instance.
	 * @param thread the source thread
	 * @param args the application arguments
	 * @param forceReferenceCleanup if soft/weak reference cleanup should be forced
	 * @see #initialize(String[])
	 */
	protected Restarter(Thread thread, String[] args, boolean forceReferenceCleanup) {
		Assert.notNull(thread, "Thread must not be null");
		Assert.notNull(args, "Args must not be null");
		SilentExitExceptionHandler.setup(thread);
		this.forceReferenceCleanup = forceReferenceCleanup;
		this.applicationClassLoader = thread.getContextClassLoader();
		this.mainClassName = new MainMethod(thread).getDeclaringClassName();
		this.args = args;
		this.exceptionHandler = thread.getUncaughtExceptionHandler();
		this.actionThead = new ActionThead();
	}

	private void initialize() {
		preInitializeLeakyClasses();
		try {
			ChangeableUrls changeableUrls = ChangeableUrls
					.fromUrlClassLoader((URLClassLoader) this.applicationClassLoader);
			this.urls.addAll(changeableUrls.toList());
			runAction("Initialize", false, new Action() {
				@Override
				public void run() throws Exception {
					start();
				}
			});
		}
		catch (Exception ex) {
			this.logger.warn("Unable to initialize restarter", ex);
		}
		SilentExitExceptionHandler.exitCurrentThread();
	}

	private void preInitializeLeakyClasses() {
		try {
			Class<?> readerClass = ClassNameReader.class;
			Field field = readerClass.getDeclaredField("EARLY_EXIT");
			field.setAccessible(true);
			((Throwable) field.get(null)).fillInStackTrace();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Restart the running application.
	 */
	public void restart() {
		runAction("Restart", false, new Action() {
			@Override
			public void run() throws Exception {
				Restarter.this.stop();
				Restarter.this.start();
			}
		});
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader instanceof Closeable) {
			try {
				((Closeable) classLoader).close();
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	private void start() throws Exception {
		RestartClassLoader classLoader = new RestartClassLoader(
				this.applicationClassLoader, getUrls());
		RestartLauncher launcher = new RestartLauncher(classLoader, this.mainClassName,
				this.args, this.exceptionHandler);
		launcher.start();
		launcher.join();
	}

	private void stop() throws Exception {
		triggerShutdownHooks();
		cleanupCaches();
		if (this.forceReferenceCleanup) {
			forceReferenceCleanup();
		}
		System.gc();
		System.runFinalization();
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
		cleanupKnownCaches();
	}

	private void cleanupKnownCaches() throws Exception {
		// Whilst not strictly necessary it helps to cleanup soft reference caches
		// early rather than waiting for memory limits to be reached
		clear(ResolvableType.class, "cache");
		clear("org.springframework.core.SerializableTypeWrapper", "cache");
		clear(CachedIntrospectionResults.class, "acceptedClassLoaders");
		clear(CachedIntrospectionResults.class, "strongClassCache");
		clear(CachedIntrospectionResults.class, "softClassCache");
		clear(ReflectionUtils.class, "declaredFieldsCache");
		clear(ReflectionUtils.class, "declaredMethodsCache");
		clear(AnnotationUtils.class, "findAnnotationCache");
		clear(AnnotationUtils.class, "annotatedInterfaceCache");
		clear("com.sun.naming.internal.ResourceManager", "propertiesCache");
	}

	private void clear(String className, String fieldName) {
		try {
			clear(Class.forName(className), fieldName);
		}
		catch (Exception ex) {
			this.logger.debug("Unable to clear field " + className + " " + fieldName, ex);
		}
	}

	private void clear(Class<?> type, String fieldName) throws Exception {
		Field field = type.getDeclaredField(fieldName);
		field.setAccessible(true);
		Object instance = field.get(null);
		if (instance instanceof Set) {
			((Set<?>) instance).clear();
		}
		if (instance instanceof Map) {
			Map<?, ?> map = ((Map<?, ?>) instance);
			for (Iterator<?> iterator = map.keySet().iterator(); iterator.hasNext();) {
				Object value = iterator.next();
				if (value instanceof Class
						&& ((Class<?>) value).getClassLoader() instanceof RestartClassLoader) {
					iterator.remove();
				}

			}
		}
	}

	/**
	 * Cleanup any soft/weak references by forcing an {@link OutOfMemoryError} error.
	 */
	private void forceReferenceCleanup() {
		try {
			final List<long[]> memory = new LinkedList<long[]>();
			while (true) {
				memory.add(new long[102400]);
			}
		}
		catch (final OutOfMemoryError ex) {
		}
	}

	private URL[] getUrls() {
		return new ArrayList<URL>(this.urls).toArray(new URL[this.urls.size()]);
	}

	/**
	 * Called to finish {@link Restarter} initialization when application logging is
	 * available.
	 */
	void finish() {
		this.logger = DeferredLog.replay(this.logger, LogFactory.getLog(getClass()));
	}

	/**
	 * Run a specific {@link Action} using an {@link ActionThead}. The action thread is
	 * created early to ensure that {@code Thread.inheritedAccessControlContext} doesn't
	 * accidentally keep a reference to the disposable {@link RestartClassLoader}.
	 * @param name the name of the thread
	 * @param join if the caller should block until the action completes
	 * @param action the action to run
	 */
	private synchronized void runAction(String name, boolean join, Action action) {
		this.actionThead.run(name, join, action);
	}

	/**
	 * Initialize restart support for the current application. Called automatically by
	 * {@link RestartApplicationListener} but can also be called directly if main
	 * application arguments are not the same as those passed to the
	 * {@link SpringApplication}.
	 * @param args main application arguments
	 */
	public static void initialize(String[] args) {
		initialize(args, false);
	}

	/**
	 * Initialize restart support for the current application. Called automatically by
	 * {@link RestartApplicationListener} but can also be called directly if main
	 * application arguments are not the same as those passed to the
	 * {@link SpringApplication}.
	 * @param args main application arguments
	 * @param forceReferenceCleanup if forcing of soft/weak reference should happen on
	 * each restart. This will slow down restarts and is indended primarily for testing
	 */
	public static void initialize(String[] args, boolean forceReferenceCleanup) {
		if (instance == null) {
			synchronized (Restarter.class) {
				instance = new Restarter(args, forceReferenceCleanup);
			}
			instance.initialize();
		}
	}

	/**
	 * Return the active {@link Restarter} instance. Cannot be called before
	 * {@link #initialize(String[]) initialization}.
	 * @return the restarter
	 */
	public synchronized static Restarter getInstance() {
		Assert.state(instance != null, "Restarter has not been initialized");
		return instance;
	}

	/**
	 * An action that can be run by the {@link ActionThead}.
	 */
	private static interface Action {

		void run() throws Exception;

	}

	/**
	 * A {@link Thread} used to run actions without leaking classloader memory.
	 * @see Restarter#runAction(Action)
	 */
	private class ActionThead extends Thread {

		private Action action;

		public ActionThead() {
			setDaemon(false);
		}

		@Override
		public void run() {
			try {
				this.action.run();
				// We are safe to refresh the ActionThread (and indirectly call
				// AccessController.getContext()) since our stack doesn't include the
				// RestartClassLoader
				Restarter.this.actionThead = new ActionThead();
			}
			catch (Exception ex) {
				ex.printStackTrace();
				System.exit(1);
			}
		}

		public void run(String name, boolean join, Action action) {
			setName(name);
			this.action = action;
			start();
			try {
				if (join) {
					join();
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}

	}

}
