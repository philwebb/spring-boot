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

package org.springframework.boot.docker.compose.core;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.process.DisabledIfProcessUnavailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerCli}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@DisabledIfProcessUnavailable({ "docker", "compose" })
class DockerCliTests {

	@Test
	void runBasicCommand() {
		DockerCli cli = new DockerCli(null, null, Collections.emptySet());
		List<DockerCliContextResponse> context = cli.run(new DockerCliCommand.Context());
		assertThat(context).isNotEmpty();
	}

}
