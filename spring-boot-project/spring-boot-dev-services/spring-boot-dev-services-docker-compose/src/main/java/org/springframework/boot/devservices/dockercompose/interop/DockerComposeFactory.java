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

package org.springframework.boot.devservices.dockercompose.interop;

import java.nio.file.Path;

import org.springframework.boot.devservices.dockercompose.configuration.DockerComposeDevServiceConfigurationProperties;
import org.springframework.boot.devservices.dockercompose.interop.command.DockerNotInstalledException;
import org.springframework.boot.devservices.dockercompose.interop.command.DockerNotRunningException;

/**
 * Factory for {@link DockerCompose} instances.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public interface DockerComposeFactory {

	/**
	 * Creates a new {@link DockerCompose} for the given config file.
	 * @param configuration the configuration
	 * @param configFile the config file to use
	 * @return instance of {@link DockerCompose}
	 * @throws DockerNotRunningException if docker is not running
	 * @throws DockerNotInstalledException if docker is not installed
	 */
	DockerCompose create(DockerComposeDevServiceConfigurationProperties configuration, Path configFile);

	/**
	 * Creates the default implementation for that interface.
	 * @return default implementation
	 */
	static DockerComposeFactory createDefault() {
		return new DefaultDockerComposeFactory();
	}

}
