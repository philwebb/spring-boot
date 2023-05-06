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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Utility class that can be used to format a correlation identifier so that it can be
 * logged in a consistent way.
 * <p>
 * Formatting is performed based on a pattern of the following following form
 * {@code <style>,<name>[|length],<name>[|length],...}.
 * <p>
 * For example, a pattern of {@code '-,applicationCorrelationId,traceId|32,spanId|16'}
 * would generate correlation IDs of the form
 * {@code [0123456789-01234567890123456789012345678901-0123456789012345]}.
 * <p>
 * The following styles are currently supported by the formatter:
 * <ul>
 * <li>'-' - Formats the correlation ID as a dash separated value contained in square
 * brackets with a trailing slash. Missing named items that have a defined length will be
 * replaced '#' characters.</li>
 * </ul>
 *
 * @author Phillip Webb
 * @since 3.1.0
 * @see #of(String)
 * @see #of(Collection)
 */
public class CorrelationIdFormatter {

	/**
	 * Default {@link CorrelationIdFormatter} used when no pattern is specified.
	 */
	public static final CorrelationIdFormatter DEFAULT = CorrelationIdFormatter
		.of("-,[applicationCorrelationId],traceId(32),spanId(16)");

	public static final Function<String, String> EMPTY_RESOLVER = (name) -> null;

	private final Style style;

	private final List<NamedItem> namedItems;

	private CorrelationIdFormatter(Style style, List<NamedItem> namedItems) {
		this.style = style;
		this.namedItems = namedItems;
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
		resolver = (resolver != null) ? resolver : CorrelationIdFormatter.EMPTY_RESOLVER;
		try {
			Map<NamedItem, String> resolved = new LinkedHashMap<>(this.namedItems.size());
			for (NamedItem namedItem : this.namedItems) {
				resolved.put(namedItem, resolver.apply(namedItem.name()));
			}
			boolean blankOutValues = !resolved.entrySet().stream().anyMatch(this::resolvedNonOptionalValue);
			this.style.formatTo(appendable, resolved, blankOutValues);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private boolean resolvedNonOptionalValue(Entry<NamedItem, String> entry) {
		return !entry.getKey().optional() && StringUtils.hasText(entry.getValue());
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder(this.style.code);
		this.namedItems.forEach((namedItem) -> result.append(",").append(namedItem));
		return result.toString();
	}

	/**
	 * Create a new {@link CorrelationIdFormatter} instance from the given pattern.
	 * @param pattern a comma separated pattern
	 * @return a new {@link CorrelationIdFormatter} instance
	 */
	public static CorrelationIdFormatter of(String pattern) {
		return (!StringUtils.hasText(pattern)) ? DEFAULT : of(List.of(pattern.split(",")));
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
		Iterator<String> iterator = pattern.iterator();
		Style style = Style.forCode(iterator.next().trim());
		List<NamedItem> namedItems = new ArrayList<>(pattern.size() - 1);
		while (iterator.hasNext()) {
			namedItems.add(NamedItem.of(iterator.next().trim()));
		}
		return new CorrelationIdFormatter(style, List.copyOf(namedItems));
	}

	/**
	 * Supported styles.
	 */
	private enum Style {

		DEFAULT("-") {

			@Override
			void formatTo(Appendable appendable, Map<NamedItem, String> resolved, boolean blankOutValues)
					throws IOException {
				int padding = 0;
				boolean first = true;
				appendable.append("[");
				for (Map.Entry<NamedItem, String> entry : resolved.entrySet()) {
					NamedItem namedItem = entry.getKey();
					String value = entry.getValue();
					if (blankOutValues) {
						padding += (!StringUtils.hasLength(value)) ? namedItem.length() : value.length();
						continue;
					}
					if (namedItem.length() > 0) {
						value = (!StringUtils.hasText(value)) ? ".".repeat(namedItem.length) : value;
						padding += namedItem.length() - ((value != null) ? value.length() : 0);
					}
					if (StringUtils.hasText(value)) {
						appendable.append((!first) ? "-" : "");
						appendable.append(value);
						first = false;
					}
				}
				appendable.append((padding > 0) ? " ".repeat(padding) : "");
				appendable.append("] ");
			}

		};

		private final String code;

		Style(String code) {
			this.code = code;
		}

		abstract void formatTo(Appendable appendable, Map<NamedItem, String> resolved, boolean blankOutValues)
				throws IOException;

		static Style forCode(String code) {
			Optional<Style> result = Arrays.stream(values())
				.filter((candidate) -> candidate.code.equals(code))
				.findFirst();
			return result.orElseThrow(() -> new IllegalStateException("Unknown code '%s'".formatted(code)));
		}

	}

	/**
	 * A single named item.
	 */
	private static record NamedItem(String name, boolean optional, int length) {

		private static final Pattern regex = Pattern.compile("^([\\w\\[\\]]+)(?:\\((.*)\\))?$");

		@Override
		public String toString() {
			String result = (!optional()) ? this.name : "[%s]".formatted(this.name);
			return (length() != 0) ? "%s(%s)".formatted(result, length()) : result;

		}

		public static NamedItem of(String pattern) {
			try {
				Matcher matcher = regex.matcher(pattern);
				Assert.state(matcher.matches(), () -> "Malformed pattern '%s".formatted(pattern));
				String name = matcher.group(1);
				String options = matcher.group(2);
				boolean optional = name.contains("[") || name.contains("]");
				name = (!optional) ? name : name.replace("[", "").replace("]", "");
				int length = (options != null) ? Integer.parseInt(options) : 0;
				return new NamedItem(name, optional, length);
			}
			catch (RuntimeException ex) {
				throw new IllegalStateException("Malformed pattern '%s'".formatted(pattern), ex);
			}
		}

	}

}
