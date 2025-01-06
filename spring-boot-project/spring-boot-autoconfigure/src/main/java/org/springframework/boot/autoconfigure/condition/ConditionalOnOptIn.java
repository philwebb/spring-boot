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

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;
import org.springframework.core.annotation.AliasFor;

/**
 * {@link Conditional @Conditional} that checks that a user has specifically opted-in to a
 * specific feature by looking at the value of an {@code <prefix>.enabled} property. The
 * condition matches only if the user has set the value to true.
 *
 * @author Phillip Webb
 * @since 3.5.0
 * @see ConditionalOnOptOut
 * @see ConditionalOnNoOptOut
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@ConditionalOnProperty(name = "enabled", havingValue = "true")
public @interface ConditionalOnOptIn {

	/**
	 * The property prefix used when checking.
	 * @return the property prefix.
	 */
	@AliasFor(annotation = ConditionalOnProperty.class, attribute = "prefix")
	String value();

}
