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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DurationStyle}.
 *
 * @author Phillip Webb
 */
public class DurationStyleTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void detectAndParseWhenValueIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Value must not be null");
		DurationStyle.detectAndParse(null);
	}

	@Test
	public void detectAndParseWhenIso8601ShouldReturnDuration() {
		assertThat(DurationStyle.detectAndParse("PT20.345S"))
				.isEqualTo(Duration.parse("PT20.345S"));
		assertThat(DurationStyle.detectAndParse("PT15M"))
				.isEqualTo(Duration.parse("PT15M"));
		assertThat(DurationStyle.detectAndParse("+PT15M"))
				.isEqualTo(Duration.parse("PT15M"));
		assertThat(DurationStyle.detectAndParse("PT10H"))
				.isEqualTo(Duration.parse("PT10H"));
		assertThat(DurationStyle.detectAndParse("P2D")).isEqualTo(Duration.parse("P2D"));
		assertThat(DurationStyle.detectAndParse("P2DT3H4M"))
				.isEqualTo(Duration.parse("P2DT3H4M"));
		assertThat(DurationStyle.detectAndParse("-PT6H3M"))
				.isEqualTo(Duration.parse("-PT6H3M"));
		assertThat(DurationStyle.detectAndParse("-PT-6H+3M"))
				.isEqualTo(Duration.parse("-PT-6H+3M"));
	}

	@Test
	public void detectAndParseWhenSimpleNanosShouldReturnDuration() {
		assertThat(DurationStyle.detectAndParse("10ns")).isEqualTo(Duration.ofNanos(10));
		assertThat(DurationStyle.detectAndParse("10NS")).isEqualTo(Duration.ofNanos(10));
		assertThat(DurationStyle.detectAndParse("+10ns")).isEqualTo(Duration.ofNanos(10));
		assertThat(DurationStyle.detectAndParse("-10ns"))
				.isEqualTo(Duration.ofNanos(-10));
	}

	@Test
	public void detectAndParseWhenSimpleMillisShouldReturnDuration() {
		assertThat(DurationStyle.detectAndParse("10ms")).isEqualTo(Duration.ofMillis(10));
		assertThat(DurationStyle.detectAndParse("10MS")).isEqualTo(Duration.ofMillis(10));
		assertThat(DurationStyle.detectAndParse("+10ms"))
				.isEqualTo(Duration.ofMillis(10));
		assertThat(DurationStyle.detectAndParse("-10ms"))
				.isEqualTo(Duration.ofMillis(-10));
	}

	@Test
	public void detectAndParseWhenSimpleSecondsShouldReturnDuration() {
		assertThat(DurationStyle.detectAndParse("10s")).isEqualTo(Duration.ofSeconds(10));
		assertThat(DurationStyle.detectAndParse("10S")).isEqualTo(Duration.ofSeconds(10));
		assertThat(DurationStyle.detectAndParse("+10s"))
				.isEqualTo(Duration.ofSeconds(10));
		assertThat(DurationStyle.detectAndParse("-10s"))
				.isEqualTo(Duration.ofSeconds(-10));
	}

	@Test
	public void detectAndParseWhenSimpleMinutesShouldReturnDuration() {
		assertThat(DurationStyle.detectAndParse("10m")).isEqualTo(Duration.ofMinutes(10));
		assertThat(DurationStyle.detectAndParse("10M")).isEqualTo(Duration.ofMinutes(10));
		assertThat(DurationStyle.detectAndParse("+10m"))
				.isEqualTo(Duration.ofMinutes(10));
		assertThat(DurationStyle.detectAndParse("-10m"))
				.isEqualTo(Duration.ofMinutes(-10));
	}

	@Test
	public void detectAndParseWhenSimpleHoursShouldReturnDuration() {
		assertThat(DurationStyle.detectAndParse("10h")).isEqualTo(Duration.ofHours(10));
		assertThat(DurationStyle.detectAndParse("10H")).isEqualTo(Duration.ofHours(10));
		assertThat(DurationStyle.detectAndParse("+10h")).isEqualTo(Duration.ofHours(10));
		assertThat(DurationStyle.detectAndParse("-10h")).isEqualTo(Duration.ofHours(-10));
	}

	@Test
	public void detectAndParseWhenSimpleDaysShouldReturnDuration() {
		assertThat(DurationStyle.detectAndParse("10d")).isEqualTo(Duration.ofDays(10));
		assertThat(DurationStyle.detectAndParse("10D")).isEqualTo(Duration.ofDays(10));
		assertThat(DurationStyle.detectAndParse("+10d")).isEqualTo(Duration.ofDays(10));
		assertThat(DurationStyle.detectAndParse("-10d")).isEqualTo(Duration.ofDays(-10));
	}

	@Test
	public void detectAndParseWhenSimpleWithoutSuffixShouldReturnDuration() {
		assertThat(DurationStyle.detectAndParse("10")).isEqualTo(Duration.ofMillis(10));
		assertThat(DurationStyle.detectAndParse("+10")).isEqualTo(Duration.ofMillis(10));
		assertThat(DurationStyle.detectAndParse("-10")).isEqualTo(Duration.ofMillis(-10));
	}

	@Test
	public void detectAndParseWhenSimpleWithoutSuffixButWithChronoUnitShouldReturnDuration() {
		assertThat(DurationStyle.detectAndParse("10", ChronoUnit.SECONDS))
				.isEqualTo(Duration.ofSeconds(10));
		assertThat(DurationStyle.detectAndParse("+10", ChronoUnit.SECONDS))
				.isEqualTo(Duration.ofSeconds(10));
		assertThat(DurationStyle.detectAndParse("-10", ChronoUnit.SECONDS))
				.isEqualTo(Duration.ofSeconds(-10));
	}

	@Test
	public void detectAndParseWhenBadFormatShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("'10foo' is not a valid duration");
		DurationStyle.detectAndParse("10foo");
	}

}
