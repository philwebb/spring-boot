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

package sample.jetty;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;

public class LoadAndUnloadMain {

	public static void mainx(String... args) throws ClassNotFoundException,
			NoSuchFieldException, IllegalAccessException, InterruptedException {
		URL url = LoadAndUnloadMain.class.getProtectionDomain().getCodeSource()
				.getLocation();
		final String className = LoadAndUnloadMain.class.getPackage().getName()
				+ ".UtilityClass";
		{

			for (int i = 0; i < 2; i++) {
				ClassLoader cl = new CustomClassLoader(url);
				Class clazz = cl.loadClass(className);
				loadClass(clazz);

				cl = new CustomClassLoader(url);
				clazz = cl.loadClass(className);
				loadClass(clazz);
				triggerGC();
			}
		}
		triggerGC();
	}

	private static void triggerGC() throws InterruptedException {
		System.out.println("\n-- Starting GC");
		System.gc();
		Thread.sleep(100);
		System.out.println("-- End of GC\n");
	}

	private static void loadClass(Class clazz) throws NoSuchFieldException,
			IllegalAccessException {
		final Field id = clazz.getDeclaredField("ID");
		id.setAccessible(true);
		id.get(null);
	}

	private static class CustomClassLoader extends URLClassLoader {
		public CustomClassLoader(URL url) {
			super(new URL[] { url }, null);
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve)
				throws ClassNotFoundException {
			try {
				return super.loadClass(name, resolve);
			}
			catch (ClassNotFoundException e) {
				return Class.forName(name, resolve,
						LoadAndUnloadMain.class.getClassLoader());
			}
		}

		@Override
		protected void finalize() throws Throwable {
			super.finalize();
			System.out.println(this.toString() + " - CL Finalized.");
		}
	}
}

class UtilityClass {

	static final String ID = Integer.toHexString(System
			.identityHashCode(UtilityClass.class));

	private static final Object FINAL = new Object() {
		@Override
		protected void finalize() throws Throwable {
			super.finalize();
			System.out.println(ID + " Finalized.");
		}
	};

	static {
		System.out.println(ID + " Initialising");
	}
}
