/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.util.PropertyPlaceholderHelper;

/**
 * Common {@link Layout}s.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 1.0.0
 */
public final class Layouts {

	private Layouts() {
	}

	/**
	 * Return a layout for the given source file.
	 * @param file the source file
	 * @return a {@link Layout}
	 */
	public static Layout forFile(File file) {
		if (file == null) {
			throw new IllegalArgumentException("File must not be null");
		}
		String lowerCaseFileName = file.getName().toLowerCase(Locale.ENGLISH);
		if (lowerCaseFileName.endsWith(".jar")) {
			return new Jar();
		}
		if (lowerCaseFileName.endsWith(".war")) {
			return new War();
		}
		if (file.isDirectory() || lowerCaseFileName.endsWith(".zip")) {
			return new Expanded();
		}
		throw new IllegalStateException("Unable to deduce layout for '" + file + "'");
	}

	/**
	 * Executable JAR layout.
	 */
	public static class Jar implements RepackagingLayout {

		@Override
		public String getLauncherClassName() {
			return "org.springframework.boot.loader.JarLauncher";
		}

		@Override
		public String getLibraryDestination(String libraryName, LibraryScope scope) {
			return "BOOT-INF/lib/";
		}

		@Override
		public String getClassesLocation() {
			return "";
		}

		@Override
		public String getRepackagedClassesLocation() {
			return "BOOT-INF/classes/";
		}

		@Override
		public String getClasspathIndexLocation() {
			return "BOOT-INF/classpath.idx";
		}

		@Override
		public boolean isExecutable() {
			return true;
		}

	}

	/**
	 * Executable JAR layout with support for layers.
	 */
	public static class LayeredJar extends Jar implements LayeredLayout {

		private static final PropertyPlaceholderHelper HELPER = new PropertyPlaceholderHelper("{", "}");

		private static final String LAYERED_LIBRARY_DESTINATION = "BOOT-INF/{layer}/lib/";

		private static final String LAYERED_CLASSES_DESTINATION = "BOOT-INF/{layer}/classes/";

		@Override
		public String getLayeredLibraryDestination(LayerResolver resolver, String libraryName) {
			return HELPER.replacePlaceholders(getLibraryDestination(libraryName, null),
					(placeholderName) -> resolver.resolveLayer(LAYERED_LIBRARY_DESTINATION, libraryName));
		}

		@Override
		public String getLayeredClassesDestination(LayerResolver resolver) {
			return HELPER.replacePlaceholders(getRepackagedClassesLocation(),
					(placeholderName) -> resolver.resolveLayer(LAYERED_CLASSES_DESTINATION, ""));
		}

	}

	/**
	 * Executable expanded archive layout.
	 */
	public static class Expanded extends Jar {

		@Override
		public String getLauncherClassName() {
			return "org.springframework.boot.loader.PropertiesLauncher";
		}

	}

	/**
	 * No layout.
	 */
	public static class None extends Jar {

		@Override
		public String getLauncherClassName() {
			return null;
		}

		@Override
		public boolean isExecutable() {
			return false;
		}

	}

	/**
	 * Executable WAR layout.
	 */
	public static class War implements Layout {

		private static final Map<LibraryScope, String> SCOPE_DESTINATIONS;

		static {
			Map<LibraryScope, String> map = new HashMap<>();
			map.put(LibraryScope.COMPILE, "WEB-INF/lib/");
			map.put(LibraryScope.CUSTOM, "WEB-INF/lib/");
			map.put(LibraryScope.RUNTIME, "WEB-INF/lib/");
			map.put(LibraryScope.PROVIDED, "WEB-INF/lib-provided/");
			SCOPE_DESTINATIONS = Collections.unmodifiableMap(map);
		}

		@Override
		public String getLauncherClassName() {
			return "org.springframework.boot.loader.WarLauncher";
		}

		@Override
		public String getLibraryDestination(String libraryName, LibraryScope scope) {
			return SCOPE_DESTINATIONS.get(scope);
		}

		@Override
		public String getClassesLocation() {
			return "WEB-INF/classes/";
		}

		@Override
		public boolean isExecutable() {
			return true;
		}

	}

}
