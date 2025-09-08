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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.service.registry.AbstractHttpServiceRegistrar;
import org.springframework.web.service.registry.HttpServiceGroup;

/**
 * Indicates that an annotated interface is a "HTTP Service client" and may be registered
 * with a {@link AbstractHttpServiceRegistrar}.
 * <p>
 * This annotation allows HTTP Service clients to be autodetected though classpath
 * scanning, typically configured with
 * {@link HttpServiceClientScan @HttpServiceClientScan}.
 * <p>
 * This annotation should <b>only</b> be used on interfaces that are not imported directly
 * into the HTTP Service registrar.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface HttpServiceClient {

	/**
	 * Alias for {@link #group()}.
	 * @return the HTTP Service group name
	 */
	@AliasFor("group")
	String value() default HttpServiceGroup.DEFAULT_GROUP_NAME;

	/**
	 * The name of the HTTP Service group.
	 * <p>
	 * If not specified, declared HTTP Services are grouped under the
	 * {@link HttpServiceGroup#DEFAULT_GROUP_NAME}.
	 * @return the HTTP Service group name
	 */
	@AliasFor("value")
	String group() default HttpServiceGroup.DEFAULT_GROUP_NAME;

}
