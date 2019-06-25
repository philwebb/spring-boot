/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.annotation;

import org.springframework.core.annotation.AnnotationRegistry;

/**
 * {@link AnnotationRegistry} for common third-party libraries.
 *
 * @author Phillip Webb
 */
class ThirdPartyLibraryAnnotationRegistry implements AnnotationRegistry {

	private static final String SPRING_FRAMEWORK_PACKAGE = "org.springframework.";

	// @formatter:off
	private static final String[] SKIP = {
			"com.fasterxml.jackson.core",
			"com.fasterxml.jackson.databind",
			"com.fasterxml.jackson.module",
			"freemarker.core",
			"freemarker.template",
			"io.micrometer.core",
			"javax",
			"org.apache.catalina"
	};
	// @formatter:on

	@Override
	public boolean canSkipIntrospection(Class<?> candidate, String annotationName) {
		String candidateName = candidate.getName();
		if (annotationName.startsWith(SPRING_FRAMEWORK_PACKAGE)
				&& !candidateName.startsWith(SPRING_FRAMEWORK_PACKAGE)) {
			for (String skip : SKIP) {
				if (candidateName.startsWith(skip)) {
					return true;
				}
			}
		}
		return false;
	}

}
