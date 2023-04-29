/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.testcontainers;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.testcontainers.containers.Container;

import org.springframework.context.annotation.Import;

/**
 * Imports {@link Container} bean definitions from the static fields declared in a class
 * or interface.
 *
 * @author Phillip Webb
 * @since 3.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ImportTestcontainersRegistrar.class)
public @interface ImportTestcontainers {

	/**
	 * The classes that should be searched for {@link Container} fields. Each class must
	 * declare at least one static {@link Container}. An exception will be raised if and
	 * any non-static {@link Container} fields are found or if any fields values are
	 * {@code null}.
	 * <p>
	 * If no {@code value} is defined then the class that declares the
	 * {@code ImportTestcontainers @ImportTestcontainers} will be searched.
	 * @return the classes to search for static {@link Container} fields
	 */
	Class<?>[] value() default {};

}
