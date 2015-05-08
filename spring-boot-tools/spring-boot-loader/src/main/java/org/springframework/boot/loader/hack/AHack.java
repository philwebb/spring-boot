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

package org.springframework.boot.loader.hack;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;

import org.springframework.boot.loader.LaunchedURLClassLoader;

/**
 * @author pwebb
 */
public class AHack {

	public static void main(String[] args) throws Exception {
		URL[] urls = new URL[] { new URL(
				"file:///Users/pwebb/.m2/repository/org/springframework/spring-core/4.2.0.BUILD-SNAPSHOT/spring-core-4.2.0.BUILD-SNAPSHOT.jar") };
		ClassLoader classLoader = new LaunchedURLClassLoader(urls, Thread.currentThread()
				.getContextClassLoader());
		// CustomClassLoader classLoader = new CustomClassLoader();
		{
			Class<?> loadClass = classLoader
					.loadClass("org.springframework.util.ReflectionUtils");
			Method m = loadClass.getDeclaredMethod("getAllDeclaredMethods", Class.class);
			m.invoke(null, HashMap.class);
			m = null;
			loadClass = null;
		}
		classLoader = new CustomClassLoader();
		triggerGC();
		Thread.sleep(500);
	}

	private static void triggerGC() throws InterruptedException {
		System.out.println("\n-- Starting GC");
		System.gc();
		Thread.sleep(100);
		System.out.println("-- End of GC\n");
	}

	private static class CustomClassLoader extends URLClassLoader {
		public CustomClassLoader() {
			super(new URL[] {}, null);
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve)
				throws ClassNotFoundException {
			try {
				return super.loadClass(name, resolve);
			}
			catch (ClassNotFoundException e) {
				e.printStackTrace();
				throw new IllegalStateException();
			}
		}

		@Override
		protected void finalize() throws Throwable {
			super.finalize();
			System.out.println(this.toString() + " - CL Finalized.");
		}
	}
}
