/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.convert;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Duration format styles.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public enum DurationStyle {

	/**
	 * Simple formatting, for example '1s'.
	 */
	SIMPLE("^([\\+\\-]?\\d+)([a-zA-Z]{0,2})$") {

		@Override
		public Duration parse(String value, ChronoUnit unit) {
			try {
				Matcher matcher = matcher(value);
				Assert.state(matcher.matches(), "Does not match simple duration pattern");
				long amount = Long.parseLong(matcher.group(1));
				return Duration.of(amount, getUnit(matcher.group(2), unit));
			}
			catch (Exception ex) {
				throw new IllegalStateException(
						"'" + value + "' is not a valid simple duration", ex);
			}
		}

		@Override
		public String print(Duration source, ChronoUnit unit) {
			unit = (unit != null ? unit : ChronoUnit.MILLIS);
			for (Map.Entry<String, ChronoUnit> entry : UNITS.entrySet()) {
				if (entry.getValue() == unit) {
					return source.get(unit) + entry.getKey();
				}
			}
			throw new IllegalStateException("Unsupported chrono unit " + unit);
		}

		private ChronoUnit getUnit(String value, ChronoUnit defaultUnit) {
			if (StringUtils.isEmpty(value)) {
				return (defaultUnit != null ? defaultUnit : ChronoUnit.MILLIS);
			}
			ChronoUnit unit = UNITS.get(value.toLowerCase());
			Assert.state(unit != null, () -> "Unknown unit '" + value + "'");
			return unit;
		}

	},

	/**
	 * ISO-8601 formatting.
	 */
	ISO8601("^[\\+\\-]?P.*$") {

		@Override
		public Duration parse(String value, ChronoUnit unit) {
			try {
				return Duration.parse(value);
			}
			catch (Exception ex) {
				throw new IllegalStateException(
						"'" + value + "' is not a valid ISO-8601 duration", ex);
			}
		}

		@Override
		public String print(Duration source, ChronoUnit unit) {
			return source.toString();
		}

	};

	protected static final Map<String, ChronoUnit> UNITS;

	static {
		Map<String, ChronoUnit> units = new LinkedHashMap<>();
		units.put("ns", ChronoUnit.NANOS);
		units.put("ms", ChronoUnit.MILLIS);
		units.put("s", ChronoUnit.SECONDS);
		units.put("m", ChronoUnit.MINUTES);
		units.put("h", ChronoUnit.HOURS);
		units.put("d", ChronoUnit.DAYS);
		UNITS = Collections.unmodifiableMap(units);
	}

	private final Pattern pattern;

	private DurationStyle(String pattern) {
		this.pattern = Pattern.compile(pattern);
	}

	protected final boolean matches(String value) {
		return this.pattern.matcher(value).matches();
	}

	protected final Matcher matcher(String value) {
		return this.pattern.matcher(value);
	}

	/**
	 * Parse the given value to a duration.
	 * @param value the value to parse
	 * @return a duration
	 */
	public Duration parse(String value) {
		return parse(value, null);
	}

	/**
	 * Parse the given value to a duration.
	 * @param value the value to parse
	 * @param unit the duration unit to use if the value doesn't specify one ({@code null}
	 * will default to ms)
	 * @return a duration
	 */
	public abstract Duration parse(String value, ChronoUnit unit);

	public String print(Duration source) {
		return print(source, null);
	}

	public abstract String print(Duration source, ChronoUnit unit);

	/**
	 * Detect the style then parse the value to return a duration.
	 * @param value the value to parse
	 * @return the parsed duration
	 * @throws IllegalStateException if the value is not a known style or cannot be parsed
	 */
	public static Duration detectAndParse(String value) {
		return detectAndParse(value, null);
	}

	/**
	 * Detect the style then parse the value to return a duration.
	 * @param value the value to parse
	 * @param unit the duration unit to use if the value doesn't specify one ({@code null}
	 * will default to ms)
	 * @return the parsed duration
	 * @throws IllegalStateException if the value is not a known style or cannot be parsed
	 */
	public static Duration detectAndParse(String value, ChronoUnit unit) {
		return detect(value).parse(value, unit);
	}

	/**
	 * Detect the style from the given source value.
	 * @param value the source value
	 * @return the duration style
	 * @throws IllegalStateException if the value is not a known style
	 */
	public static DurationStyle detect(String value) {
		Assert.notNull(value, "Value must not be null");
		for (DurationStyle candidate : values()) {
			if (candidate.matches(value)) {
				return candidate;
			}
		}
		throw new IllegalArgumentException("'" + value + "' is not a valid duration");
	}

}
