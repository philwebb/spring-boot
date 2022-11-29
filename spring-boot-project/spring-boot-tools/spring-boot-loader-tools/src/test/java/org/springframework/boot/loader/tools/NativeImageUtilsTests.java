/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for @{link NativeImageUtils}.
 *
 * @author Moritz Halbritter
 */
class NativeImageUtilsTests {

	@Test
	void shouldCreateExcludeConfigArguments() {
		List<String> arguments = NativeImageUtils
				.createExcludeConfigArguments(List.of("path/to/dependency-1.jar", "dependency-2.jar"));
		assertThat(arguments).containsExactly("--exclude-config", "\\Qdependency-1.jar\\E",
				"^/META-INF/native-image/.*", "--exclude-config", "\\Qdependency-2.jar\\E",
				"^/META-INF/native-image/.*");
	}

}
