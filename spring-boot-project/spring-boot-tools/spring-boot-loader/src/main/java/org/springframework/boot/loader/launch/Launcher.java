/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader.launch;

import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;

import org.springframework.boot.loader.net.protocol.Handlers;

/**
 * Base class for launchers that can start an application with a fully configured
 * classpath.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @since 3.2.0
 */
public abstract class Launcher {

	private static final String JAR_MODE_RUNNER = "org.springframework.boot.loader.launch.JarModeRunner";

	/**
	 * Launch the application. This method is the initial entry point that should be
	 * called by a subclass {@code public static void main(String[] args)} method.
	 * @param args the incoming arguments
	 * @throws Exception if the application fails to launch
	 */
	protected void launch(String[] args) throws Exception {
		if (!isExploded()) {
			Handlers.register();
		}
		try {
			List<URL> classPathUrls = getClassPathUrls();
			ClassLoader classLoader = createClassLoader(classPathUrls);
			String jarMode = System.getProperty("jarmode");
			String mainClassName = (jarMode != null && !jarMode.isEmpty()) ? JAR_MODE_RUNNER : getMainClass();
			launch(classLoader, mainClassName, args);
		}
		catch (UncheckedIOException ex) {
			throw ex.getCause();
		}
	}

	/**
	 * Returns the archives that will be used to construct the class path.
	 * @return the class path archives
	 * @throws Exception if the class path archives cannot be obtained
	 */
	protected abstract List<URL> getClassPathUrls() throws Exception;

	/**
	 * Create a classloader for the specified archives.
	 * @param classPathUrls the classpath URLs
	 * @return the classloader
	 * @throws Exception if the classloader cannot be created
	 */
	protected ClassLoader createClassLoader(List<URL> classPathUrls) throws Exception {
		return createClassLoader(classPathUrls.toArray(new URL[0]));
	}

	private ClassLoader createClassLoader(URL[] urls) {
		ClassLoader parent = getClass().getClassLoader();
		return new LaunchedURLClassLoader(isExploded(), getArchive(), urls, parent);
	}

	/**
	 * Launch the application given the archive file and a fully configured classloader.
	 * @param classLoader the classloader
	 * @param mainClassName the main class to run
	 * @param args the incoming arguments
	 * @throws Exception if the launch fails
	 */
	protected void launch(ClassLoader classLoader, String mainClassName, String[] args) throws Exception {
		Thread.currentThread().setContextClassLoader(classLoader);
		Class<?> mainClass = Class.forName(mainClassName, false, Thread.currentThread().getContextClassLoader());
		Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);
		mainMethod.setAccessible(true);
		mainMethod.invoke(null, new Object[] { args });
	}

	/**
	 * Returns the main class that should be launched.
	 * @return the name of the main class
	 * @throws Exception if the main class cannot be obtained
	 */
	protected abstract String getMainClass() throws Exception;

	/**
	 * Return the archive being launched or {@code null} if there is no archive.
	 * @return the launched archive
	 */
	protected abstract Archive getArchive();

	/**
	 * Returns if the launcher is running in an exploded mode. If this method returns
	 * {@code true} then only regular JARs are supported and the additional URL and
	 * ClassLoader support infrastructure can be optimized.
	 * @return if the jar is exploded.
	 */
	protected boolean isExploded() {
		Archive archive = getArchive();
		return archive != null && archive.isExploded();
	}

}