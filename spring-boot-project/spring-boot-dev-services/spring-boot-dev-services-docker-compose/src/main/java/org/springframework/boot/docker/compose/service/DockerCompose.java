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

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public interface DockerCompose {

	void up();

	void down(Duration timeout);

	void start();

	void stop(Duration timeout);

	List<DefinedService> listServices();

	static DockerCompose get(DockerComposeFile file, String hostname, Set<String> activeProfiles) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @return
	 */
	boolean isEmpty(); // hasDefinitions

	/**
	 * @return
	 */
	boolean hasRunningService(); // FIXME isStarted

}
