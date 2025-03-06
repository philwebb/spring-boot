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
 * {@link SelectorsException} thrown when a label does not exist.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class NoSuchLabelKeyException extends SelectorsException {

	private final String labelKey;

	/**
	 * Create a new {@link NoSuchLabelKeyException} with the given message.
	 * @param message the exception message or {@code null} to use the default message
	 */
	public NoSuchLabelKeyException(String message) {
		this(message, null, null);
	}

	/**
	 * Create a new {@link NoSuchLabelKeyException} with the given message and cause.
	 * @param message the exception message or {@code null} to use the default message
	 * @param cause the cause of the exception or {@code null}
	 */
	public NoSuchLabelKeyException(String message, Throwable cause) {
		this(message, null, cause);
	}

	/**
	 * Create a new {@link NoSuchLabelKeyException} with the given message and cause.
	 * @param message the exception message or {@code null} to use the default message
	 * @param labelKey the label key that was not found
	 * @param cause the cause of the exception or {@code null}
	 */
	public NoSuchLabelKeyException(String message, String labelKey, Throwable cause) {
		super(defaultMessage(message, labelKey), cause);
		this.labelKey = labelKey;
	}

	private static String defaultMessage(String message, String labelKey) {
		return (!StringUtils.hasLength(labelKey)) ? "No label with the given key available"
				: "No label with the key '%s' available".formatted(labelKey);
	}

	/**
	 * Return the label key that could not be found (if known).
	 * @return the label key
	 */
	public String getLabelKey() {
		return this.labelKey;
	}

}
