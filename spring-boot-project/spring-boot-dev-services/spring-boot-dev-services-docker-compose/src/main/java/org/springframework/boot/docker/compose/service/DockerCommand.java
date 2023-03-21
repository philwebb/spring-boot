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

package org.springframework.boot.docker.compose.service;

/**
 * Commands that can be executed by the {@link DockerCli}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
abstract sealed class DockerCommand<R> {

	/**
	 * The {@code docker context} command.
	 */
	final class Context extends DockerCommand<DockerContextResponse> {

	}

	/**
	 * The {@code docker inspect} command.
	 */
	final class Inspect extends DockerCommand<DockerInspectResponse> {

	}

	/**
	 * The {@code docker compose config} command.
	 */
	final class ComposeConfig extends DockerCommand<DockerComposeConfigResponse> {

	}

	/**
	 * The {@code docker compose ps} command.
	 */
	final class ComposePs extends DockerCommand<DockerComposePsResponse> {

	}

	/**
	 * The {@code docker compose start} command.
	 */
	final class ComposeStart extends DockerCommand<Void> {

	}

	/**
	 * The {@code docker compose stop} command.
	 */
	final class ComposeStop extends DockerCommand<Void> {

	}

	/**
	 * The {@code docker compose up} command.
	 */
	final class ComposeUp extends DockerCommand<Void> {

	}

	/**
	 * The {@code docker compose down} command.
	 */
	final class ComposeDown extends DockerCommand<Void> {

	}

}
