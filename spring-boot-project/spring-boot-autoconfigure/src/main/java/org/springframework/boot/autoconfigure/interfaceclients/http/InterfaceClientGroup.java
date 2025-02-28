/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.interfaceclients.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.type.filter.TypeFilter;

/**
 * Registers an HTTP service client along with associated interface clients. Scans the
 * listed packages for {@link @HttpExchange}-annotated interfaces to add or adds the
 * directly provided interfaces to the client.
 *
 * @author Olga Maciaszek-Sharma
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Repeatable(EnableInterfaceClients.class)
public @interface InterfaceClientGroup {

	/**
	 * The {@code name} of the host or service the client communicates with.
	 * @return client name
	 */
	String value();

	// TODO*: baseUrl from name; make optional

	/**
	 * Name of the client. If not provided, resolved from url host value.
	 * @return An absolute URL or resolvable serviceId
	 */
	String baseUrl();

	/**
	 * Base packages to scan for annotated components. Use {@link #basePackageClasses()}
	 * for a type-safe alternative to String-based package names.
	 * @return the array of 'basePackages'
	 */
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages()} for specifying the packages to
	 * scan for annotated components. The package of each class specified will be scanned.
	 * <p>
	 * Consider creating a special no-op marker class or interface in each package that
	 * serves no purpose other than being referenced by this attribute.
	 * @return the array of 'basePackageClasses'
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * List of interface types to instantiate for the client. If not empty, disables
	 * classpath scanning.
	 * @return an array of {@link org.springframework.web.service.annotation.HttpExchange}
	 * classes
	 */
	Class<?>[] httpServiceTypes() default {};

	// TODO*: decide if we need it; if yes, add matching implementation

	/**
	 * Specifies which types are eligible for component scanning.
	 */
	InterfaceFilter[] includeFilters() default {};

	// TODO*: decide if we need it; if yes, add matching implementation

	/**
	 * Specifies which types are not eligible for Interface Client scanning.
	 */
	InterfaceFilter[] excludeFilters() default {};

	// TODO*: not sure if we even need it

	/**
	 * Declares the type filter to be used as an
	 * {@linkplain InterfaceClientGroup#includeFilters include filter} or
	 * {@linkplain InterfaceClientGroup#excludeFilters exclude filter}. Mirrors the
	 * behaviour of {@link ComponentScan.Filter}. Code based on
	 * {@link ComponentScan.Filter}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({})
	@interface InterfaceFilter {

		/**
		 * The type of filter to use.
		 * <p>
		 * Default is {@link FilterType#ANNOTATION}.
		 * @see #classes
		 * @see #pattern
		 */
		FilterType type() default FilterType.ANNOTATION;

		/**
		 * Alias for {@link #classes}.
		 * @see #classes
		 */
		@AliasFor("classes")
		Class<?>[] value() default {};

		/**
		 * The class or classes to use as the filter.
		 * <p>
		 * The following table explains how the classes will be interpreted based on the
		 * configured value of the {@link #type} attribute.
		 * <table border="1">
		 * <tr>
		 * <th>{@code FilterType}</th>
		 * <th>Class Interpreted As</th>
		 * </tr>
		 * <tr>
		 * <td>{@link FilterType#ANNOTATION ANNOTATION}</td>
		 * <td>the annotation itself</td>
		 * </tr>
		 * <tr>
		 * <td>{@link FilterType#ASSIGNABLE_TYPE ASSIGNABLE_TYPE}</td>
		 * <td>the type that detected components should be assignable to</td>
		 * </tr>
		 * <tr>
		 * <td>{@link FilterType#CUSTOM CUSTOM}</td>
		 * <td>an implementation of {@link TypeFilter}</td>
		 * </tr>
		 * </table>
		 * <p>
		 * When multiple classes are specified, <em>OR</em> logic is applied &mdash; for
		 * example, "include types annotated with {@code @Foo} OR {@code @Bar}".
		 * <p>
		 * Custom {@link TypeFilter TypeFilters} may optionally implement any of the
		 * following {@link org.springframework.beans.factory.Aware Aware} interfaces, and
		 * their respective methods will be called prior to {@link TypeFilter#match
		 * match}:
		 * <ul>
		 * <li>{@link org.springframework.context.EnvironmentAware EnvironmentAware}</li>
		 * <li>{@link org.springframework.beans.factory.BeanFactoryAware BeanFactoryAware}
		 * <li>{@link org.springframework.beans.factory.BeanClassLoaderAware
		 * BeanClassLoaderAware}
		 * <li>{@link org.springframework.context.ResourceLoaderAware ResourceLoaderAware}
		 * </ul>
		 * <p>
		 * Specifying zero classes is permitted but will have no effect on interface
		 * scanning.
		 * @since 4.2
		 * @see #value
		 * @see #type
		 */
		@AliasFor("value")
		Class<?>[] classes() default {};

		/**
		 * The pattern (or patterns) to use for the filter, as an alternative to
		 * specifying a Class {@link #value}.
		 * <p>
		 * If {@link #type} is set to {@link FilterType#ASPECTJ ASPECTJ}, this is an
		 * AspectJ type pattern expression. If {@link #type} is set to
		 * {@link FilterType#REGEX REGEX}, this is a regex pattern for the fully-qualified
		 * class names to match.
		 * @see #type
		 * @see #classes
		 */
		String[] pattern() default {};

	}

}
