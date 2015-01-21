/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * Strategy class used to implement automatic reloading for Spring Boot application.
 * Implementations should be registered vis Java's {@link ServiceLoader} mechanism.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public abstract class Reloader {

	/**
	 * Called to prepare the reloader. This method will be called for each located
	 * {@link Reloader} before any {@link #start(String[])} methods are called.
	 * @param reloaders an immutable list of {@link Reloader} instances in the order that
	 * they will be started.
	 */
	protected void prepare(List<Reloader> reloaders) {
	}

	/**
	 * Called to start the reloader.
	 * @param args the application arguments
	 */
	protected abstract void start(String[] args);

	/**
	 * Apply all reloading strategies located on the classpath.
	 * @param args the application arguments
	 */
	public static void apply(String[] args) {
		List<Reloader> reloaders = new ArrayList<Reloader>();
		for (Reloader reloader : ServiceLoader.load(Reloader.class)) {
			reloaders.add(reloader);
		}
		AnnotationAwareOrderComparator.sort(reloaders);
		List<Reloader> unmodifiableReloaders = Collections.unmodifiableList(reloaders);
		for (Reloader reloader : reloaders) {
			reloader.prepare(unmodifiableReloaders);
		}
		for (Reloader reloader : reloaders) {
			reloader.start(args);
		}
	}

}
