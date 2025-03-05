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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Selectable} and {@link SimpleSelectable}.
 *
 * @author Phillip Webb
 */
class SelectableTests {

	@Test
	void ofWithStringCreateSelectable() {
		Selectable selectable = Selectable.of("test");
		assertThat(selectable.name()).isEqualTo("test");
		assertThat(selectable.labels()).isSameAs(Labels.NONE);
		assertThat(selectable).hasToString("test");
	}

	@Test
	void ofWithStringMapCreatesSelectable() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("a", "A");
		map.put("b", "B");
		Selectable selectable = Selectable.of("test", map);
		assertThat(selectable.name()).isEqualTo("test");
		assertThat(selectable.labels()).isEqualTo(Labels.fromMap(map));
		assertThat(selectable).hasToString("test [a=A,b=B]");
	}

	@Test
	void ofWithStringLabelsCreatesSelectable() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("a", "A");
		map.put("b", "B");
		Labels labels = Labels.fromMap(map);
		Selectable selectable = Selectable.of("test", labels);
		assertThat(selectable.name()).isEqualTo("test");
		assertThat(selectable.labels()).isSameAs(labels);
		assertThat(selectable).hasToString("test [a=A,b=B]");
	}

	@Test
	void fromMapEntryCreatesSelectable() {
		Selectable selectable = Selectable.fromMapEntry(Map.entry("k", "v"), (value) -> Labels.of(Label.of("x")));
		assertThat(selectable.name()).isEqualTo("k");
		assertThat(selectable.labels().contains("x")).isTrue();
	}

	@Test
	void fromMapEntryWhenLabelProvidersIsNullCreatesSelectable() {
		Selectable selectable = Selectable.fromMapEntry(Map.entry("k", "v"), null);
		assertThat(selectable.name()).isEqualTo("k");
		assertThat(selectable.labels()).isSameAs(Labels.NONE);
	}

	@Test
	void fromCreatesSelectable() {
		StringBuilder source = new StringBuilder("test");
		Selectable selectable = Selectable.from(source, (object) -> object + "!",
				(object) -> Labels.fromMap(Map.of("x", object.toString())));
		assertThat(selectable.name()).isEqualTo("test!");
		assertThat(selectable.labels().contains(Label.of("x", "test"))).isTrue();
	}

	@Test
	void fromWhenNameProviderIsNullCreateSelectable() {
		StringBuilder source = new StringBuilder("test");
		Selectable selectable = Selectable.from(source, null, (object) -> Labels.of(Label.of("x")));
		assertThat(selectable.name()).isEqualTo("test");
		assertThat(selectable.labels().contains("x")).isTrue();
	}

	@Test
	void fromWhenLabelProviderIsNullCreateSelectable() {
		StringBuilder source = new StringBuilder("test");
		Selectable selectable = Selectable.from(source, Object::toString, null);
		assertThat(selectable.name()).isEqualTo("test");
		assertThat(selectable.labels()).isSameAs(Labels.NONE);
	}

	@Test
	void blankReturnsSelectableWithNoNameOrLabels() {
		assertThat(Selectable.blank().name()).isEmpty();
		assertThat(Selectable.blank().labels()).isEmpty();
	}

	@Test
	void blankCanBeUsedToFilterSelectors() {
		Selector<?> s1 = Selector.select();
		Selector<?> s2 = Selector.select().onlyWhenLabeled("foo");
		Selector<?> s3 = Selector.select().onlyWhenNamed("bar");
		Selector<?> s4 = Selector.select();
		assertThat(Stream.of(s1, s2, s3, s4).filter(Selectable.blank())).containsExactly(s1, s4);
	}

	interface MySelector extends Selector<MySelector> {

		void doit();

	}

}
