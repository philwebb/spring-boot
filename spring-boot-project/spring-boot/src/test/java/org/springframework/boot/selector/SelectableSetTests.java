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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.selector.SelectableSet.Entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SelectableSet}, {@link SimpleSelectableSet},
 * {@link AbstractSelectableSet} and {@link SimpleSelectableSetEntry}.
 *
 * @author Phillip Webb
 */
class SelectableSetTests {

	@Test
	void getWhenNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> SelectableSet.empty().getEntry(null))
			.withMessage("'name' must not be null");
	}

	@Test
	void getWhenNameNotSelectableThrowsException() {
		SelectableSet<?, TestElement> set = createSimpleSet();
		assertThatExceptionOfType(NoSuchSelectableNameException.class).isThrownBy(() -> set.get("missing"))
			.withMessage("No selectable with the name 'missing' available");
	}

	@Test
	void getWhenNameNotSelectableDueToFilterThrowsException() {
		SelectableSet<?, TestElement> set = createSimpleSet().havingName("other");
		assertThatExceptionOfType(NoSuchSelectableNameException.class).isThrownBy(() -> set.get("missing"))
			.withMessage("No selectable with the name 'missing' available");
	}

	@Test
	void streamStreamsElemements() {
		SelectableSet<?, TestElement> set = createSimpleSet();
		assertThat(set.stream()).map(TestElement::name).containsExactly("Spring", "Boot", "Spring Boot");
	}

	@Test
	void streamWhenFilteredStreamsElemements() {
		SelectableSet<?, TestElement> set = createSimpleSet().havingName((name) -> name.startsWith("sp"));
		assertThat(set.stream()).map(TestElement::name).containsExactly("Spring", "Spring Boot");
	}

	@Test
	void streamEntriesStreamsEntries() {
		SelectableSet<?, TestElement> set = createSimpleSet();
		assertThat(set.streamEntries()).map(Object::toString)
			.containsExactly("spring -> 'Spring'", "boot -> 'Boot'", "springboot -> 'Spring Boot'");
	}

	@Test
	void streamEntriesWhenFilteredStreamsEntries() {
		SelectableSet<?, TestElement> set = createSimpleSet().havingName((name) -> name.startsWith("sp"));
		assertThat(set.streamEntries()).map(Object::toString)
			.containsExactly("spring -> 'Spring'", "springboot -> 'Spring Boot'");
	}

	@Test
	void entriesStreamsEntries() {
		SelectableSet<?, TestElement> set = createSimpleSet();
		assertThat(set.entries()).map(Object::toString)
			.containsExactly("spring -> 'Spring'", "boot -> 'Boot'", "springboot -> 'Spring Boot'");
	}

	@Test
	void entriesWhenFilteredStreamsEntries() {
		SelectableSet<?, TestElement> set = createSimpleSet().havingName((name) -> name.startsWith("sp"));
		assertThat(set.entries()).map(Object::toString)
			.containsExactly("spring -> 'Spring'", "springboot -> 'Spring Boot'");
	}

	@Test
	void isEmptyWhenEmptyReturnsTrue() {
		assertThat(SelectableSet.empty().isEmpty()).isTrue();
	}

	@Test
	void isEmptyWhenFilteredEmptyReturnsTrue() {
		assertThat(createSimpleSet().havingName("missing").isEmpty()).isTrue();
	}

	@Test
	void isEmptyWhenNotEmptyReturnsFalse() {
		assertThat(createSimpleSet().isEmpty()).isFalse();
	}

	@Test
	void isEmptyWhenFilteredNotEmptyReturnsFalse() {
		assertThat(createSimpleSet().havingName("spring").isEmpty()).isFalse();
	}

	@Test
	void havingName() {
		assertThat(createSimpleSet().havingName("spring").stream()).map(Object::toString).containsExactly("Spring");
	}

	@Test
	void havingNamesWithArray() {
		assertThat(createSimpleSet().havingNames("spring", "boot").stream()).map(Object::toString)
			.containsExactly("Spring", "Boot");
	}

	@Test
	void havingNamesWithCollection() {
		assertThat(createSimpleSet().havingNames(Set.of("spring", "boot")).stream()).map(Object::toString)
			.containsExactly("Spring", "Boot");
	}

	@Test
	void havingLabelWithString() {
		assertThat(createLabeledSet().havingLabel("spring").stream()).map(Object::toString).containsExactly("Spring");
	}

	@Test
	void havingLabelWithStringString() {
		assertThat(createLabeledSet().havingLabel("spring", "SPRING").stream()).map(Object::toString)
			.containsExactly("Spring");
	}

	@Test
	void havingLabelWithStringPredicate() {
		assertThat(createLabeledSet().havingLabel("spring", (name) -> name.startsWith("SP")).stream())
			.map(Object::toString)
			.containsExactly("Spring");
	}

	@Test
	void havingLabel() {
		assertThat(createLabeledSet().havingLabel(Label.of("spring", "SPRING")).stream()).map(Object::toString)
			.containsExactly("Spring");
	}

	@Test
	void havingWithSelector() {
		assertThat(createLabeledSet().having(Selector.select().onlyWhenNamed("spring")).stream()).map(Object::toString)
			.containsExactly("Spring");
	}

	@Test
	void havingWithPredicate() {
		assertThat(createLabeledSet().having((selectable) -> selectable.name().startsWith("sp")).stream())
			.map(Object::toString)
			.containsExactly("Spring", "Spring Boot");

	}

	@Test
	void havingWithBiPredicate() {
		assertThat(createLabeledSet().having((selectable, element) -> element.name().startsWith("Sp")).stream())
			.map(Object::toString)
			.containsExactly("Spring", "Spring Boot");
	}

	@Test
	void havingFilteredWithBiPredicate() {
		assertThat(createLabeledSet().havingName("spring")
			.having((selectable, element) -> element.name().startsWith("Sp"))
			.stream()).map(Object::toString).containsExactly("Spring");
	}

	@Test
	void compoundHavingCalls() {
		assertThat(createLabeledSet().havingLabel("test")
			.havingName((name) -> name.startsWith("s"))
			.havingLabel("spring-boot")).map(Object::toString).containsExactly("Spring Boot");
	}

	@Test
	void fromMap() {
		assertThat(SelectableSet.fromMap(Map.of("foo", "bar")).stream()).map(Object::toString).containsExactly("bar");
	}

	@Test
	void fromMapWithLabelsProvider() {
		assertThat(SelectableSet.fromMap(Map.of("foo", "bar"), (value) -> Labels.of(Label.of("is-a-" + value)))
			.streamEntries()).map(Entry::selectable).map(Object::toString).containsExactly("foo [is-a-bar]");
	}

	@Test
	void fromCollection() {
		Collection<TestName> collection = List.of(new TestName("spring", "framework"), new TestName("spring", "boot"));
		SelectableSet<?, TestName> selectableSet = SelectableSet.fromCollection(collection, TestName::last);
		assertThat(selectableSet.stream()).containsExactlyElementsOf(collection);
		assertThat(selectableSet.get("framework")).isEqualTo(new TestName("spring", "framework"));
	}

	@Test
	void fromWithStream() {
		Stream<TestName> stream = Stream.of(new TestName("spring", "framework"), new TestName("spring", "boot"));
		SelectableSet<?, TestName> selectableSet = SelectableSet.from(stream,
				(name) -> Entry.ofInstance(Selectable.of(name.last()), name));
		assertThat(selectableSet.stream()).containsExactly(new TestName("spring", "framework"),
				new TestName("spring", "boot"));
		assertThat(selectableSet.get("framework")).isEqualTo(new TestName("spring", "framework"));
	}

	@Test
	void fromWithIterable() {
		Iterable<TestName> iterable = List.of(new TestName("spring", "framework"), new TestName("spring", "boot"));
		SelectableSet<?, TestName> selectableSet = SelectableSet.from(iterable,
				(name) -> Entry.ofInstance(Selectable.of(name.last()), name));
		assertThat(selectableSet.stream()).containsExactlyElementsOf(iterable);
		assertThat(selectableSet.get("framework")).isEqualTo(new TestName("spring", "framework"));
	}

	@Test
	void emptyReturnsEmptySet() {
		assertThat(SelectableSet.empty()).hasSize(0);
	}

	@Test
	void toStringLooksSensible() {
		assertThat(createSimpleSet()).hasToString("[spring -> 'Spring', boot -> 'Boot', springboot -> 'Spring Boot']");
	}

	private SelectableSet<?, TestElement> createSimpleSet() {
		Map<String, TestElement> map = new LinkedHashMap<>();
		map.put("spring", new TestElement("Spring"));
		map.put("boot", new TestElement("Boot"));
		map.put("springboot", new TestElement("Spring Boot"));
		return SelectableSet.fromMap(map);
	}

	private SelectableSet<?, TestElement> createLabeledSet() {
		Map<String, TestElement> map = new LinkedHashMap<>();
		map.put("spring", new TestElement("Spring"));
		map.put("boot", new TestElement("Boot"));
		map.put("springboot", new TestElement("Spring Boot"));
		return SelectableSet.fromMap(map, (element) -> {
			String labelName = element.name().toLowerCase().replace(" ", "-");
			String labelValue = element.name().toUpperCase().replace(" ", "_");
			Label label = Label.of(labelName, labelValue);
			return Labels.of(label, Label.of("test"));
		});
	}

	/**
	 * Tests for {@link Entry}.
	 */
	@Nested
	class EntryTests {

		// FIXME

	}

	record TestElement(String name) {

		@Override
		public final String toString() {
			return name();
		}

	}

	record TestName(String first, String last) {

	}

}
