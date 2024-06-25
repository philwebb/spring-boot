/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.logging.structured;

/**
 * Metadata about the application.
 *
 * @author Moritz Halbritter
 * @since 3.4.0
 */
public class ApplicationMetadata {

	private final Long pid;

	private final String name;

	private final String version;

	private final String environment;

	private final String nodeName;

	public ApplicationMetadata(Long pid, String name, String version, String environment, String nodeName) {
		this.pid = pid;
		this.name = name;
		this.version = version;
		this.environment = environment;
		this.nodeName = nodeName;
	}

	public Long getPid() {
		return this.pid;
	}

	public String getName() {
		return this.name;
	}

	public String getVersion() {
		return this.version;
	}

	public String getEnvironment() {
		return this.environment;
	}

	public String getNodeName() {
		return this.nodeName;
	}

}
