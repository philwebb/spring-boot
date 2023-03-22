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

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplicationShutdownHandlers;
import org.springframework.boot.docker.compose.management.DockerComposeProperties.Stop;
import org.springframework.boot.docker.compose.service.DockerCompose;
import org.springframework.boot.docker.compose.service.DockerComposeFile;
import org.springframework.boot.docker.compose.service.DockerComposeServices;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.log.LogMessage;

/**
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerComposeLifecycleManager {

	private static final Log logger = LogFactory.getLog(DockerComposeLifecycleManager.class);

	private final DockerComposeProperties properties;

	private final File workingDirectory;

	DockerComposeLifecycleManager(DockerComposeProperties properties) {
		this(properties, new File("."));
	}

	DockerComposeLifecycleManager(DockerComposeProperties properties, File workingDirectory) {
		this.properties = properties;
		this.workingDirectory = workingDirectory;
	}

	void prepare(ConfigurableApplicationContext applicationContext,
			SpringApplicationShutdownHandlers shutdownHandlers) {
		// FIXME if we're in a test exit we might want to exit
		DockerComposeFile composeFile = (this.properties.getFile() != null)
				? DockerComposeFile.of(this.properties.getFile()) : DockerComposeFile.find(this.workingDirectory);
		logger.info(LogMessage.format("Found docker compose file '%s'", composeFile));
		DockerCompose dockerCompose = DockerCompose.get(composeFile, this.properties.getHostname(),
				this.properties.getProfiles().getActive());
		DockerComposeServices services = dockerCompose.listServices();
		if (services.isEmpty()) {
			logger.warn(LogMessage.format("No servies defined in docker compose file '%s'", composeFile));
			return;
		}
		if (this.properties.getLifecycleManagement().shouldStart() && !services.hasRunningService()) {
			services = this.properties.getStart().getCommand().applyTo(dockerCompose);
			if (this.properties.getLifecycleManagement().shouldStop()) {
				Stop stop = this.properties.getStop();
				shutdownHandlers.add(() -> stop.getCommand().applyTo(dockerCompose, stop.getTimeout()));
			}
		}
		// FIXME wait until ready
		// FIXME fire an event to trigger bean registration
		// applicationContext.publishEvent(null);
	}

}
