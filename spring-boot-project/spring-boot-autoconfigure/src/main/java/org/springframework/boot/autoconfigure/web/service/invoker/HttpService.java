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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.service.registry.AbstractHttpServiceRegistrar;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Indicates that an annotated class is an HTTP service to be registered with an
 * {@link HttpServiceGroup}.
 * <p>
 * Classes annotated with {@link HttpService @HttpService} are eligible for discovery
 * using {@link HttpServiceScan @HttpServiceScan}.
 * <p>
 * <strong>NOTE:</strong> The {@link #group()} and {@link #clientType()} attributes are
 * only used for scanned services. They will not be considered if the interface is also
 * imported or discovered using {@link ImportHttpServices @ImportHttpServices}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see ImportHttpServices
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HttpService {

	/**
	 * The name of the HTTP Service group.
	 * <p>
	 * If not specified, declared HTTP Services are grouped under the
	 * {@link HttpServiceGroup#DEFAULT_GROUP_NAME}.
	 */
	@AliasFor("group")
	String value() default HttpServiceGroup.DEFAULT_GROUP_NAME;

	/**
	 * The name of the HTTP Service group.
	 * <p>
	 * If not specified, declared HTTP Services are grouped under the
	 * {@link HttpServiceGroup#DEFAULT_GROUP_NAME}.
	 */
	@AliasFor("value")
	String group() default HttpServiceGroup.DEFAULT_GROUP_NAME;

	/**
	 * Specify the type of client to use for the group.
	 * <p>
	 * By default, this is {@link ClientType#UNSPECIFIED} in which case {@code RestClient}
	 * is used, but this default can be reset via
	 * {@link AbstractHttpServiceRegistrar#setDefaultClientType}.
	 */
	ClientType clientType() default ClientType.UNSPECIFIED;

}
