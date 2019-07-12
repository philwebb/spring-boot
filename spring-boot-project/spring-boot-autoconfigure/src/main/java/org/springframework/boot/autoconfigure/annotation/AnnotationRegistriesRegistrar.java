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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.AnnotationRegistries;
import org.springframework.core.annotation.AnnotationRegistry;

/**
 * Registers Spring Boot annotation registries.
 *
 * @author Phillip Webb
 */
class AnnotationRegistriesRegistrar implements ApplicationListener<ApplicationStartingEvent> {

	private static final Set<AnnotationRegistry> REGISTRIES;
	static {
		Set<AnnotationRegistry> registries = new LinkedHashSet<>();
		registries.add(new ThirdPartyLibraryAnnotationRegistry());
		registries.add(new AnnexAnnotationRegistry());
		REGISTRIES = Collections.unmodifiableSet(registries);
	}

	@Override
	public void onApplicationEvent(ApplicationStartingEvent event) {
		REGISTRIES.forEach(AnnotationRegistries::add);
	}

}
