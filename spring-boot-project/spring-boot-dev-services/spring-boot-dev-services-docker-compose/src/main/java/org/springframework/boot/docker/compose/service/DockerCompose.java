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
import java.util.Set;

/**
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public interface DockerCompose {

	DockerComposeServices listServices();

	DockerComposeServices up();

	DockerComposeServices start();

	void down(Duration timeout);

	DockerComposeServices stop(Duration timeout);

	/**
	 * @param dockerComposeFile
	 * @param hostname
	 * @param active
	 * @return
	 */
	static DockerCompose get(DockerComposeFile dockerComposeFile, String hostname, Set<String> activeProfiles) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
