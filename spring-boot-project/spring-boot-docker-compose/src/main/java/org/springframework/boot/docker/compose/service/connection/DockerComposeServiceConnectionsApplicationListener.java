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

package org.springframework.boot.docker.compose.service.connection;

import java.util.List;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactories;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.management.DockerComposeServicesReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationListener} that listens for an {@link DockerComposeServicesReadyEvent}
 * in order to establish service connections.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerComposeServiceConnectionsApplicationListener
		implements ApplicationListener<DockerComposeServicesReadyEvent> {

	private final ConnectionDetailsFactories factories;

	public DockerComposeServiceConnectionsApplicationListener() {
		this(new ConnectionDetailsFactories());
	}

	DockerComposeServiceConnectionsApplicationListener(ConnectionDetailsFactories factories) {
		this.factories = factories;
	}

	@Override
	public void onApplicationEvent(DockerComposeServicesReadyEvent event) {
		ApplicationContext applicationContext = event.getSource();
		if (applicationContext instanceof BeanDefinitionRegistry registry) {
			registerConnectionDetails(registry, event.getRunningServices());
		}
	}

	private void registerConnectionDetails(BeanDefinitionRegistry registry, List<RunningService> runningServices) {
		for (RunningService runningService : runningServices) {
			ConnectionDetails connectionDetails = getConnectionDetails(runningService);
			if (connectionDetails != null) {
				registerConnectionDetails(registry, runningService, connectionDetails);
			}
		}
	}

	private ConnectionDetails getConnectionDetails(RunningService runningService) {
		DockerComposeConnectionSource source = new DockerComposeConnectionSource(runningService);
		return this.factories.getConnectionDetails(source);
	}

	private void registerConnectionDetails(BeanDefinitionRegistry registry, RunningService runningService,
			ConnectionDetails connectionDetails) {
		String beanName = "dockerCompose" + StringUtils.capitalize(runningService.name()) + "ServiceConnection";
		connectionDetails.register(registry, beanName);
	}

}
