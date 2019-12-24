/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.jarmode.tools;

import java.io.File;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link DockerfileCommand}.
 *
 * @author Phillip Webb
 */
class DockerfileCommandTests {

	@Mock
	private Context context;

	private DockerfileCommand command;

	private TestPrintStream out;

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
		this.command = new DockerfileCommand(this.context);
		this.out = new TestPrintStream(this);
	}

	@Test
	void runOutputsDockerfile() {
		this.command.run(this.out, Collections.emptyMap(), Collections.emptyList());
		assertThat(this.out).hasSameContentAsResource("dockerfile-default.txt");
	}

	@Test
	void runWhenHasRelativeDirOutputsDockerfile() {
		given(this.context.getRelativeJarDir()).willReturn("target");
		this.command.run(this.out, Collections.emptyMap(), Collections.emptyList());
		assertThat(this.out).hasSameContentAsResource("dockerfile-with-relative-dir.txt");
	}

	@Test
	void runWhenHasFolderOptionOutputsDockerfile() {
		this.command.run(this.out, Collections.singletonMap(DockerfileCommand.FOLDER_OPTION, "custom"),
				Collections.emptyList());
		assertThat(this.out).hasSameContentAsResource("dockerfile-with-custom-dir.txt");
	}

	@Test
	void runWhenHasNoWildcardsOptionOutputsDockerfile() {
		given(this.context.getJarFile()).willReturn(new File("test.jar"));
		this.command.run(this.out, Collections.singletonMap(DockerfileCommand.NO_WILDCARDS_OPTION, null),
				Collections.emptyList());
		assertThat(this.out).hasSameContentAsResource("dockerfile-with-no-wildcards.txt");
	}

}
