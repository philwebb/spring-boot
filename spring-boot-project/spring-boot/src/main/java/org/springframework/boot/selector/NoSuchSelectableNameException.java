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

package org.springframework.boot.selector;

import org.springframework.util.StringUtils;

/**
 * {@link SelectorsException} thrown when a request for a selectable with a specific name
 * cannot be satisfied.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class NoSuchSelectableNameException extends SelectorsException {

	private final String selectableName;

	/**
	 * Create a new {@link NoSuchSelectableNameException} with the given message.
	 * @param message the exception message or {@code null} to use the default message
	 */
	public NoSuchSelectableNameException(String message) {
		this(message, null);
	}

	/**
	 * Create a new {@link NoSuchSelectableNameException} with the given message and
	 * cause.
	 * @param message the exception message or {@code null} to use the default message
	 * @param cause the cause of the exception or {@code null}
	 */
	public NoSuchSelectableNameException(String message, Throwable cause) {
		this(message, null, cause);
	}

	/**
	 * Create a new {@link NoSuchSelectableNameException} with the given message and
	 * cause.
	 * @param message the exception message or {@code null} to use the default message
	 * @param selectableName the name that was not used to search for the selectable
	 * @param cause the cause of the exception or {@code null}
	 */
	public NoSuchSelectableNameException(String message, String selectableName, Throwable cause) {
		super((message != null) ? message : defaultMessage(selectableName), cause);
		this.selectableName = selectableName;

	}

	private static String defaultMessage(String selectableName) {
		return (!StringUtils.hasLength(selectableName)) ? "No selectable with the given name available"
				: "No selectable with the name '%s' available".formatted(selectableName);
	}

	/**
	 * Return the selectqable name that could not be found (if known).
	 * @return the selectqable name
	 */
	public String getSelectableName() {
		return this.selectableName;
	}

}
