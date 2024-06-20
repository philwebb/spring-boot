/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.logging.structured;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;

/**
 * Collection of {@link StructuredLoggingFormat structured logging formats}. Loads formats
 * by using {@link SpringFactoriesLoader} with the {@link StructuredLoggingFormat} key.
 *
 * @author Moritz Halbritter
 * @since 3.4.0
 */
public final class StructuredLoggingFormats {

	private final Map<String, StructuredLoggingFormat> formats;

	/**
	 * Creates a new {@link StructuredLoggingFormats} with the given {@code formats}.
	 * @param formats the formats to use
	 */
	private StructuredLoggingFormats(Iterable<? extends StructuredLoggingFormat> formats) {
		Assert.notNull(formats, "Formats must not be null");
		Map<String, StructuredLoggingFormat> groupedById = new HashMap<>();
		for (StructuredLoggingFormat format : formats) {
			String id = format.getId().toLowerCase(Locale.ENGLISH);
			StructuredLoggingFormat existing = groupedById.putIfAbsent(id, format);
			if (existing != null) {
				throw new IllegalStateException("Duplicate format: id '%s' is used by both '%s' and '%s'".formatted(id,
						format.getClass().getName(), existing.getClass().getName()));
			}
		}
		this.formats = Map.copyOf(groupedById);
	}

	/**
	 * Returns the supported formats.
	 * @return the supported formats
	 */
	public Set<String> getFormats() {
		return this.formats.keySet();
	}

	/**
	 * Returns the requested {@link StructuredLoggingFormat}. Returns {@code null} if the
	 * format isn't known.
	 * @param format the requested format
	 * @return the requested format or{@code null} if the format isn't known.
	 */
	public StructuredLoggingFormat get(String format) {
		Assert.notNull(format, "Format must not be null");
		return this.formats.get(format.toLowerCase(Locale.ENGLISH));
	}

	/**
	 * Loads structured logging formats from {@code spring.factories} using the
	 * {@link StructuredLoggingFormat} key.
	 * @param loader the loader to use
	 * @return the loaded structured logging formats
	 */
	public static StructuredLoggingFormats loadFromSpringFactories(SpringFactoriesLoader loader) {
		Assert.notNull(loader, "Loader must not be null");
		return new StructuredLoggingFormats(loader.load(StructuredLoggingFormat.class));
	}

	/**
	 * Loads structured logging formats from {@code spring.factories} using the
	 * {@link StructuredLoggingFormat} key.
	 * @return the loaded structured logging formats
	 */
	public static StructuredLoggingFormats loadFromSpringFactories() {
		return loadFromSpringFactories(
				SpringFactoriesLoader.forDefaultResourceLocation(StructuredLoggingFormats.class.getClassLoader()));
	}

}
