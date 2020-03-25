/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.gradle.tasks.bundling;

/**
 * Base interface for Spring Boot manifest attributes.
 *
 * @author Phillip Webb
 * @see NonLayeredBootManifestAttributes
 * @see LayeredBootManifestAttributes
 * @see BootArchiveSupport
 */
abstract class BootManifestAttributes {

	static final String START_CLASS = "Start-Class";

	static final String BOOT_VERSION = "Spring-Boot-Version";

	static final String CLASSPATH_INDEX = "Spring-Boot-Classpath-Index";

	private final String mainClass;

	private final String classPathIndex;

	BootManifestAttributes(String mainClass, String classPathIndex) {
		this.mainClass = mainClass;
		this.classPathIndex = classPathIndex;
	}

	/**
	 * Return the main application class name.
	 * @return the main class
	 */
	String getMainClass() {
		return this.mainClass;
	}

	/**
	 * Return the classpath index or {@code null} if no index is provided.
	 * @return the classpath index
	 */
	String getClassPathIndex() {
		return this.classPathIndex;

	}

}
