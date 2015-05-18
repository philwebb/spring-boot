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

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Method;
import java.net.URL;

import org.springframework.boot.developertools.restart.classloader.RestartClassLoader;

/**
 * @author Phillip Webb
 */
class RestartLauncher {

	private LaunchThread launchThread;

	public RestartLauncher(String mainClassName, String[] args,
			UncaughtExceptionHandler exceptionHandler, ClassLoader parentClassLoader,
			URL[] urls) {
		// We need to create the launch thread early to ensure that AccessController
		// doesn't keep a reference to the RestartClassLoader (and therefore prevent GC)
		System.out.println("Made a restartlauncher");
		this.launchThread = new LaunchThread(mainClassName, args, exceptionHandler,
				parentClassLoader, urls);
	}

	public void start(ClassLoader parentClassLoader, URL[] urls)
			throws InterruptedException {
		System.out.println("Starting the RestartLauncher");
		new Exception().printStackTrace();
		LaunchThread launchThread = this.launchThread;
		launchThread.start();
		launchThread.join();
		this.launchThread = launchThread.nextLaunchThread;
	}

	private static class LaunchThread extends Thread {

		private final String mainClassName;

		private final String[] args;

		private LaunchThread nextLaunchThread;

		private URL[] urls;

		private ClassLoader pc;

		public LaunchThread(String mainClassName, String[] args,
				UncaughtExceptionHandler exceptionHandler, ClassLoader parentClassLoader,
				URL[] urls) {
			this.mainClassName = mainClassName;
			this.args = args;
			this.urls = urls;
			this.pc = parentClassLoader;
			setName("main (restartable)");
			setUncaughtExceptionHandler(exceptionHandler);
			setDaemon(false);
			setContextClassLoader(new RestartClassLoader(parentClassLoader, urls));
		}

		@Override
		public void run() {
			try {
				System.gc();
				this.nextLaunchThread = new LaunchThread(this.mainClassName, this.args,
						getUncaughtExceptionHandler(), this.pc, this.urls);
				System.out.println(this.nextLaunchThread.getClass().getClassLoader());
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
