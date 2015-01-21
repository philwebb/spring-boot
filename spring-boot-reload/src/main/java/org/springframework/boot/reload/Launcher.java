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

package org.springframework.boot.reload;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.IdentityHashMap;

import org.springframework.util.ReflectionUtils;

/**
 * @author pwebb
 * @author Andy Clement
 */
public class Launcher {

	private final ClassLoader fixedClassLoader;

	private final URL[] reloadableUrls;

	private final String mainClassName;

	private final UncaughtExceptionHandler exceptionHandler;

	private String[] args;

	public Launcher(ClassLoader fixedClassLoader, URL[] reloadableUrls,
			Method mainMethod, String[] args, UncaughtExceptionHandler exceptionHandler) {
		this.fixedClassLoader = fixedClassLoader;
		this.reloadableUrls = reloadableUrls;
		this.mainClassName = mainMethod.getDeclaringClass().getName();
		this.args = args;
		this.exceptionHandler = exceptionHandler;
	}

	public void launch() {
		ReloadClassLoader classLoader = new ReloadClassLoader(this.reloadableUrls,
				this.fixedClassLoader);
		LaunchThread launchThread = new LaunchThread(this.mainClassName, this.args);
		launchThread.setContextClassLoader(classLoader);
		launchThread.setUncaughtExceptionHandler(this.exceptionHandler);
		launchThread.setName("main (reloadable)");
		launchThread.start();
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

	private static void shutdown() {
		try {
			Class<?> hooksClass = Class.forName("java.lang.ApplicationShutdownHooks");
			Method runHooksMethod = ReflectionUtils.findMethod(hooksClass, "runHooks");
			runHooksMethod.setAccessible(true);
			runHooksMethod.invoke(null);
			Field hooksField = hooksClass.getDeclaredField("hooks");
			hooksField.setAccessible(true);
			hooksField.set(null, new IdentityHashMap());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void restart() {
		shutdown();
		launch();
	}

}
