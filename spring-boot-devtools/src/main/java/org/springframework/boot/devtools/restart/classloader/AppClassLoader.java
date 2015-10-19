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

package org.springframework.boot.devtools.restart.classloader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Phillip Webb
 * @since 1.3.0
 */
public class AppClassLoader extends URLClassLoader {

	private ClassLoader restartClassLoader;

	private AppClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		boolean b = false;
		if (name.endsWith("MyDomainObject") || name.endsWith("ObjectInputStream")) {
			System.err.println("Loading " + name + " from " + this);
			b = true;
		}
		Class<?> loaded = x(name);
		if (b) {
			System.err.println("Loaded " + loaded + " from " + loaded.getClassLoader());
		}
		return loaded;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		Class<?> class1 = super.findClass(name);
		if (name.endsWith("MyDomainObject") || name.endsWith("ObjectInputStream")) {
			System.err.println("Found " + name + " from " + this);
		}
		return class1;
	}

	/**
	 * @param name
	 * @return
	 * @throws ClassNotFoundException
	 */
	private Class<?> x(String name) throws ClassNotFoundException {
		if (name.equals(this.getClass().getName())) {
			return this.getClass();
		}
		if (this.restartClassLoader != null) {
			if (name.equals(this.restartClassLoader.getClass().getName())) {
				return this.restartClassLoader.getClass();
			}
			return this.restartClassLoader.loadClass(name);
		}
		return super.loadClass(name);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		if (name.endsWith("MyDomainObject")) {
			System.err.println("Loading " + name + " from " + this);
		}
		return super.loadClass(name, resolve);
	}

	public void setRestartLoader(ClassLoader restartClassLoader) {
		this.restartClassLoader = restartClassLoader;
	}

	public Class<?> loadApplicationClass(String name) throws ClassNotFoundException {
		return super.loadClass(name);
	}

	public static boolean canApplyTo(ClassLoader classLoader) {
		return !isAlreadyApplied(classLoader) && classLoader instanceof URLClassLoader;
	}

	private static boolean isAlreadyApplied(ClassLoader classLoader) {
		while (classLoader != null) {
			if (classLoader.getClass().getName().equals(AppClassLoader.class.getName())) {
				return true;
			}
			classLoader = classLoader.getParent();
		}
		return false;

	}

	public static ClassLoader apply(ClassLoader classLoader) {
		return new AppClassLoader(((URLClassLoader) classLoader).getURLs(),
				classLoader.getParent());
	}

}
