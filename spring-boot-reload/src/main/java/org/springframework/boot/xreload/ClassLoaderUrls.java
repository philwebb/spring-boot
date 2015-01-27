/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.xreload;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * A collection of fixed and potentially reloadable classloader URLs.
 *
 * @author Phillip Webb
 */
public class ClassLoaderUrls {

	private final List<URL> reloadable;

	private final List<URL> fixed;

	public ClassLoaderUrls(URLClassLoader classLoader) {
		this(classLoader.getURLs());
	}

	public ClassLoaderUrls(URL[] urls) {
		this.fixed = new ArrayList<URL>(urls.length);
		this.reloadable = new ArrayList<URL>(urls.length);
		for (URL url : urls) {
			if (isReloadable(url)) {
				this.reloadable.add(url);
			}
			else {
				this.fixed.add(url);
			}
		}
	}

	private boolean isReloadable(URL url) {
		String s = url.toString();
		return s.startsWith("file:") && s.endsWith("/");
	}

	public URL[] getFixed() {
		return this.fixed.toArray(new URL[this.fixed.size()]);
	}

	public URL[] getReloadable() {
		// FIXME might not need
		return this.reloadable.toArray(new URL[this.reloadable.size()]);
	}
}
