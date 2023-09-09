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

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.boot.loader.net.protocol.jar.Handler;

/**
 * Base class for launchers that can start an application with a fully configured
 * classpath backed by one or more {@link Archive} instances.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @since 3.2.0
 */
public abstract class Launcher {

	private static final String JAR_MODE_LAUNCHER = "org.springframework.boot.loader.jarmode.JarModeLauncher";

	protected static final int DEFAULT_NUMBER_OF_CLASSPATH_URLS = 50;

	/**
	 * Launch the application. This method is the initial entry point that should be
	 * called by a subclass {@code public static void main(String[] args)} method.
	 * @param args the incoming arguments
	 * @throws Exception if the application fails to launch
	 */
	protected void launch(String[] args) throws Exception {
		if (!isExploded()) {
			Handler.register();
		}
		ClassLoader classLoader = createClassLoader(getArchives());
		String jarMode = System.getProperty("jarmode");
		String launchClass = (jarMode != null && !jarMode.isEmpty()) ? JAR_MODE_LAUNCHER : getMainClass();
		launch(classLoader, launchClass, args);
	}

	/**
	 * Create a classloader for the specified archives.
	 * @param archives the archives
	 * @return the classloader
	 * @throws Exception if the classloader cannot be created
	 */
	protected ClassLoader createClassLoader(Iterator<Archive> archives) throws Exception {
		List<URL> urls = new ArrayList<>(DEFAULT_NUMBER_OF_CLASSPATH_URLS);
		archives.forEachRemaining((archive) -> urls.add(archive.getUrl()));
		return createClassLoader(urls.toArray(new URL[0]));
	}

	/**
	 * Create a classloader for the specified URLs.
	 * @param urls the URLs
	 * @return the classloader
	 * @throws Exception if the classloader cannot be created
	 */
	protected ClassLoader createClassLoader(URL[] urls) throws Exception {
		return new LaunchedURLClassLoader(isExploded(), getRootArchive(), urls, getClass().getClassLoader());
	}

	/**
	 * Launch the application given the archive file and a fully configured classloader.
	 * @param classLoader the classloader
	 * @param launchClass the launch class to run
	 * @param args the incoming arguments
	 * @throws Exception if the launch fails
	 */
	protected void launch(ClassLoader classLoader, String launchClass, String[] args) throws Exception {
		Thread.currentThread().setContextClassLoader(classLoader);
		createMainMethodRunner(classLoader, launchClass, args).run();
	}

	/**
	 * Create the {@code MainMethodRunner} used to launch the application.
	 * @param classLoader the classloader
	 * @param mainClass the main class
	 * @param args the incoming arguments
	 * @return the main method runner
	 */
	protected MainMethodRunner createMainMethodRunner(ClassLoader classLoader, String mainClass, String[] args) {
		return new MainMethodRunner(mainClass, args);
	}

	/**
	 * Returns the main class that should be launched.
	 * @return the name of the main class
	 * @throws Exception if the main class cannot be obtained
	 */
	protected abstract String getMainClass() throws Exception;

	/**
	 * Returns the archives that will be used to construct the class path.
	 * @return the class path archives
	 * @throws Exception if the class path archives cannot be obtained
	 */
	protected abstract Iterator<Archive> getArchives() throws Exception;

	/**
	 * Return the root archive.
	 * @return the root archive
	 */
	protected Archive getRootArchive() {
		return null;
	}

	/**
	 * Returns if the launcher is running in an exploded mode. If this method returns
	 * {@code true} then only regular JARs are supported and the additional URL and
	 * ClassLoader support infrastructure can be optimized.
	 * @return if the jar is exploded.
	 */
	protected boolean isExploded() {
		return false;
	}

	/**
	 * Factory method to create an appropriate {@link Archive} must on the running code.
	 * @return an {@link Archive} instance
	 * @throws Exception if the archive cannot be created
	 */
	protected static Archive createArchive() throws Exception {
		ProtectionDomain protectionDomain = Launcher.class.getProtectionDomain();
		CodeSource codeSource = protectionDomain.getCodeSource();
		URI location = (codeSource != null) ? codeSource.getLocation().toURI() : null;
		String path = (location != null) ? location.getSchemeSpecificPart() : null;
		if (path == null) {
			throw new IllegalStateException("Unable to determine code source archive");
		}
		File root = new File(path);
		if (!root.exists()) {
			throw new IllegalStateException("Unable to determine code source archive from " + root);
		}
		return (root.isDirectory() ? new ExplodedArchive(root) : new JarFileArchive(root));
	}

}
