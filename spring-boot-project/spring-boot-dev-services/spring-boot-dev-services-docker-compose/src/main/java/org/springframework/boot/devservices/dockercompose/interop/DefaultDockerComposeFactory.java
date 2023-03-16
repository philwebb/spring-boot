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
import org.springframework.boot.devservices.dockercompose.interop.command.DockerExec;
import org.springframework.boot.devservices.dockercompose.interop.command.DockerExecFactory;

/**
 * Default implementation of {@link DockerComposeFactory}. Returns
 * {@link DefaultDockerCompose} instances.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class DefaultDockerComposeFactory implements DockerComposeFactory {

	@Override
	public DockerCompose create(DockerComposeDevServiceConfigurationProperties configuration, Path configFile) {
		DockerExec dockerExec = DockerExecFactory.create(configFile, configuration.getActiveProfiles());
		return new DefaultDockerCompose(configFile, dockerExec, configuration.getDockerHostname());
	}

}
