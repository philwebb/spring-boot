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

import org.junit.Test;

/**
 * Tests for {@link ConfigurationPropertyNameBuilder}.
 *
 * @author Phillip Webb
 */
public class ConfigurationPropertyNameBuilderTests {

	@Test
	public void createWhenPatternIsNullShouldThrowException() throws Exception {

	}

	@Test
	public void createWhenElementBuilderIsNullShouldThrowException() throws Exception {

	}

	@Test
	public void buildShouldCreateName() throws Exception {

	}

	@Test
	public void buildShouldValidateUsingPattern() {
	}

	@Test
	public void buildWhenHasNoElementsShouldThrowException() throws Exception {

	}

	@Test
	public void buildShouldUseElementBuilder() throws Exception {

	}

	@Test
	public void fromNameShouldSetElements() throws Exception {

	}

	@Test
	public void fromNameWhenHasExistingShouldSetNewElements() throws Exception {

	}

	@Test
	public void fromStringShouldSetElements() throws Exception {

	}

	@Test
	public void fromStringWhenHasExistingShouldSetNewElements() throws Exception {

	}

	@Test
	public void appendShouldAppendElement() throws Exception {

	}
	//
	// assertThat(this.tokenizer.tokenize("foo")).isEqualTo(elements("foo"));
	// assertThat(this.tokenizer.tokenize("[foo]")).isEqualTo(elements("[foo]"));
	// assertThat(this.tokenizer.tokenize("foo.bar")).isEqualTo(elements("foo", "bar"));
	// assertThat(this.tokenizer.tokenize("foo[foo.bar]"))
	// .isEqualTo(elements("foo", "[foo.bar]"));
	// assertThat(this.tokenizer.tokenize("foo.[bar].baz"))
	// .isEqualTo(elements("foo", "[bar]", "baz"));
	// assertThat(this.tokenizer.tokenize("[foo")).isEqualTo(elements("[foo]"));

}
