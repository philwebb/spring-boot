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

package org.springframework.boot.devservices.xdockercompose;

import java.util.List;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactories;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;
import org.springframework.context.ApplicationListener;

/**
 * Uses docker compose to provide dev services. The docker compose file (usually named
 * {@code compose.yaml}) can be configured using the
 * {@code spring.dev-services.docker-compose.config-file} property. If this property isn't
 * set, it uses the following files:
 * <ul>
 * <li>{@code compose.yaml}</li>
 * <li>{@code compose.yml}</li>
 * <li>{@code docker-compose.yaml}</li>
 * <li>{@code docker-compose.yml}</li>
 * </ul>
 * If no such file is found, it backs off. If docker compose is not already running, it
 * will be started. This can be disabled by setting
 * {@code spring.dev-services.docker-compose.auto-start} to {@code false}. If docker
 * compose has been started by this provider, docker compose will be stopped afterwards.
 * <p>
 * It uses {@link RunningServiceServiceConnectionProvider extractors} to delegate the work
 * of translating running docker compose services to {@link ConnectionDetails service
 * connections}. Those providers can be registered in {@code spring.factories} under the
 * {@link RunningServiceServiceConnectionProvider} key.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class DockerComposeListener2 implements ApplicationListener<ApplicationPreparedEvent> {

	@Override
	public void onApplicationEvent(ApplicationPreparedEvent event) {
		ConnectionDetailsFactories factories = null;
		List<RunningService> sources = null;
		for (RunningService source : sources) {
			ConnectionDetails connectionDetails = factories.getConnectionDetailsFactory(source)
				.getConnectionDetails(source);
			if (connectionDetails != null) {
				registerBean(connectionDetails);
			}
		}

	}

	private void registerBean(ConnectionDetails connectionDetails) {
	}

}
