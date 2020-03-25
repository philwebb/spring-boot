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
 * {@link BootManifestAttributes} for non-layered layouts.
 *
 * @author Phillip Webb
 */
class NonLayeredBootManifestAttributes extends BootManifestAttributes {

	static final String CLASSES = "Spring-Boot-Classes";

	static final String LIB = "Spring-Boot-Lib";

	private final String classes;

	private final String lib;

	NonLayeredBootManifestAttributes(String mainClass, String classPathIndex, String classes, String lib) {
		super(mainClass, classPathIndex);
		this.classes = classes;
		this.lib = lib;
	}

	/**
	 * Return the classes folder location.
	 * @return the classes location
	 */
	String getClasses() {
		return this.classes;
	}

	/**
	 * Return the lib folder location.
	 * @return the lib location
	 */
	String getLib() {
		return this.lib;
	}

}
