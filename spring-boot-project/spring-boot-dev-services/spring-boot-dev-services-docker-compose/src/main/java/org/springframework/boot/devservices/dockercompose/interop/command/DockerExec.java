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

package org.springframework.boot.devservices.dockercompose.interop.command;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

/**
 * Executes docker commands.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public interface DockerExec {

	/**
	 * Returns information about docker.
	 * @return information about docker
	 */
	DockerInfo getDockerInfo();

	/**
	 * Returns information about docker compose.
	 * @return information about docker compose
	 */
	DockerComposeInfo getDockerComposeInfo();

	/**
	 * Runs 'docker context'.
	 * @return deserialized 'docker context' output
	 */
	List<DockerContextOutput> runDockerContext();

	/**
	 * Runs 'docker compose ps'.
	 * @return deserialized 'docker compose ps' output
	 */
	List<ComposePsOutput> runComposePs();

	/**
	 * Runs 'docker inspect' on the given {@code ids}.
	 * @param ids ids
	 * @return deserialized 'docker inspect' output
	 */
	List<DockerInspectOutput> runDockerInspect(Collection<String> ids);

	/**
	 * Runs 'docker compose up' and waits until services are healthy.
	 */
	void runComposeUp();

	/**
	 * Runs 'docker compose stop --timeout'.
	 * @param timeout the timeout for the stop operation
	 */
	void runComposeStop(Duration timeout);

	/**
	 * Runs 'docker compose down --timeout'.
	 * @param timeout the timeout for the down operation
	 */
	void runComposeDown(Duration timeout);

	/**
	 * Runs 'docker compose config'.
	 * @return deserialized 'docker compose config' output
	 */
	ComposeConfigOutput runComposeConfig();

}
