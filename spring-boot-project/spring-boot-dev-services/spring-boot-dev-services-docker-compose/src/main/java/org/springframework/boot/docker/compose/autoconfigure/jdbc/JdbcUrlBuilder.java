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

package org.springframework.boot.docker.compose.autoconfigure.jdbc;

import org.springframework.boot.docker.compose.service.DockerComposeRunningService;

/**
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public class JdbcUrlBuilder {

	private final String protocol;

	private final int sourcePort;

	public JdbcUrlBuilder(String protocol, int sourcePort) {
		this.protocol = protocol;
		this.sourcePort = sourcePort;
	}

	public String build(DockerComposeRunningService service, String database) {
		return null;
	}

	// FIXME String parameters =
	// source.labels().getOrDefault("org.springframework.boot.jdbc.parameters", "");

}
