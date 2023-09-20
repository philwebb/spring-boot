/*
 * Copyright 2012-2023 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.springframework.boot.loader.jar.NestedJarFile;
import org.springframework.boot.loader.net.protocol.jar.Handler;

/**
 * {@link ClassLoader} used by the {@link Launcher}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 3.2.0
 */
public class LaunchedClassLoader extends URLClassLoader {

	private static final String JAR_MODE_PACKAGE_PREFIX = "org.springframework.boot.loader.jarmode.";

	private static final String JAR_MODE_RUNNER_CLASS_NAME = JarModeRunner.class.getName();

	static {
		ClassLoader.registerAsParallelCapable();
	}

	private final boolean exploded;

	private final Archive rootArchive;

	private final Object definePackageLock = new Object();

	private volatile DefinePackageCallType definePackageCallType;

	private final URL[] urls;

	private final Map<URL, JarFile> jarFiles = new ConcurrentHashMap<>();

	private final Set<String> undefinablePackages = Collections.newSetFromMap(new ConcurrentHashMap<>());

	/**
	 * Create a new {@link LaunchedClassLoader} instance.
	 * @param exploded if the underlying archive is exploded
	 * @param urls the URLs from which to load classes and resources
	 * @param parent the parent class loader for delegation
	 */
	public LaunchedClassLoader(boolean exploded, URL[] urls, ClassLoader parent) {
		this(exploded, null, urls, parent);
	}

	/**
	 * Create a new {@link LaunchedClassLoader} instance.
	 * @param exploded if the underlying archive is exploded
	 * @param rootArchive the root archive or {@code null}
	 * @param urls the URLs from which to load classes and resources
	 * @param parent the parent class loader for delegation
	 */
	public LaunchedClassLoader(boolean exploded, Archive rootArchive, URL[] urls, ClassLoader parent) {
		super(urls, parent);
		this.exploded = exploded;
		this.rootArchive = rootArchive;
		this.urls = urls;
	}

	@Override
	public URL findResource(String name) {
		if (this.exploded) {
			return super.findResource(name);
		}
		Handler.useFastConnectionExceptions(true);
		try {
			return super.findResource(name);
		}
		finally {
			Handler.useFastConnectionExceptions(false);
		}
	}

	@Override
	public Enumeration<URL> findResources(String name) throws IOException {
		if (this.exploded) {
			return super.findResources(name);
		}
		Handler.useFastConnectionExceptions(true);
		try {
			return new UseFastConnectionExceptionsEnumeration(super.findResources(name));
		}
		finally {
			Handler.useFastConnectionExceptions(false);
		}
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (name.startsWith(JAR_MODE_PACKAGE_PREFIX) || name.equals(JAR_MODE_RUNNER_CLASS_NAME)) {
			try {
				Class<?> result = loadClassInLaunchedClassLoader(name);
				if (resolve) {
					resolveClass(result);
				}
				return result;
			}
			catch (ClassNotFoundException ex) {
				// Ignore
			}
		}
		if (this.exploded) {
			return super.loadClass(name, resolve);
		}
		Handler.useFastConnectionExceptions(true);
		try {
			try {
				definePackageIfNecessary(name);
			}
			catch (IllegalArgumentException ex) {
				tolerateRaceConditionDueToBeingParallelCapable(ex, name);
			}
			return super.loadClass(name, resolve);
		}
		finally {
			Handler.useFastConnectionExceptions(false);
		}
	}

	private Class<?> loadClassInLaunchedClassLoader(String name) throws ClassNotFoundException {
		try {
			String internalName = name.replace('.', '/') + ".class";
			try (InputStream inputStream = getParent().getResourceAsStream(internalName);
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				if (inputStream == null) {
					throw new ClassNotFoundException(name);
				}
				inputStream.transferTo(outputStream);
				byte[] bytes = outputStream.toByteArray();
				Class<?> definedClass = defineClass(name, bytes, 0, bytes.length);
				definePackageIfNecessary(name);
				return definedClass;
			}
		}
		catch (IOException ex) {
			throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
		}
	}

	/**
	 * Define a package before a {@code findClass} call is made. This is necessary to
	 * ensure that the appropriate manifest for nested JARs is associated with the
	 * package.
	 * @param className the class name being found
	 */
	private void definePackageIfNecessary(String className) {
		if (className.startsWith("java.")) {
			return;
		}
		int lastDot = className.lastIndexOf('.');
		if (lastDot >= 0) {
			String packageName = className.substring(0, lastDot);
			if (getDefinedPackage(packageName) == null) {
				try {
					definePackage(className, packageName);
				}
				catch (IllegalArgumentException ex) {
					tolerateRaceConditionDueToBeingParallelCapable(ex, packageName);
				}
			}
		}
	}

	private void tolerateRaceConditionDueToBeingParallelCapable(IllegalArgumentException ex, String packageName)
			throws AssertionError {
		if (getDefinedPackage(packageName) == null) {
			// This should never happen as the IllegalArgumentException indicates that the
			// package has already been defined and, therefore, getDefinedPackage(name)
			// should not have returned null.
			throw new AssertionError(
					"Package %s has already been defined but it could not be found".formatted(packageName), ex);
		}
	}

	private void definePackage(String className, String packageName) {
		if (this.undefinablePackages.contains(packageName)) {
			return;
		}
		String packageEntryName = packageName.replace('.', '/') + "/";
		String classEntryName = className.replace('.', '/') + ".class";
		for (URL url : this.urls) {
			try {
				JarFile jarFile = getJarFile(url);
				if (jarFile != null) {
					if (hasEntry(jarFile, classEntryName) && hasEntry(jarFile, packageEntryName)
							&& jarFile.getManifest() != null) {
						definePackage(packageName, jarFile.getManifest(), url);
						return;
					}
				}
			}
			catch (IOException ex) {
				// Ignore
			}
		}
		this.undefinablePackages.add(packageName);
	}

	private boolean hasEntry(JarFile jarFile, String name) {
		return (jarFile instanceof NestedJarFile nestedJarFile) ? nestedJarFile.hasEntry(name)
				: jarFile.getEntry(name) != null;
	}

	private JarFile getJarFile(URL url) throws IOException {
		JarFile jarFile = this.jarFiles.get(url);
		if (jarFile != null) {
			return jarFile;
		}
		URLConnection connection = url.openConnection();
		if (!(connection instanceof JarURLConnection)) {
			return null;
		}
		connection.setUseCaches(false);
		jarFile = ((JarURLConnection) connection).getJarFile();
		synchronized (this.jarFiles) {
			JarFile previous = this.jarFiles.putIfAbsent(url, jarFile);
			if (previous != null) {
				jarFile.close();
				jarFile = previous;
			}
		}
		return jarFile;
	}

	@Override
	protected Package definePackage(String name, Manifest man, URL url) throws IllegalArgumentException {
		if (!this.exploded) {
			return super.definePackage(name, man, url);
		}
		synchronized (this.definePackageLock) {
			return definePackage(DefinePackageCallType.MANIFEST, () -> super.definePackage(name, man, url));
		}
	}

	@Override
	protected Package definePackage(String name, String specTitle, String specVersion, String specVendor,
			String implTitle, String implVersion, String implVendor, URL sealBase) throws IllegalArgumentException {
		if (!this.exploded) {
			return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor,
					sealBase);
		}
		synchronized (this.definePackageLock) {
			if (this.definePackageCallType == null) {
				// We're not part of a call chain which means that the URLClassLoader
				// is trying to define a package for our exploded JAR. We use the
				// manifest version to ensure package attributes are set
				Manifest manifest = getManifest(this.rootArchive);
				if (manifest != null) {
					return definePackage(name, manifest, sealBase);
				}
			}
			return definePackage(DefinePackageCallType.ATTRIBUTES, () -> super.definePackage(name, specTitle,
					specVersion, specVendor, implTitle, implVersion, implVendor, sealBase));
		}
	}

	private Manifest getManifest(Archive archive) {
		try {
			return (archive != null) ? archive.getManifest() : null;
		}
		catch (IOException ex) {
			return null;
		}
	}

	private <T> T definePackage(DefinePackageCallType type, Supplier<T> call) {
		DefinePackageCallType existingType = this.definePackageCallType;
		try {
			this.definePackageCallType = type;
			return call.get();
		}
		finally {
			this.definePackageCallType = existingType;
		}
	}

	/**
	 * Clear any caches. This method is called reflectively by
	 * {@code ClearCachesApplicationListener}.
	 */
	public void clearCache() {
		try {
			clearJarFiles();
		}
		catch (IOException ex) {
			// Ignore
		}
		if (this.exploded) {
			return;
		}
		org.springframework.boot.loader.net.protocol.jar.Handler.clearCache();
		org.springframework.boot.loader.net.protocol.nested.Handler.clearCache();
		for (URL url : getURLs()) {
			try {
				URLConnection connection = url.openConnection();
				if (connection instanceof JarURLConnection jarUrlConnection) {
					clearCache(jarUrlConnection);
				}
			}
			catch (IOException ex) {
				// Ignore
			}
		}
	}

	private void clearCache(JarURLConnection connection) throws IOException {
		JarFile jarFile = connection.getJarFile();
		if (jarFile instanceof NestedJarFile nestedJarFile) {
			nestedJarFile.clearCache();
		}
	}

	@Override
	public void close() throws IOException {
		super.close();
		clearJarFiles();
	}

	private void clearJarFiles() throws IOException {
		synchronized (this.jarFiles) {
			for (JarFile jarFile : this.jarFiles.values()) {
				jarFile.close();
			}
			this.jarFiles.clear();
		}
	}

	/**
	 * {@link Enumeration} that uses fast connections.
	 */
	private static class UseFastConnectionExceptionsEnumeration implements Enumeration<URL> {

		private final Enumeration<URL> delegate;

		UseFastConnectionExceptionsEnumeration(Enumeration<URL> delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean hasMoreElements() {
			Handler.useFastConnectionExceptions(true);
			try {
				return this.delegate.hasMoreElements();
			}
			finally {
				Handler.useFastConnectionExceptions(false);
			}

		}

		@Override
		public URL nextElement() {
			Handler.useFastConnectionExceptions(true);
			try {
				return this.delegate.nextElement();
			}
			finally {
				Handler.useFastConnectionExceptions(false);
			}
		}

	}

	/**
	 * The different types of call made to define a package. We track these for exploded
	 * jars so that we can detect packages that should have manifest attributes applied.
	 */
	private enum DefinePackageCallType {

		/**
		 * A define package call from a resource that has a manifest.
		 */
		MANIFEST,

		/**
		 * A define package call with a direct set of attributes.
		 */
		ATTRIBUTES

	}

}
