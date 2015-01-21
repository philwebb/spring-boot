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

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Parent last (child first) classloader for the specified URLs.
 *
 * @author Phillip Webb
 * @author Andy Clement
 */
public class ReloadClassLoader extends URLClassLoader {

	public ReloadClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	@Override
	public URL getResource(String name) {
		URL resource = findResource(name);
		if (resource == null) {
			ClassLoader parent = getParent();
			if (parent != null) {
				resource = parent.getResource(name);
			}
		}
		return resource;
	}

	@Override
	public synchronized Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		Class<?> clz = findLoadedClass(name);
		if (clz == null) {
			try {
				clz = findClass(name);
			}
			catch (ClassNotFoundException cnfe) {
				ClassLoader parent = getParent();
				if (parent != null) {
					clz = parent.loadClass(name);
				}
				else {
					clz = getSystemClassLoader().loadClass(name);
				}
			}
		}
		if (resolve) {
			resolveClass(clz);
		}
		return clz;
	}
}
