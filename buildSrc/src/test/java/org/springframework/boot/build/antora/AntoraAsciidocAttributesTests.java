/*
 * Copyright 2024 the original author or authors.
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

package org.springframework.boot.build.antora;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AntoraAsciidocAttributes}.
 *
 * @author Phillip Webb
 */
class AntoraAsciidocAttributesTests {

	@Test
	void githubRefWhenReleasedVersionIsTag() {
		AntoraAsciidocAttributes attributes = new AntoraAsciidocAttributes("1.2.3", true, null);
		assertThat(attributes.getAttributes()).containsEntry("github-ref", "v1.2.3");
	}

	@Test
	void githubRefWhenLatestSnapshotVersionIsMainBranch() {
		AntoraAsciidocAttributes attributes = new AntoraAsciidocAttributes("1.2.3-SNAPSHOT", true, null);
		assertThat(attributes.getAttributes()).containsEntry("github-ref", "main");
	}

	@Test
	void githubRefWhenOlderSnapshotVersionIsBranch() {
		AntoraAsciidocAttributes attributes = new AntoraAsciidocAttributes("1.2.3-SNAPSHOT", false, null);
		assertThat(attributes.getAttributes()).containsEntry("github-ref", "1.2.x");
	}

	@Test
	void githubRefWhenOlderSnapshotHotFixVersionIsBranch() {
		AntoraAsciidocAttributes attributes = new AntoraAsciidocAttributes("1.2.3.1-SNAPSHOT", false, null);
		assertThat(attributes.getAttributes()).containsEntry("github-ref", "1.2.1.x");
	}

}
