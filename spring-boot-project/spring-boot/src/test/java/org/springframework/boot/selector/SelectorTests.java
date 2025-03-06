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

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Selector} and {@link SimpleSelector}.
 *
 * @author Phillip Webb
 */
class SelectorTests {

	private Selectable usaWestCoastServer = Selectable.of("usa-west-coast-server",
			Labels.of(Label.of("region", "us-west")));

	private Selectable usaEastCoastServer = Selectable.of("usa-east-coast-server",
			Labels.of(Label.of("region", "us-east"), Label.of("production", null)));

	private Selectable usaCentralServer = Selectable.of("usa-central-server",
			Labels.of(Label.of("region", "us-central"), Label.of("production", "yes")));

	private Selectable euFranceServer = Selectable.of("eu-france-server",
			Labels.of(Label.of("region", "eu-france"), Label.of("production", "us-ok")));

	@Test
	void selectWhenNoRestrictionsReturnsTrue() {
		Selector<?> selector = Selector.select();
		assertThat(selector.selects(this.usaWestCoastServer)).isTrue();
	}

	@Test
	void onylWhenNamedWithStringSelectsByName() {
		Selector<?> selector = Selector.select().onlyWhenNamed("usa-west-coast-server");
		assertThat(selector.selects(this.usaWestCoastServer)).isTrue();
		assertThat(selector.selects(this.usaEastCoastServer)).isFalse();
	}

	@Test
	void onylWhenNamedWithPredicateSelectsByName() {
		Selector<?> selector = Selector.select().onlyWhenNamed((name) -> name.startsWith("usa-"));
		assertThat(selector.selects(this.usaWestCoastServer)).isTrue();
		assertThat(selector.selects(this.usaEastCoastServer)).isTrue();
		assertThat(selector.selects(this.usaCentralServer)).isTrue();
		assertThat(selector.selects(this.euFranceServer)).isFalse();
	}

	@Test
	void onylWhenNamedWithStringArraySelectsByNames() {
		Selector<?> selector = Selector.select().onlyWhenNamed("usa-west-coast-server", "usa-east-coast-server");
		assertThat(selector.selects(this.usaWestCoastServer)).isTrue();
		assertThat(selector.selects(this.usaEastCoastServer)).isTrue();
		assertThat(selector.selects(this.usaCentralServer)).isFalse();
	}

	@Test
	void onylWhenNameWithCollectionSelectsByNames() {
		Selector<?> selector = Selector.select()
			.onlyWhenNamed(Set.of("usa-west-coast-server", "usa-east-coast-server"));
		assertThat(selector.selects(this.usaWestCoastServer)).isTrue();
		assertThat(selector.selects(this.usaEastCoastServer)).isTrue();
		assertThat(selector.selects(this.usaCentralServer)).isFalse();
	}

	@Test
	void onlyWhenLabeledWithStringSelectsByLabel() {
		Selector<?> selector = Selector.select().onlyWhenLabeled("production");
		assertThat(selector.selects(this.usaWestCoastServer)).isFalse();
		assertThat(selector.selects(this.usaEastCoastServer)).isTrue();
		assertThat(selector.selects(this.usaCentralServer)).isTrue();
	}

	@Test
	void onlyWhenLabeledWithLabelSelectsByLabel() {
		Selector<?> selector = Selector.select().onlyWhenLabeled(Label.of("region", "us-west"));
		assertThat(selector.selects(this.usaWestCoastServer)).isTrue();
		assertThat(selector.selects(this.usaEastCoastServer)).isFalse();
	}

	@Test
	void onlyWhenLabeledWithStringStringSelectsByLabel() {
		Selector<?> selector = Selector.select().onlyWhenLabeled("region", "us-west");
		assertThat(selector.selects(this.usaWestCoastServer)).isTrue();
		assertThat(selector.selects(this.usaEastCoastServer)).isFalse();
	}

	@Test
	void onlyWhenLabeledWithStringPredicateSelectsByLabel() {
		Selector<?> selector = Selector.select()
			.onlyWhenLabeled("region", (labelValue) -> labelValue.startsWith("us-"));
		assertThat(selector.selects(this.usaWestCoastServer)).isTrue();
		assertThat(selector.selects(this.usaEastCoastServer)).isTrue();
		assertThat(selector.selects(this.euFranceServer)).isFalse();
	}

	@Test
	void onlyWhenLabeledWithPredicateSelectsByLabel() {
		Selector<?> selector = Selector.select()
			.onlyWhenLabeled((label) -> label.matches("region", (labelValue) -> labelValue.startsWith("us-")));
		assertThat(selector.selects(this.usaWestCoastServer)).isTrue();
		assertThat(selector.selects(this.usaEastCoastServer)).isTrue();
		assertThat(selector.selects(this.euFranceServer)).isFalse();
	}

	@Test
	void onlyWhenWithSelectorSelectsBySelector() {
		Selector<?> selector = Selector.select().onlyWhen(Selector.select().onlyWhenLabeled("region", "us-west"));
		assertThat(selector.selects(this.usaWestCoastServer)).isTrue();
		assertThat(selector.selects(this.usaEastCoastServer)).isFalse();
	}

	@Test
	void onlyWhenWithPredicateSelectsBySelector() {
		Selector<?> selector = Selector.select()
			.onlyWhen((selectable) -> selectable.labels().contains("region", "us-west"));
		assertThat(selector.selects(this.usaWestCoastServer)).isTrue();
		assertThat(selector.selects(this.usaEastCoastServer)).isFalse();
	}

	@Test
	void compoundSelections() {
		Selector<?> selector = Selector.select()
			.onlyWhenLabeled("region", (labelName) -> labelName.startsWith("us-"))
			.onlyWhenLabeled("production");
		assertThat(selector.selects(this.usaWestCoastServer)).isFalse();
		assertThat(selector.selects(this.usaEastCoastServer)).isTrue();
		assertThat(selector.selects(this.usaCentralServer)).isTrue();
		assertThat(selector.selects(this.euFranceServer)).isFalse();
	}

	@Test
	void bankSelection() {
		Selector<?> selector = Selector.select();
		assertThat(selector.selects(Selectable.blank())).isTrue();
		assertThat(selector.onlyWhenNamed("").selects(Selectable.blank())).isTrue();
		assertThat(selector.onlyWhenNamed("anon").selects(Selectable.blank())).isFalse();
	}

	// FIXME streamSelected selectingBlank selecting

}
