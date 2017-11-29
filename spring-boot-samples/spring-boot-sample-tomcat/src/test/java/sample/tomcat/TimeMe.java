/*
 * Copyright 2012-2017 the original author or authors.
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

package sample.tomcat;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author pwebb
 */
public class TimeMe {

	private static final Method m;

	static {
		try {
			m = Class.class.getDeclaredMethod("getMethod0", String.class, Class[].class,
					Boolean.TYPE);
		}
		catch (NoSuchMethodException e) {
			throw new UnsupportedOperationException("Auto-generated method stub", e);
		}
		catch (SecurityException e) {
			throw new UnsupportedOperationException("Auto-generated method stub", e);
		}

	}

	public static void main(String[] args) {
		String[] random = new String[5000];
		for (int i = 0; i < random.length; i++) {
			random[i] = UUID.randomUUID().toString();
		}
		long t = System.nanoTime();
		for (String name : random) {
			hasMethod(TimeMe.class, name);
		}
		System.out.println(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t));
	}

	private static boolean hasMethod(Class<?> sourceClass, String name,
			Class<?>... paramTypes) {
		// return ClassUtils.hasMethod(sourceClass, name, paramTypes);
		try {
			if (!hasCandidate(sourceClass, name)) {
				return false;
			}
			// return (boolean) m.invoke(sourceClass, name, paramTypes, true);
			return sourceClass.getMethod(name, paramTypes) != null;
		}
		catch (Exception e) {
			return false;
		}
	}

	private static boolean hasCandidate(Class<?> sourceClass, String name) {
		for (Method method : sourceClass.getMethods()) {
			if (method.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

}
