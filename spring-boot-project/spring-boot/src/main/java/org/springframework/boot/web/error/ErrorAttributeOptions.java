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

/**
 * Options controlling the contents of {@code ErrorAttributes}.
 *
 * @author Scott Frederick
 * @since 2.3.0
 */
public class ErrorAttributeOptions {

	private final boolean includeException;

	private final boolean includeStackTrace;

	private final boolean includeMessage;

	private final boolean includeBindingErrors;

	/**
	 * Construct an {@code ErrorAttributeOptions} with default values.
	 */
	public ErrorAttributeOptions() {
		this.includeException = false;
		this.includeStackTrace = false;
		this.includeMessage = false;
		this.includeBindingErrors = false;
	}

	ErrorAttributeOptions(boolean includeException, boolean includeStackTrace, boolean includeMessage,
			boolean includeBindingErrors) {
		this.includeException = includeException;
		this.includeStackTrace = includeStackTrace;
		this.includeMessage = includeMessage;
		this.includeBindingErrors = includeBindingErrors;
	}

	/**
	 * Set the option for including the exception class name attribute in the error
	 * response.
	 * @param include {@code true} to include the exception class name attribute in the
	 * error response, {@code false} otherwise
	 * @return an updated {@code ErrorAttributeOptions}
	 */
	public ErrorAttributeOptions includeException(boolean include) {
		return new ErrorAttributeOptions(include, this.includeStackTrace, this.includeMessage,
				this.includeBindingErrors);
	}

	/**
	 * Set the option for including the stack trace attribute in the error response.
	 * @param include {@code true} to include the stack trace attribute in the error
	 * response, {@code false} otherwise
	 * @return an updated {@code ErrorAttributeOptions}
	 */
	public ErrorAttributeOptions includeStackTrace(boolean include) {
		return new ErrorAttributeOptions(this.includeException, include, this.includeMessage,
				this.includeBindingErrors);
	}

	/**
	 * Set the option for including the message attribute in the error response.
	 * @param include {@code true} to include the message attribute in the error response,
	 * {@code false} otherwise
	 * @return an updated {@code ErrorAttributeOptions}
	 */
	public ErrorAttributeOptions includeMessage(boolean include) {
		return new ErrorAttributeOptions(this.includeException, this.includeStackTrace, include,
				this.includeBindingErrors);
	}

	/**
	 * Set the option for including the binding errors attribute in the error response.
	 * @param include {@code true} to include the binding errors attribute in the error
	 * response, {@code false} otherwise
	 * @return an updated {@code ErrorAttributeOptions}
	 */
	public ErrorAttributeOptions includeBindingErrors(boolean include) {
		return new ErrorAttributeOptions(this.includeException, this.includeStackTrace, this.includeMessage, include);
	}

	/**
	 * Get the option for including the exception class name attribute in the error
	 * response.
	 * @return {@code true} if the exception class name attribute is included in the error
	 * response, {@code false} otherwise
	 */
	public boolean isIncludeException() {
		return this.includeException;
	}

	/**
	 * Get the option for including the stack trace attribute in the error response.
	 * @return {@code true} if the stack trace attribute is included in the error
	 * response, {@code false} otherwise
	 */
	public boolean isIncludeStackTrace() {
		return this.includeStackTrace;
	}

	/**
	 * Get the option for including the message attribute in the error response.
	 * @return {@code true} if the message attribute is included in the error response,
	 * {@code false} otherwise
	 */
	public boolean isIncludeMessage() {
		return this.includeMessage;
	}

	/**
	 * Get the option for including the binding errors attribute in the error response.
	 * @return {@code true} if the binding errors attribute is included in the error
	 * response, {@code false} otherwise
	 */
	public boolean isIncludeBindingErrors() {
		return this.includeBindingErrors;
	}

}
