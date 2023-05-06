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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link CorrelationIdFormatter}.
 *
 * @author Phillip Webb
 */
class CorrelationIdFormatterTests {

	@Test
	void defaultFormatWhenHasApplicationCorrelationId() {
		Map<String, String> context = new HashMap<>();
		context.put("applicationCorrelationId", "0123456789012345");
		context.put("traceId", "01234567890123456789012345678901");
		context.put("spanId", "0123456789012345");
		String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
		assertThat(formatted).isEqualTo("[0123456789012345-01234567890123456789012345678901-0123456789012345] ");
	}

	@Test
	void defaultFormatWhenDoesNotHaveApplicationCorrelationId() {
		Map<String, String> context = new HashMap<>();
		context.put("traceId", "01234567890123456789012345678901");
		context.put("spanId", "0123456789012345");
		String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
		assertThat(formatted).isEqualTo("[01234567890123456789012345678901-0123456789012345] ");
	}

	@Test
	void defaultFormatWhenHasMissingSpanId() {
		Map<String, String> context = new HashMap<>();
		context.put("traceId", "01234567890123456789012345678901");
		String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
		assertThat(formatted).isEqualTo("[01234567890123456789012345678901-................] ");
	}

	@Test
	void defaultFormatWhenHasShortTraceId() {
		Map<String, String> context = new HashMap<>();
		context.put("traceId", "0123456789012345678901234567");
		context.put("spanId", "0123456789012345");
		String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
		assertThat(formatted).isEqualTo("[0123456789012345678901234567-0123456789012345    ] ");
	}

	@Test
	void defaultFormatWhenHasShortTraceIdAndLongSpanId() {
		Map<String, String> context = new HashMap<>();
		context.put("traceId", "0123456789012345678901234567");
		context.put("spanId", "012345678901234567");
		String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
		assertThat(formatted).isEqualTo("[0123456789012345678901234567-012345678901234567  ] ");
	}

	@Test
	void defaultFormatWhenHasLongSpanId() {
		Map<String, String> context = new HashMap<>();
		context.put("traceId", "01234567890123456789012345678901");
		context.put("spanId", "0123456789012345678901");
		String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
		assertThat(formatted).isEqualTo("[01234567890123456789012345678901-0123456789012345678901] ");
	}

	@Test
	void formatWhenHasCustomPattern() {
		CorrelationIdFormatter formatter = CorrelationIdFormatter.of("-,a,b(4),c(2)");
		Map<String, String> context = new HashMap<>();
		context.put("a", "spring");
		context.put("b", "boot");
		context.put("c", "ok");
		String formatted = formatter.format(context::get);
		assertThat(formatted).isEqualTo("[spring-boot-ok] ");
	}

	@Test
	void formatWhenContextIsNullUsesEmptyContext() {
		String formatted = CorrelationIdFormatter.DEFAULT.format(null);
		assertThat(formatted).isEqualTo("[                                                ] ");
	}

	@Test
	void formatWhenOnlyResolvesOptional() {
		Map<String, String> context = new HashMap<>();
		context.put("applicationCorrelationId", "0123456789012345");
		String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
		assertThat(formatted).isEqualTo("[                                                                ] ");
		context.put("traceId", "0123456789012345678901234567");
		context.put("spanId", "0123456789012345");
		formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
		assertThat(formatted).isEqualTo("[0123456789012345-0123456789012345678901234567-0123456789012345    ] ");
	}

	@Test
	void ofNullStringReturnsDefaultPattern() {
		assertThat(CorrelationIdFormatter.of((String) null)).isSameAs(CorrelationIdFormatter.DEFAULT);
	}

	@Test
	void ofEmptyStringReturnsDefaultPattern() {
		assertThat(CorrelationIdFormatter.of("")).isSameAs(CorrelationIdFormatter.DEFAULT);
	}

	@Test
	void ofNullStringArrayReturnsDefault() {
		assertThat(CorrelationIdFormatter.of((String[]) null)).isSameAs(CorrelationIdFormatter.DEFAULT);
	}

	@Test
	void ofNullCollectionReturnsDefaultPattern() {
		assertThat(CorrelationIdFormatter.of((Collection<String>) null)).isSameAs(CorrelationIdFormatter.DEFAULT);
	}

	@Test
	void ofEmptyCollectionReturnsDefaultPattern() {
		assertThat(CorrelationIdFormatter.of(Collections.emptyList())).isSameAs(CorrelationIdFormatter.DEFAULT);
	}

	@Test
	void ofWhenStyleIsUnknown() {
		assertThatIllegalStateException().isThrownBy(() -> CorrelationIdFormatter.of("bad"))
			.withMessage("Unknown code 'bad'");
	}

	@Test
	void ofWhenNamedItemLengthIsNotNumber() {
		assertThatIllegalStateException().isThrownBy(() -> CorrelationIdFormatter.of("-,name(bad)"))
			.withMessage("Malformed pattern 'name(bad)'")
			.withCauseInstanceOf(NumberFormatException.class);
	}

	@Test
	void ofWhenNamedItemIsMalformed() {
		assertThatIllegalStateException().isThrownBy(() -> CorrelationIdFormatter.of("-,name||123"))
			.withMessage("Malformed pattern 'name||123'");
	}

	@Test
	void ofWhenHasWhitespace() {
		CorrelationIdFormatter formatter = CorrelationIdFormatter.of(" - , a,  b(4),  c(2)");
		Map<String, String> context = new HashMap<>();
		context.put("a", "spring");
		context.put("b", "boot");
		context.put("c", "ok");
		String formatted = formatter.format(context::get);
		assertThat(formatted).isEqualTo("[spring-boot-ok] ");
	}

	@Test
	void toStringReturnsPatternString() {
		assertThat(CorrelationIdFormatter.DEFAULT).hasToString("-,[applicationCorrelationId],traceId(32),spanId(16)");
	}

}
