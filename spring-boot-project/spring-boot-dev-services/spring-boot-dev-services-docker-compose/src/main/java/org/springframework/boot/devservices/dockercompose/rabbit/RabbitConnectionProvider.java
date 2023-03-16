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

package org.springframework.boot.devservices.dockercompose.rabbit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.autoconfigure.amqp.RabbitServiceConnection;
import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnection;
import org.springframework.boot.devservices.dockercompose.RunningServiceServiceConnectionProvider;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;
import org.springframework.boot.origin.Origin;
import org.springframework.util.ClassUtils;

/**
 * Handles connections to a RabbitMQ service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class RabbitConnectionProvider implements RunningServiceServiceConnectionProvider {

	private final boolean serviceConnectionPresent;

	RabbitConnectionProvider(ClassLoader classLoader) {
		this.serviceConnectionPresent = ClassUtils
			.isPresent("org.springframework.boot.autoconfigure.amqp.RabbitServiceConnection", classLoader);
	}

	@Override
	public List<? extends ServiceConnection> provideServiceConnection(List<RunningService> services) {
		if (!this.serviceConnectionPresent) {
			return Collections.emptyList();
		}
		List<RabbitServiceConnection> result = new ArrayList<>();
		for (RunningService service : services) {
			if (!RabbitService.matches(service)) {
				continue;
			}
			RabbitService rabbitService = new RabbitService(service);
			result.add(new DockerComposeRabbitServiceConnection(rabbitService));
		}
		return result;
	}

	private static class DockerComposeRabbitServiceConnection implements RabbitServiceConnection {

		private final RabbitService service;

		DockerComposeRabbitServiceConnection(RabbitService service) {
			this.service = service;
		}

		@Override
		public String getUsername() {
			return this.service.getUsername();
		}

		@Override
		public String getPassword() {
			return this.service.getPassword();
		}

		@Override
		public String getVirtualHost() {
			return "/";
		}

		@Override
		public List<Address> getAddresses() {
			return List.of(new Address(this.service.getHost(), this.service.getPort()));
		}

		@Override
		public String getName() {
			return "docker-compose-rabbit-%s".formatted(this.service.getName());
		}

		@Override
		public Origin getOrigin() {
			return this.service.getOrigin();
		}

		@Override
		public String toString() {
			return "DockerCompose[host='%s',port=%d,username='%s']".formatted(this.service.getHost(),
					this.service.getPort(), getUsername());
		}

	}

}
