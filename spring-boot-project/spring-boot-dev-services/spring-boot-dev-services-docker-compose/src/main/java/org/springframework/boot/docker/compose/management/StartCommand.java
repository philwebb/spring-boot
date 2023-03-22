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

import java.util.function.Function;

import org.springframework.boot.docker.compose.service.DockerCompose;
import org.springframework.boot.docker.compose.service.DockerComposeServices;

/**
 * @author pwebb
 */
public enum StartCommand {

	UP(DockerCompose::up),

	START(DockerCompose::start);

	private final Function<DockerCompose, DockerComposeServices> action;

	StartCommand(Function<DockerCompose, DockerComposeServices> action) {
		this.action = action;
	}

	DockerComposeServices applyTo(DockerCompose dockerCompose) {
		return this.action.apply(dockerCompose);
	}

}
