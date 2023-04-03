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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplicationShutdownHandlers;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.docker.compose.core.DockerCompose;
import org.springframework.boot.docker.compose.core.DockerComposeFile;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.management.DockerComposeProperties.Shutdown;
import org.springframework.boot.docker.compose.management.DockerComposeProperties.Startup;
import org.springframework.boot.docker.compose.readiness.ServiceReadinessChecks;
import org.springframework.context.ApplicationContext;
import org.springframework.core.log.LogMessage;

/**
 * Manages the lifecycle for docker compose services.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @see DockerComposeListener
 */
class DockerComposeLifecycleManager {

	private static final Log logger = LogFactory.getLog(DockerComposeLifecycleManager.class);

	private static final Object IGNORE_LABEL = "org.springframework.boot.ignore";

	private final File workingDirectory;

	private final ApplicationContext applicationContext;

	private final ClassLoader classLoader;

	private final SpringApplicationShutdownHandlers shutdownHandlers;

	private final DockerComposeProperties properties;

	private final DockerComposeSkipCheck skipCheck;

	private final ServiceReadinessChecks serviceReadinessChecks;

	DockerComposeLifecycleManager(ApplicationContext applicationContext, Binder binder,
			SpringApplicationShutdownHandlers shutdownHandlers, DockerComposeProperties properties) {
		this(null, applicationContext, binder, shutdownHandlers, properties, new DockerComposeSkipCheck(), null);
	}

	DockerComposeLifecycleManager(File workingDirectory, ApplicationContext applicationContext, Binder binder,
			SpringApplicationShutdownHandlers shutdownHandlers, DockerComposeProperties properties,
			DockerComposeSkipCheck skipCheck, ServiceReadinessChecks serviceReadinessChecks) {
		this.workingDirectory = workingDirectory;
		this.applicationContext = applicationContext;
		this.classLoader = applicationContext.getClassLoader();
		this.shutdownHandlers = shutdownHandlers;
		this.properties = properties;
		this.skipCheck = skipCheck;
		this.serviceReadinessChecks = (serviceReadinessChecks != null) ? serviceReadinessChecks
				: new ServiceReadinessChecks(this.classLoader, applicationContext.getEnvironment(), binder);
	}

	void startup() {
		if (!this.properties.isEnabled()) {
			logger.trace("Docker compose support not enabled");
			return;
		}
		if (this.skipCheck.shouldSkip(this.classLoader, logger, this.properties.getSkip())) {
			logger.trace("Docker compose support skipped");
			return;
		}
		DockerComposeFile composeFile = getComposeFile();
		Set<String> activeProfiles = this.properties.getProfiles().getActive();
		DockerCompose dockerCompose = getDockerCompose(composeFile, activeProfiles);
		if (!dockerCompose.hasDefinedServices()) {
			logger.warn(LogMessage.format("No services defined in docker compose file '%s' with active profiles %s",
					composeFile, activeProfiles));
			return;
		}
		LifecycleManagement lifecycleManagement = this.properties.getLifecycleManagement();
		Startup startup = this.properties.getStartup();
		Shutdown shutdown = this.properties.getShutdown();
		if (lifecycleManagement.shouldStartup() && !dockerCompose.hasRunningServices()) {
			startup.getCommand().applyTo(dockerCompose);
			if (lifecycleManagement.shouldShutdown()) {
				this.shutdownHandlers.add(() -> shutdown.getCommand().applyTo(dockerCompose, shutdown.getTimeout()));
			}
		}
		List<RunningService> runningServices = new ArrayList<>(dockerCompose.getRunningServices());
		runningServices.removeIf(this::isIgnored);
		this.serviceReadinessChecks.waitUntilReady(runningServices);
		DockerComposeServicesReadyEvent event = new DockerComposeServicesReadyEvent(this.applicationContext,
				runningServices);
		this.applicationContext.publishEvent(event);
	}

	protected DockerComposeFile getComposeFile() {
		DockerComposeFile composeFile = (this.properties.getFile() != null)
				? DockerComposeFile.of(this.properties.getFile()) : DockerComposeFile.find(this.workingDirectory);
		logger.info(LogMessage.format("Found docker compose file '%s'", composeFile));
		return composeFile;
	}

	protected DockerCompose getDockerCompose(DockerComposeFile composeFile, Set<String> activeProfiles) {
		return DockerCompose.get(composeFile, this.properties.getHost(), activeProfiles);
	}

	private boolean isIgnored(RunningService service) {
		return service.labels().containsKey(IGNORE_LABEL);
	}

}
