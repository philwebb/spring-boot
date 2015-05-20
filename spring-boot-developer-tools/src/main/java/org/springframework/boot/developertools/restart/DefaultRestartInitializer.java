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

package org.springframework.boot.developertools.restart;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Default {@link RestartInitializer}.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class DefaultRestartInitializer implements RestartInitializer {

	private static final Set<String> SKIPPED_STACK_ELEMENTS;
	static {
		Set<String> skipped = new LinkedHashSet<String>();
		skipped.add("org.junit.runners.");
		skipped.add("org.springframework.boot.test.");
		SKIPPED_STACK_ELEMENTS = Collections.unmodifiableSet(skipped);
	}

	@Override
	public URL[] getInitialUrls(Thread thread) {
		if (!thread.getName().equals("main")) {
			return null;
		}
		if (!thread.getContextClassLoader().getClass().getName()
				.contains("AppClassLoader")) {
			return null;
		}
		for (StackTraceElement element : thread.getStackTrace()) {
			for (String skipped : SKIPPED_STACK_ELEMENTS) {
				if (element.getClassName().startsWith(skipped)) {
					return null;
				}
			}
		}
		return ChangeableUrls.fromUrlClassLoader(
				(URLClassLoader) thread.getContextClassLoader()).toArray();
	}

}
