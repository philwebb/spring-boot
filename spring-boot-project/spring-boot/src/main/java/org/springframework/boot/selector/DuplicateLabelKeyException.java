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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link SelectorsException} thrown to prevent duplicate {@link Label#key() label keys}
 * from being added.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class DuplicateLabelKeyException extends SelectorsException {

	private final Collection<Label> duplicates;

	/**
	 * Create a new {@link DuplicateSelectableNameException}.
	 * @param message the exception message or {@code null} to use the default message
	 */
	public DuplicateLabelKeyException(String message) {
		this(message, null);
	}

	/**
	 * Create a new {@link DuplicateLabelKeyException}.
	 * @param message the exception message or {@code null} to use the default message
	 * @param cause the cause of the exception or {@code null}
	 */
	public DuplicateLabelKeyException(String message, Throwable cause) {
		this(message, null, cause);
	}

	/**
	 * Create a new {@link DuplicateLabelKeyException}.
	 * @param message the exception message or {@code null} to use the default message
	 * @param duplicates the duplicates (if known)
	 * @param cause the cause of the exception or {@code null}
	 */
	public DuplicateLabelKeyException(String message, Collection<? extends Label> duplicates, Throwable cause) {
		super((!StringUtils.hasText(message)) ? defaultMessage(duplicates) : message, cause);
		this.duplicates = (duplicates != null) ? List.copyOf(duplicates) : Collections.emptyList();
	}

	private static String defaultMessage(Collection<? extends Label> duplicates) {
		String message = "Duplicate labels detected";
		if (!CollectionUtils.isEmpty(duplicates)) {
			message += duplicates.stream().map(SimpleLabel::toString).collect(Collectors.joining("', '", ": '", "'"));
		}
		return message;
	}

	/**
	 * Return the duplicates that caused the exception or an empty collection if the
	 * duplicates are not known.
	 * @return a collection of the duplicates
	 */
	public Collection<Label> getDuplicates() {
		return this.duplicates;
	}

}
