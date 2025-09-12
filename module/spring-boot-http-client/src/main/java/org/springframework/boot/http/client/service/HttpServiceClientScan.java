/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.http.client.service;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Annotation to scan for {@link HttpServiceClient @HttpServiceClient} annotated
 * interfaces which can be used as client proxies, and have those proxies registered as
 * beans.
 * <p>
 * This interface can be used as an alternative to
 * {@link ImportHttpServices @ImportHttpServices} when the
 * {@link HttpExchange @HttpExchange} interface is <em>only</em> designed for client use.
 * <p>
 * Discovered HTTP Services will be registered under the group specified in the
 * {@link HttpServiceClient @HttpServiceClient} annotation.
 * <p>
 * HTTP Service clients imported by this annotation should <b>not</b> be also be
 * registered directly.
 *
 * @author Phillip Webb
 * @see ImportHttpServices
 * @since 4.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Repeatable(HttpServiceClientScan.Container.class)
@Import(HttpServiceClientScanRegistrar.class)
public @interface HttpServiceClientScan {

	/**
	 * Alias for {@link #basePackages}.
	 * <p>
	 * Allows for more concise annotation declarations if no other attributes are needed
	 * &mdash; for example, {@code @HttpServiceClientScan("org.my.pkg")} instead of
	 * {@code @HttpServiceClientScan(basePackages = "org.my.pkg")}.
	 * @return the base packages
	 */
	@AliasFor("basePackages")
	String[] value() default {};

	/**
	 * Base packages to scan.
	 * <p>
	 * {@link #value} is an alias for (and mutually exclusive with) this attribute.
	 * <p>
	 * Use {@link #basePackageClasses} for a type-safe alternative to String-based package
	 * names.
	 * @return the base packages
	 */
	@AliasFor("value")
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages} for specifying the packages to scan.
	 * The package of each class specified will be scanned.
	 * <p>
	 * Consider creating a special no-op marker class or interface in each package that
	 * serves no purpose other than being referenced by this attribute.
	 * @return the base package classes
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * Specify the type of client to use for the group.
	 * <p>
	 * By default, this is {@link ClientType#UNSPECIFIED}.
	 * @return the client type
	 */
	ClientType clientType() default ClientType.UNSPECIFIED;

	/**
	 * Container annotation that is necessary for the repeatable
	 * {@link HttpServiceClientScan HttpServiceClientScan} annotation, but does not need
	 * to be declared in application code.
	 */
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Import(HttpServiceClientScan.class)
	@interface Container {

		HttpServiceClientScan[] value();

	}

}
