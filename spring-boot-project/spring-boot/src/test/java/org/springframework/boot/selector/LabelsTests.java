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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link Labels} and {@link SimpleLabels}.
 *
 * @author Phillip Webb
 */
class LabelsTests {

	private static final String NULL_STRING = null;

	private static final Label l1 = Label.of("k1", "v1");

	private static final Label l2 = Label.of("k2", "v2");

	private static final Label missing = Label.of("missing", "missing");

	@Test
	void noneIsEmpty() {
		assertThat(Labels.NONE).isEmpty();
	}

	@LabelsTest
	void getGetsLabel(Labels labels) {
		assertThat(labels.get("k1")).satisfies(matching(l1));
		assertThat(labels.get("k2")).satisfies(matching(l2));
	}

	@LabelsTest
	void getWhenNotPresentThrowsException(Labels labels) {
		assertThatExceptionOfType(NoSuchLabelKeyException.class).isThrownBy(() -> labels.get("missing"))
			.withMessage("No label with the key 'missing' available");
	}

	@LabelsTest
	void containsWithLabel(Labels labels) {
		assertThat(labels.contains(l1)).isTrue();
		assertThat(labels.contains(l2)).isTrue();
		assertThat(labels.contains(missing)).isFalse();
		assertThat(labels.contains((Label) null)).isFalse();
	}

	@LabelsTest
	void containsWithString(Labels labels) {
		assertThat(labels.contains("k1")).isTrue();
		assertThat(labels.contains("k2")).isTrue();
		assertThat(labels.contains("missing")).isFalse();
		assertThat(labels.contains(NULL_STRING)).isFalse();
	}

	@LabelsTest
	void containsWithStringString(Labels labels) {
		assertThat(labels.contains("k1", "v1")).isTrue();
		assertThat(labels.contains("k2", "v2")).isTrue();
		assertThat(labels.contains("k1", "V1")).isFalse();
		assertThat(labels.contains("K1", "v1")).isFalse();
		assertThat(labels.contains("K1", NULL_STRING)).isFalse();
		assertThat(labels.contains(NULL_STRING, "v1")).isFalse();
		assertThat(labels.contains(NULL_STRING, NULL_STRING)).isFalse();
	}

	@LabelsTest
	void containsWithStringPredicate(Labels labels) {
		assertThat(labels.contains("k1", "V1"::equalsIgnoreCase)).isTrue();
		assertThat(labels.contains("k2", "V1"::equalsIgnoreCase)).isFalse();
	}

	@LabelsTest
	void containsWithPredicate(Labels labels) {
		assertThat(labels.contains((label) -> label.matches("k1"))).isTrue();
		assertThat(labels.contains((label) -> label.matches("missing"))).isFalse();
	}

	@LabelsTest
	void streamStreamsLabels(Labels labels) {
		assertThat(labels.stream().map(Label::key)).containsExactly("k1", "k2");
	}

	@Test
	void ofWithArrayCreatesLabels() {
		assertThat(Labels.of(l1, l2)).hasToString("[k1=v1,k2=v2]");
	}

	@Test
	void ofWithCollectionCreatesLabels() {
		assertThat(Labels.of(List.of(l1, l2))).hasToString("[k1=v1,k2=v2]");
	}

	@Test
	void ofWithStreamCreatesLabels() {
		assertThat(Labels.of(Stream.of(l1, l2))).hasToString("[k1=v1,k2=v2]");
	}

	@Test
	void fromWithMapCreatesLabels() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("k1", "v1");
		map.put("k2", "v2");
		map.put("kn", null);
		assertThat(Labels.fromMap(map)).hasToString("[k1=v1,k2=v2,kn]");
	}

	@Test
	void fromMapWhenMapIsNullReturnsNone() {
		assertThat(Labels.fromMap(null)).isSameAs(Labels.NONE);
	}

	@Test
	void fromMapWhenMapIsEmptyReturnsNone() {
		assertThat(Labels.fromMap(Collections.emptyMap())).isSameAs(Labels.NONE);
	}

	@Test
	void toStringIsNotInRecordFormat() {
		Labels labels = Labels.of(l1, l2, Label.of("kn"));
		assertThat(labels).hasToString("[k1=v1,k2=v2,kn]");
	}

	@Test
	void labelsCannotContainDuplicateKeys() {
		assertThatExceptionOfType(DuplicateLabelKeyException.class)
			.isThrownBy(() -> Labels.of(Label.of("a"), Label.of("a", "thing")))
			.withMessage("Duplicate labels detected: 'a=thing', 'a'");
	}

	@Test
	void labelsCannotContainLabelsWithoutKey() {
		CustomLabelImplementation label = new CustomLabelImplementation("   ", "test");
		assertThatIllegalStateException().isThrownBy(() -> Labels.of(label)).withMessage("'label' must have a key");
	}

	private Consumer<Label> matching(Label expected) {
		return (actual) -> assertThat(actual.matches(expected)).isTrue();
	}

	static Stream<Arguments> labels() {
		Labels labels = Labels.of(l1, l2);
		Labels customImplementation = Labels.of(labels.stream().map(CustomLabelImplementation::new));
		return Stream.of(Arguments.of(labels), Arguments.of(customImplementation));
	}

	@ParameterizedTest
	@MethodSource("labels")
	@Retention(RetentionPolicy.RUNTIME)
	@interface LabelsTest {

	}

}
