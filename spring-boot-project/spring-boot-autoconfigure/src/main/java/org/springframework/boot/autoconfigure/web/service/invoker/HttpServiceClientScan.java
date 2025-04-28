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

package org.springframework.boot.autoconfigure.web.service.invoker;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.service.registry.AbstractHttpServiceRegistrar;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;

/**
 * Configures scanning directives for use with {@link HttpServiceClient @HttpService}
 * classes.
 *
 * @author Phillip Webb
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Repeatable(HttpServiceClientScan.Container.class)
public @interface HttpServiceClientScan {

	/**
	 * Alias for {@link #basePackages}.
	 * <p>
	 * Allows for more concise annotation declarations if no other attributes are needed
	 * &mdash; for example, {@code @ComponentScan("org.my.pkg")} instead of
	 * {@code @ComponentScan(basePackages = "org.my.pkg")}.
	 */
	@AliasFor("basePackages")
	String[] value() default {};

	/**
	 * Base packages to scan for annotated components.
	 * <p>
	 * {@link #value} is an alias for (and mutually exclusive with) this attribute.
	 * <p>
	 * Use {@link #basePackageClasses} for a type-safe alternative to String-based package
	 * names.
	 */
	@AliasFor("value")
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages} for specifying the packages to scan
	 * for annotated components. The package of each class specified will be scanned.
	 * <p>
	 * Consider creating a special no-op marker class or interface in each package that
	 * serves no purpose other than being referenced by this attribute.
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * Specify the type of client to use for the group.
	 * <p>
	 * By default, this is {@link ClientType#UNSPECIFIED} in which case {@code RestClient}
	 * is used, but this default can be reset via
	 * {@link AbstractHttpServiceRegistrar#setDefaultClientType}.
	 */
	ClientType clientType() default ClientType.UNSPECIFIED;

	/**
	 * Repeatable container for {@link HttpServiceClientScan @HttpServiceScan}
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Documented
	@interface Container {

		HttpServiceClientScan[] value();

	}

}
