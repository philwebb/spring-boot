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

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Test for {@link Label} and {@link SimpleLabel}.
 *
 * @author Phillip Webb
 */
class LabelTests {

	@Test
	void matchesWithString() {
		assertThat(Label.of("k").matches("k")).isTrue();
		assertThat(Label.of("k", "v").matches("k")).isTrue();
		assertThat(Label.of("k").matches("K")).isFalse();
		assertThat(Label.of("k", "v").matches("K")).isFalse();
		assertThat(Label.of("x", "v").matches("k")).isFalse();
	}

	@Test
	void matchesWithLabel() {
		assertThat(Label.of("k").matches(Label.of("k"))).isTrue();
		assertThat(Label.of("k").matches(new CustomLabelImplementation("k", null))).isTrue();
		assertThat(Label.of("k", "v").matches(Label.of("k", "v"))).isTrue();
		assertThat(Label.of("k", "v").matches(new CustomLabelImplementation("k", "v"))).isTrue();
		assertThat(Label.of("k").matches(Label.of("K"))).isFalse();
		assertThat(Label.of("k").matches(Label.of("k", "v"))).isFalse();
		assertThat(Label.of("k").matches(new CustomLabelImplementation("K", null))).isFalse();
		assertThat(Label.of("k", "v").matches(Label.of("K", "v"))).isFalse();
		assertThat(Label.of("k", "v").matches(Label.of("k", "V"))).isFalse();
		assertThat(Label.of("k", "v").matches(new CustomLabelImplementation("K", "v"))).isFalse();
		assertThat(Label.of("k", "v").matches(new CustomLabelImplementation("k", "V"))).isFalse();
		assertThat(Label.of("k").matches((Label) null)).isFalse();
	}

	@Test
	void matchesWithStringAndString() {
		assertThat(Label.of("k").matches("k", (String) null)).isTrue();
		assertThat(Label.of("k", "v").matches("k", "v")).isTrue();
		assertThat(Label.of("k").matches("K", (String) null)).isFalse();
		assertThat(Label.of("k").matches("k", "v")).isFalse();
		assertThat(Label.of("k", "v").matches("K", "v")).isFalse();
		assertThat(Label.of("k", "v").matches("k", "V")).isFalse();
		assertThat(Label.of("k").matches((String) null, (String) null)).isFalse();
	}

	@Test
	void matchesWithStringAndPredicate() {
		assertThat(Label.of("k", "v").matches("k", "v"::equalsIgnoreCase)).isTrue();
		assertThat(Label.of("k", "V").matches("k", "v"::equalsIgnoreCase)).isTrue();
		assertThat(Label.of("k", "v").matches("k", "x"::equalsIgnoreCase)).isFalse();
		assertThat(Label.of("k", "V").matches("k", "x"::equalsIgnoreCase)).isFalse();
		assertThat(Label.of("k", null).matches("k", "v"::equalsIgnoreCase)).isFalse();
	}

	@Test
	void matchesWithPredicate() {
		assertThat(Label.of("k", "v").matches((label) -> label.key().startsWith("k"))).isTrue();
		assertThat(Label.of("k", "v").matches((label) -> label.key().startsWith("x"))).isFalse();
	}

	@Test
	void ofWithKeyCreatesLabel() {
		Label label = Label.of("production");
		assertThat(label.key()).isEqualTo("production");
		assertThat(label.value()).isNull();
		assertThat(label).hasToString("production");
	}

	@Test
	void ofWithKeyAndValueCreatesLabel() {
		Label label = Label.of("region", "us-east");
		assertThat(label.key()).isEqualTo("region");
		assertThat(label.value()).isEqualTo("us-east");
		assertThat(label).hasToString("region=us-east");
	}

	@Test
	void ofWhenKeyIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Label.of(null, null))
			.withMessage("'key' must not be empty");
	}

	@Test
	void ofWhenKeyIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Label.of("", null))
			.withMessage("'key' must not be empty");
	}

	@Test
	void ofWhenKeyIsBlankThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Label.of("  ", null))
			.withMessage("'key' must not be empty");
	}

	@Test
	void ofWhenValueIsNullCreatesLabel() {
		assertThat(Label.of("k", null)).hasToString("k");
	}

	@Test
	void fromMapEntryCreatesLabel() {
		assertThat(Label.fromMapEntry(Map.entry("k", "v"))).hasToString("k=v");
	}

	@Test
	void fromMapEntryWhenEntryIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Label.fromMapEntry((Map.Entry<String, String>) null))
			.withMessage("'mapEntry' must not be empty");
	}

	@Test
	void toStringIsNotInRecordFormat() {
		assertThat(Label.of("k")).hasToString("k");
		assertThat(Label.of("k", "v")).hasToString("k=v");
	}

}
