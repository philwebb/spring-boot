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

package org.springframework.boot.devservices.dockercompose.zipkin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinServiceConnection;
import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnection;
import org.springframework.boot.devservices.dockercompose.RunningServiceServiceConnectionProvider;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;
import org.springframework.boot.origin.Origin;
import org.springframework.core.log.LogMessage;
import org.springframework.util.ClassUtils;

/**
 * Handles connections to Zipkin.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class ZipkinServiceConnectionProvider implements RunningServiceServiceConnectionProvider {

	private static final Log logger = LogFactory.getLog(ZipkinServiceConnectionProvider.class);

	private final boolean zipkinServiceConnectionPresent;

	ZipkinServiceConnectionProvider(ClassLoader classLoader) {
		this.zipkinServiceConnectionPresent = ClassUtils.isPresent(
				"org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinServiceConnection", classLoader);
	}

	@Override
	public List<? extends ServiceConnection> provideServiceConnection(List<RunningService> services) {
		if (!this.zipkinServiceConnectionPresent) {
			return Collections.emptyList();
		}
		List<DockerComposeZipkinServiceConnection> result = new ArrayList<>();
		for (RunningService service : services) {
			if (!ZipkinService.matches(service)) {
				continue;
			}
			ZipkinService zipkinService = new ZipkinService(service);
			logger.info(LogMessage.format("Zipkin UI is running at %s", getZipkinUi(zipkinService)));
			result.add(new DockerComposeZipkinServiceConnection(zipkinService));
		}
		return result;
	}

	private String getZipkinUi(ZipkinService service) {
		return "http://%s:%d/zipkin/".formatted(service.getHost(), service.getPort());
	}

	private static class DockerComposeZipkinServiceConnection implements ZipkinServiceConnection {

		private final ZipkinService service;

		DockerComposeZipkinServiceConnection(ZipkinService service) {
			this.service = service;
		}

		@Override
		public Origin getOrigin() {
			return this.service.getOrigin();
		}

		@Override
		public String getName() {
			return "docker-compose-zipkin-%s".formatted(this.service.getName());
		}

		@Override
		public String getHost() {
			return this.service.getHost();
		}

		@Override
		public int getPort() {
			return this.service.getPort();
		}

		@Override
		public String getSpanPath() {
			return "/api/v2/spans";
		}

		@Override
		public String toString() {
			return "DockerCompose[host='%s',port=%d,]".formatted(getHost(), getPort());
		}

	}

}
