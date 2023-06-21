/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.logging;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Utility class that can be used to format a correlation identifier for logging based on
 * <a href=
 * "https://www.w3.org/TR/trace-context/#examples-of-http-traceparent-headers">w3c</a>
 * recommendations.
 * <p>
 * Formatting is performed based on a pattern of the following following form
 * {@code "<name>(length),<name>(length),..."}. For example, a pattern of
 * {@code 'traceId(32),spanId(16)'} would generate correlation IDs of the form
 * {@code [01234567890123456789012345678901-0123456789012345]}.
 * <p>
 * <p>
 * Formatted output is always of a fixed width and with trailing whitespace. Dashes are
 * omitted of none of the named items can be resolved.
 * <p>
 * If no pattern is provided the {@link #DEFAULT} formatter will be used.
 *
 * @author Phillip Webb
 * @since 3.1.0
 * @see #of(String)
 * @see #of(Collection)
 */
public final class CorrelationIdFormatter {

	/**
	 * Default {@link CorrelationIdFormatter}.
	 */
	public static final CorrelationIdFormatter DEFAULT = CorrelationIdFormatter.of("traceId(32),spanId(16)");

	private List<Part> parts;

	private final String blank;

	private CorrelationIdFormatter(List<Part> parts) {
		this.parts = parts;
		this.blank = String.format("[%s] ", parts.stream().map(Part::blank).collect(Collectors.joining(" ")));
	}

	/**
	 * Format a correlation from the values in the given resolver.
	 * @param resolver the resolver used to resolve named values
	 * @return a formatted correlation id
	 */
	public String format(Function<String, String> resolver) {
		StringBuilder result = new StringBuilder();
		formatTo(resolver, result);
		return result.toString();
	}

	/**
	 * Format a correlation from the values in the given resolver and append it to the
	 * given {@link Appendable}.
	 * @param resolver the resolver used to resolve named values
	 * @param appendable the appendable for the formatted correlation id
	 */
	public void formatTo(Function<String, String> resolver, Appendable appendable) {
		Predicate<Part> canResolve = (part) -> StringUtils.hasLength(resolver.apply(part.name()));
		try {
			if (this.parts.stream().anyMatch(canResolve)) {
				appendable.append("[");
				for (Iterator<Part> iterator = this.parts.iterator(); iterator.hasNext();) {
					appendable.append(iterator.next().resolve(resolver));
					appendable.append((!iterator.hasNext()) ? "" : "-");
				}
				appendable.append("] ");
			}
			else {
				appendable.append(this.blank);
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Override
	public String toString() {
		return this.parts.stream().map(Part::toString).collect(Collectors.joining(","));
	}

	/**
	 * Create a new {@link CorrelationIdFormatter} instance from the given pattern.
	 * @param pattern a comma separated pattern
	 * @return a new {@link CorrelationIdFormatter} instance
	 */
	public static CorrelationIdFormatter of(String pattern) {
		try {
			return (!StringUtils.hasText(pattern)) ? DEFAULT : of(List.of(pattern.split(",")));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to parse correlation ID pattern '%s'".formatted(pattern), ex);
		}
	}

	/**
	 * Create a new {@link CorrelationIdFormatter} instance from the given pattern.
	 * @param pattern a pre-separated pattern
	 * @return a new {@link CorrelationIdFormatter} instance
	 */
	public static CorrelationIdFormatter of(String[] pattern) {
		return of((pattern != null) ? Arrays.asList(pattern) : Collections.emptyList());
	}

	/**
	 * Create a new {@link CorrelationIdFormatter} instance from the given pattern.
	 * @param pattern a pre-separated pattern
	 * @return a new {@link CorrelationIdFormatter} instance
	 */
	public static CorrelationIdFormatter of(Collection<String> pattern) {
		if (CollectionUtils.isEmpty(pattern)) {
			return DEFAULT;
		}
		List<Part> parts = pattern.stream().map(Part::of).toList();
		return new CorrelationIdFormatter(parts);
	}

	/**
	 * A part of the correlation id.
	 *
	 * @param name the name of the correlation part
	 * @param length the expected length of the correlation part
	 */
	static final record Part(String name, int length) {

		private static final Pattern pattern = Pattern.compile("^(.+?)\\((\\d+)\\)?$");

		String resolve(Function<String, String> resolver) {
			String resolved = resolver.apply(name());
			if (resolved == null) {
				return blank();
			}
			int padding = length() - resolved.length();
			return resolved + " ".repeat((padding > 0) ? padding : 0);
		}

		String blank() {
			return " ".repeat(this.length);
		}

		@Override
		public String toString() {
			return "%s(%s)".formatted(name(), length());
		}

		static Part of(String part) {
			Matcher matcher = pattern.matcher(part.trim());
			Assert.state(matcher.matches(), () -> "Malformed part '%s'".formatted(part));
			String name = matcher.group(1);
			int length = Integer.parseInt(matcher.group(2));
			return new Part(name, length);
		}

	}

}
