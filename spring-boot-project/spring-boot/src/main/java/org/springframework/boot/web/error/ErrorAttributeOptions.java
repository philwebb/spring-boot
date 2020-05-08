/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.web.error;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Options controlling the contents of {@code ErrorAttributes}.
 *
 * @author Scott Frederick
 * @since 2.3.0
 */
public final class ErrorAttributeOptions {

	private final Set<Include> includes;

	private ErrorAttributeOptions(Set<Include> includes) {
		this.includes = includes;
	}

	public boolean isIncluded(Include include) {
		return this.includes.contains(include);
	}

	public Set<Include> getIncludes() {
		return this.includes;
	}

	public ErrorAttributeOptions including(Include... includes) {
		EnumSet<Include> updated = EnumSet.copyOf(this.includes);
		updated.addAll(Arrays.asList(includes));
		return new ErrorAttributeOptions(Collections.unmodifiableSet(updated));
	}

	public ErrorAttributeOptions excluding(Include... excludes) {
		EnumSet<Include> updated = EnumSet.copyOf(this.includes);
		updated.removeAll(Arrays.asList(excludes));
		return new ErrorAttributeOptions(Collections.unmodifiableSet(updated));
	}

	public static ErrorAttributeOptions defaults() {
		return of();
	}

	public static ErrorAttributeOptions of(Include... includes) {
		return of(Arrays.asList(includes));
	}

	public static ErrorAttributeOptions of(Collection<Include> includes) {
		return new ErrorAttributeOptions(Collections.unmodifiableSet(EnumSet.copyOf(includes)));
	}

	/**
	 * Attributes that can be included.
	 */
	public static enum Include {

		/**
		 * Include the exception class name attribute.
		 */
		EXCEPTION,

		/**
		 * Include the stack trace attribute.
		 */
		STACK_TRACE,

		/**
		 * Include the message attribute.
		 */
		MESSAGE,

		/**
		 * Include the binding errors attribute.
		 */
		BINDING_ERRORS,

	}

}
