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

package org.springframework.boot.context.annotation;

import java.util.Set;

import org.springframework.beans.factory.Aware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * An {@link ImportSelector} that can determine imports early and without the need for
 * {@link Aware} callbacks being made. The standard {@link ImportSelector} interface is
 * quite flexible which can make it hard to tell exactly what imports they will add. This
 * interface should be used when an implementation can determine imports early.
 * <p>
 * Using {@link DeterminableImportSelector} is particularly useful when working with
 * Spring's testing support. It allows for better generation of {@link ApplicationContext}
 * cache keys.
 *
 * @author Phillip Webb
 * @since 1.5.0
 */
public interface DeterminableImportSelector extends ImportSelector {

	/**
	 * Determine the imports to use. Unlike {@link ImportSelector}, any {@link Aware}
	 * callbacks will not be invoked before this method is called.
	 * @param metadata the source meta-data
	 * @return a set of the imports that will be selected, not necessarily in the same
	 * order as {@link #selectImports(AnnotationMetadata)}
	 */
	Set<String> determineImports(AnnotationMetadata metadata);

}
