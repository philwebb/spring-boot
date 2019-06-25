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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import io.spring.annex.Has;

import org.springframework.core.annotation.AnnotationRegistry;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * {@link AnnotationRegistry} that checks for the Annex {@link Has @Has} annotation.
 *
 * @author Phillip Webb
 */
class AnnexAnnotationRegistry implements AnnotationRegistry {

	private static final String FILE_PREFIX = "file:";

	private static final String INDICATOR_RESOURCE = "META-INF/spring-annex";

	private final Map<ClassLoader, Set<String>> jarPathsCache = new ConcurrentReferenceHashMap<>();

	@Override
	public boolean canSkipIntrospection(Class<?> type, Class<? extends Annotation> annotationType) {
		return canSkipIntrospection(type,
				(memberAnnotation) -> MergedAnnotations.isPresent(memberAnnotation, annotationType));
	}

	@Override
	public boolean canSkipIntrospection(Class<?> type, String annotationName) {
		return canSkipIntrospection(type,
				(memberAnnotation) -> MergedAnnotations.isPresent(memberAnnotation, annotationName));
	}

	private boolean canSkipIntrospection(Class<?> type, Predicate<Class<? extends Annotation>> isPresent) {
		if (!isInProcessedJar(type)) {
			return false;
		}
		try {
			Has has = type.getDeclaredAnnotation(Has.class);
			if (has == null) {
				return true;
			}
			Class<? extends Annotation>[] memberAnnotations = has.value();
			for (Class<? extends Annotation> memberAnnotation : memberAnnotations) {
				if (isPresent.test(memberAnnotation)) {
					return true;
				}
			}
		}
		catch (TypeNotPresentException ex) {
		}
		return false;
	}

	private boolean isInProcessedJar(Class<?> type) {
		ProtectionDomain domain = type.getProtectionDomain();
		CodeSource codeSource = (domain != null) ? domain.getCodeSource() : null;
		URL location = (codeSource != null) ? codeSource.getLocation() : null;
		String path = (location != null) ? location.getPath() : null;
		return path != null && path.endsWith(".jar") && getJarPaths(type.getClassLoader()).contains(path);
	}

	private Set<String> getJarPaths(ClassLoader classLoader) {
		return this.jarPathsCache.computeIfAbsent(classLoader, this::computeJarPaths);
	}

	private Set<String> computeJarPaths(ClassLoader classLoader) {
		try {
			return compuateJarPaths(classLoader.getResources(INDICATOR_RESOURCE));
		}
		catch (IOException ex) {
			return Collections.emptySet();
		}
	}

	private Set<String> compuateJarPaths(Enumeration<URL> urls) {
		Set<String> paths = new HashSet<>();
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			if (url != null && "jar".equals(url.getProtocol()) && url.getPath().startsWith(FILE_PREFIX)) {
				String path = url.getPath();
				path = path.substring(FILE_PREFIX.length());
				path = path.substring(0, path.length() - (INDICATOR_RESOURCE.length() + 2));
				paths.add(path);
			}
		}
		return paths;
	}

}
