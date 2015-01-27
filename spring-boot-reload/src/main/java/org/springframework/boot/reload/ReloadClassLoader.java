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

	public ReloadClassLoader(ReloadableUrls reloadableUrls, ClassLoader parent) {
		super(reloadableUrls.toArray(), parent);
	}

	@Override
	public URL getResource(String name) {
		URL resource = findResource(name);
		if (resource != null) {
			return resource;
		}
		return getParent().getResource(name);
	}

	@Override
	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class<?> loadedClass = findLoadedClass(name);
		if (loadedClass == null) {
			try {
				loadedClass = findClass(name);
			}
			catch (ClassNotFoundException ex) {
				loadedClass = getParent().loadClass(name);
			}
		}
		if (resolve) {
			resolveClass(loadedClass);
		}
		return loadedClass;
	}

}
