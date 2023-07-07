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

package org.springframework.boot.loader;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.JarFile;

/**
 * @author pwebb
 */
public class Scratch {

	public static void main(String[] args) throws IOException {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		System.out.println(contextClassLoader);
		System.out.println(contextClassLoader.getClass());
		URL resource = contextClassLoader.getResource("META-INF/license.txt");
		System.out.println(resource);
		System.out.println(resource.openConnection().getClass());
		System.out.println(resource);
		System.out.println(((JarURLConnection) resource.openConnection()).getJarFile());
		URL resource2 = contextClassLoader.getResource("META-INF/license.txt");
		URLConnection openConnection = resource2.openConnection();
		JarURLConnection jarURLConnection = (JarURLConnection) resource2.openConnection();
		openConnection.setUseCaches(false);
		JarFile jarFile = jarURLConnection.getJarFile();
		System.out.println(jarFile);
		jarFile.close();
	}

}
