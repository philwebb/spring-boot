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

package org.springframework.boot.docker.compose.management;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.service.DockerCompose;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ShutdownCommand}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ShutdownCommandTests {

	private DockerCompose dockerCompose = mock(DockerCompose.class);

	private Duration duration = Duration.ofSeconds(10);

	@Test
	void applyToWhenDown() {
		ShutdownCommand.DOWN.applyTo(this.dockerCompose, this.duration);
		verify(this.dockerCompose).down(this.duration);
	}

	@Test
	void applyToWhenStart() {
		ShutdownCommand.STOP.applyTo(this.dockerCompose, this.duration);
		verify(this.dockerCompose).stop(this.duration);
	}

}
