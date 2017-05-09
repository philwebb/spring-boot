/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.context.properties.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link ConfigurationPropertyName}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Eddú Meléndez
 */
public class ConfigurationPropertyNameTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void ofNameShouldNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name must not be null");
		ConfigurationPropertyName.of(null);
	}

	@Test
	public void ofNameShouldNotStartWithNumber() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("is not valid");
		ConfigurationPropertyName.of("1foo");
	}

	@Test
	public void ofNameShouldNotStartWithDash() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("is not valid");
		ConfigurationPropertyName.of("-foo");
	}

	@Test
	public void ofNameShouldNotStartWithDot() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("is not valid");
		ConfigurationPropertyName.of(".foo");
	}

	@Test
	public void ofNameShouldNotEndWithDot() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("is not valid");
		ConfigurationPropertyName.of("foo.");
	}

	@Test
	public void ofNameShouldNotContainUppercase() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("is not valid");
		ConfigurationPropertyName.of("fOo");
	}

	@Test
	public void ofNameShouldNotContainInvalidChars() throws Exception {
		String invalid = "_@$%*+=':;";
		for (char c : invalid.toCharArray()) {
			try {
				ConfigurationPropertyName.of("foo" + c);
				fail("Did not throw for invalid char " + c);
			}
			catch (IllegalArgumentException ex) {
				assertThat(ex.getMessage()).contains("is not valid");
			}
		}
	}

	@Test
	public void ofNameWhenSimple() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("name");
		assertThat(name.toString()).isEqualTo("name");
		assertThat(name.getNumberOfElements()).isEqualTo(1);
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("name");
		assertThat(name.isIndexed(0)).isFalse();
	}

	@Test
	public void ofNameWhenRunOnAssociative() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo[bar]");
		assertThat(name.toString()).isEqualTo("foo[bar]");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("foo");
		assertThat(name.getElement(1, Form.ORIGINAL)).isEqualTo("bar");
		assertThat(name.isIndexed(0)).isFalse();
		assertThat(name.isIndexed(1)).isTrue();
	}

	@Test
	public void ofNameWhenDotOnAssociative() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo.bar");
		assertThat(name.toString()).isEqualTo("foo.bar");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("foo");
		assertThat(name.getElement(1, Form.ORIGINAL)).isEqualTo("bar");
		assertThat(name.isIndexed(0)).isFalse();
		assertThat(name.isIndexed(1)).isFalse();
	}

	@Test
	public void ofNameWhenDotAndAssociative() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo.[bar]");
		assertThat(name.toString()).isEqualTo("foo[bar]");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("foo");
		assertThat(name.getElement(1, Form.ORIGINAL)).isEqualTo("bar");
		assertThat(name.isIndexed(0)).isFalse();
		assertThat(name.isIndexed(1)).isTrue();
	}

	@Test
	public void ofNameWhenDoubleRunOnAndAssociative() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo[bar]baz");
		assertThat(name.toString()).isEqualTo("foo[bar].baz");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("foo");
		assertThat(name.getElement(1, Form.ORIGINAL)).isEqualTo("bar");
		assertThat(name.getElement(2, Form.ORIGINAL)).isEqualTo("baz");
		assertThat(name.isIndexed(0)).isFalse();
		assertThat(name.isIndexed(1)).isTrue();
		assertThat(name.isIndexed(2)).isFalse();
	}

	@Test
	public void ofNameWhenDoubleDotAndAssociative() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo.[bar].baz");
		assertThat(name.toString()).isEqualTo("foo[bar].baz");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("foo");
		assertThat(name.getElement(1, Form.ORIGINAL)).isEqualTo("bar");
		assertThat(name.getElement(2, Form.ORIGINAL)).isEqualTo("baz");
		assertThat(name.isIndexed(0)).isFalse();
		assertThat(name.isIndexed(1)).isTrue();
		assertThat(name.isIndexed(2)).isFalse();
	}

	@Test
	public void ofNameWhenMissingCloseBracket() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("is not valid");
		ConfigurationPropertyName.of("[bar");
	}

	@Test
	public void ofNameWhenMissingOpenBracket() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("is not valid");
		ConfigurationPropertyName.of("bar]");
	}

	@Test
	public void ofNameWithWhitespaceInName() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("is not valid");
		ConfigurationPropertyName.of("foo. bar");
	}

	@Test
	public void ofNameWithWhitespaceInAssociativeElement() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo[b a r]");
		assertThat(name.toString()).isEqualTo("foo[b a r]");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("foo");
		assertThat(name.getElement(1, Form.ORIGINAL)).isEqualTo("b a r");
		assertThat(name.isIndexed(0)).isFalse();
		assertThat(name.isIndexed(1)).isTrue();
	}

	@Test
	public void ofNameWithUppercaseInAssociativeElement() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo[BAR]");
		assertThat(name.toString()).isEqualTo("foo[BAR]");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("foo");
		assertThat(name.getElement(1, Form.ORIGINAL)).isEqualTo("BAR");
		assertThat(name.isIndexed(0)).isFalse();
		assertThat(name.isIndexed(1)).isTrue();
	}

	@Test
	public void equalsAndHashCode() throws Exception {
		ConfigurationPropertyName name1 = ConfigurationPropertyName.of("foo[bar]");
		ConfigurationPropertyName name2 = ConfigurationPropertyName.of("foo[bar]");
		ConfigurationPropertyName name3 = ConfigurationPropertyName.of("foo.bar");
		ConfigurationPropertyName name4 = ConfigurationPropertyName.of("f-o-o.b-a-r");
		ConfigurationPropertyName name5 = ConfigurationPropertyName.of("foo[BAR]");
		ConfigurationPropertyName name6 = ConfigurationPropertyName.of("oof[bar]");
		ConfigurationPropertyName name7 = ConfigurationPropertyName.of("foo.bar");
		ConfigurationPropertyName name8 = ConfigurationPropertyName.EMPTY;
		ConfigurationPropertyName name9 = ConfigurationPropertyName.of("foo");
		ConfigurationPropertyName name10 = ConfigurationPropertyName.of("fo");
		ConfigurationPropertyName name11 = ConfigurationPropertyName.parse("foo.BaR",
				'.');
		assertThat(name1.hashCode()).isEqualTo(name2.hashCode());
		assertThat(name1.hashCode()).isEqualTo(name2.hashCode());
		assertThat(name1.hashCode()).isEqualTo(name3.hashCode());
		assertThat(name1.hashCode()).isEqualTo(name4.hashCode());
		assertThat((Object) name1).isEqualTo(name1);
		assertThat((Object) name1).isEqualTo(name2);
		assertThat((Object) name1).isEqualTo(name3);
		assertThat((Object) name1).isEqualTo(name4);
		assertThat((Object) name11).isEqualTo(name3);
		assertThat((Object) name3).isEqualTo(name11);
		assertThat((Object) name1).isNotEqualTo(name5);
		assertThat((Object) name1).isNotEqualTo(name6);
		assertThat((Object) name7).isNotEqualTo(name8);
		assertThat((Object) name9).isNotEqualTo(name10);
		assertThat((Object) name10).isNotEqualTo(name9);
	}

	@Test
	public void getElementShouldNotIncludeAngleBrackets() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("[foo]");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("foo");
		assertThat(name.getElement(0, Form.UNIFORM)).isEqualTo("foo");
	}

	@Test
	public void elementElementInUniformFormShouldNotIncludeDashes() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("f-o-o");
		assertThat(name.getElement(0, Form.ORIGINAL)).isEqualTo("f-o-o");
		assertThat(name.getElement(0, Form.UNIFORM)).isEqualTo("foo");
	}

	@Test
	public void getElementInOriginalFormShouldReturnElement() throws Exception {
		assertThat(getElements("foo.bar", Form.ORIGINAL)).containsExactly("foo", "bar");
		assertThat(getElements("foo[0]", Form.ORIGINAL)).containsExactly("foo", "0");
		assertThat(getElements("foo.[0]", Form.ORIGINAL)).containsExactly("foo", "0");
		assertThat(getElements("foo[baz]", Form.ORIGINAL)).containsExactly("foo", "baz");
		assertThat(getElements("foo.baz", Form.ORIGINAL)).containsExactly("foo", "baz");
		assertThat(getElements("foo[baz].bar", Form.ORIGINAL)).containsExactly("foo",
				"baz", "bar");
		assertThat(getElements("foo.baz.bar", Form.ORIGINAL)).containsExactly("foo",
				"baz", "bar");
		assertThat(getElements("foo.baz-bar", Form.ORIGINAL)).containsExactly("foo",
				"baz-bar");
	}

	@Test
	public void getElementInUniformFormShouldReturnElement() throws Exception {
		assertThat(getElements("foo.bar", Form.UNIFORM)).containsExactly("foo", "bar");
		assertThat(getElements("foo[0]", Form.UNIFORM)).containsExactly("foo", "0");
		assertThat(getElements("foo.[0]", Form.UNIFORM)).containsExactly("foo", "0");
		assertThat(getElements("foo[baz]", Form.UNIFORM)).containsExactly("foo", "baz");
		assertThat(getElements("foo.baz", Form.UNIFORM)).containsExactly("foo", "baz");
		assertThat(getElements("foo[baz].bar", Form.UNIFORM)).containsExactly("foo",
				"baz", "bar");
		assertThat(getElements("foo.baz.bar", Form.UNIFORM)).containsExactly("foo", "baz",
				"bar");
		assertThat(getElements("foo.baz-bar", Form.UNIFORM)).containsExactly("foo",
				"bazbar");
	}

	private List<CharSequence> getElements(String name, Form form) {
		ConfigurationPropertyName propertyName = ConfigurationPropertyName.of(name);
		List<CharSequence> result = new ArrayList<>(propertyName.getNumberOfElements());
		for (int i = 0; i < propertyName.getNumberOfElements(); i++) {
			result.add(propertyName.getElement(i, form));
		}
		return result;
	}

	@Test
	public void isIndexedWhenIndexedShouldReturnTrue() throws Exception {
		assertThat(ConfigurationPropertyName.of("foo[0]").isLastElementIndexed())
				.isTrue();
	}

	@Test
	public void isIndexedWhenNotIndexedShouldReturnFalse() throws Exception {
		assertThat(ConfigurationPropertyName.of("foo.bar").isLastElementIndexed())
				.isFalse();
		assertThat(ConfigurationPropertyName.of("foo[0].bar").isLastElementIndexed())
				.isFalse();
	}

	@Test
	public void isAncestorOfWhenSameShouldReturnFalse() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		assertThat(name.isAncestorOf(name)).isFalse();
	}

	@Test
	public void isAncestorOfWhenParentShouldReturnTrue() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertyName child = ConfigurationPropertyName.of("foo.bar");
		assertThat(name.isAncestorOf(child)).isTrue();
		assertThat(child.isAncestorOf(name)).isFalse();
	}

	@Test
	public void isAncestorOfWhenGrandparentShouldReturnTrue() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertyName grandchild = ConfigurationPropertyName
				.of("foo.bar.baz");
		assertThat(name.isAncestorOf(grandchild)).isTrue();
		assertThat(grandchild.isAncestorOf(name)).isFalse();
	}

	@Test
	public void isAncestorOfWhenRootShouldReturnTrue() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("");
		ConfigurationPropertyName grandchild = ConfigurationPropertyName
				.of("foo.bar.baz");
		assertThat(name.isAncestorOf(grandchild)).isTrue();
		assertThat(grandchild.isAncestorOf(name)).isFalse();
	}

	@Test
	public void isParentOfWhenSameShouldReturnFalse() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		assertThat(name.isParentOf(name)).isFalse();
	}

	@Test
	public void isParentOfWhenParentShouldReturnTrue() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertyName child = ConfigurationPropertyName.of("foo.bar");
		assertThat(name.isParentOf(child)).isTrue();
		assertThat(child.isParentOf(name)).isFalse();
	}

	@Test
	public void isParentOfWhenGrandparentShouldReturnFalse() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		ConfigurationPropertyName grandchild = ConfigurationPropertyName
				.of("foo.bar.baz");
		assertThat(name.isParentOf(grandchild)).isFalse();
		assertThat(grandchild.isParentOf(name)).isFalse();
	}

	@Test
	public void isParentOfWhenRootReturnTrue() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("");
		ConfigurationPropertyName child = ConfigurationPropertyName.of("foo");
		ConfigurationPropertyName grandchild = ConfigurationPropertyName.of("foo.bar");
		assertThat(name.isParentOf(child)).isTrue();
		assertThat(name.isParentOf(grandchild)).isFalse();
		assertThat(child.isAncestorOf(name)).isFalse();
	}

	@Test
	public void appendWhenNotIndexedShouldAppendWithDot() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		assertThat(name.append("bar").toString()).isEqualTo("foo.bar");
	}

	@Test
	public void appendWhenIndexedShouldAppendWithBrackets() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo")
				.append("[bar]");
		assertThat(name.isLastElementIndexed()).isTrue();
		assertThat(name.toString()).isEqualTo("foo[bar]");
	}

	@Test
	public void appendWhenElementNameIsNotValidShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Element value '1bar' is not valid");
		ConfigurationPropertyName.of("foo").append("1bar");
	}

	@Test
	public void appendWhenElementNameMultiDotShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Element value 'bar.baz' must be a single item");
		ConfigurationPropertyName.of("foo").append("bar.baz");
	}

	@Test
	public void appendWhenElementNameIsNullShouldReturnName() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
		assertThat((Object) name.append((String) null)).isSameAs(name);
	}

	@Test
	public void compareShouldSortNames() throws Exception {
		List<ConfigurationPropertyName> names = new ArrayList<>();
		names.add(ConfigurationPropertyName.of("foo[10]"));
		names.add(ConfigurationPropertyName.of("foo.bard"));
		names.add(ConfigurationPropertyName.of("foo[2]"));
		names.add(ConfigurationPropertyName.of("foo.bar"));
		names.add(ConfigurationPropertyName.of("foo.baz"));
		names.add(ConfigurationPropertyName.of("foo"));
		Collections.sort(names);
		assertThat(names.stream().map(ConfigurationPropertyName::toString)
				.collect(Collectors.toList())).containsExactly("foo", "foo[2]", "foo[10]",
						"foo.bar", "foo.bard", "foo.baz");
	}

	@Test
	public void ofNameCanBeEmpty() throws Exception {
		ConfigurationPropertyName name = ConfigurationPropertyName.of("");
		assertThat(name.toString()).isEqualTo("");
		assertThat(name.append("foo").toString()).isEqualTo("foo");
	}

	@Test
	public void isValidWhenValidShouldReturnTrue() throws Exception {
		assertThat(ConfigurationPropertyName.isValid("")).isTrue();
		assertThat(ConfigurationPropertyName.isValid("foo")).isTrue();
		assertThat(ConfigurationPropertyName.isValid("foo.bar")).isTrue();
		assertThat(ConfigurationPropertyName.isValid("foo[0]")).isTrue();
		assertThat(ConfigurationPropertyName.isValid("foo[0].baz")).isTrue();
		assertThat(ConfigurationPropertyName.isValid("foo.b1")).isTrue();
		assertThat(ConfigurationPropertyName.isValid("foo.b-a-r")).isTrue();
		assertThat(ConfigurationPropertyName.isValid("foo[FooBar].baz")).isTrue();
	}

	@Test
	public void isValidWhenNotValidShouldReturnFalse() throws Exception {
		assertThat(ConfigurationPropertyName.isValid(null)).isFalse();
		assertThat(ConfigurationPropertyName.isValid("1foo")).isFalse();
		assertThat(ConfigurationPropertyName.isValid("FooBar")).isFalse();
		assertThat(ConfigurationPropertyName.isValid("foo!bar")).isFalse();
	}

}
