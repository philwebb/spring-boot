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

package org.springframework.boot.devservices.dockercompose.elasticsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchServiceConnection;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchServiceConnection.Node.Protocol;
import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnection;
import org.springframework.boot.devservices.dockercompose.RunningServiceServiceConnectionProvider;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;
import org.springframework.boot.origin.Origin;
import org.springframework.util.ClassUtils;

/**
 * Handles connections to an Elasticsearch service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class ElasticsearchConnectionProvider implements RunningServiceServiceConnectionProvider {

	private final boolean serviceConnectionPresent;

	ElasticsearchConnectionProvider(ClassLoader classLoader) {
		this.serviceConnectionPresent = ClassUtils.isPresent(
				"org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchServiceConnection", classLoader);
	}

	@Override
	public List<? extends ServiceConnection> provideServiceConnection(List<RunningService> services) {
		if (!this.serviceConnectionPresent) {
			return Collections.emptyList();
		}
		List<ElasticsearchServiceConnection> result = new ArrayList<>();
		for (RunningService service : services) {
			if (!ElasticsearchService.matches(service)) {
				continue;
			}
			ElasticsearchService elasticsearchService = new ElasticsearchService(service);
			result.add(new DockerComposeElasticsearchServiceConnection(elasticsearchService));
		}
		return result;
	}

	private static class DockerComposeElasticsearchServiceConnection implements ElasticsearchServiceConnection {

		private final ElasticsearchService service;

		DockerComposeElasticsearchServiceConnection(ElasticsearchService service) {
			this.service = service;
		}

		@Override
		public List<Node> getNodes() {
			return List.of(new Node(this.service.getHost(), this.service.getPort(), Protocol.HTTP, getUsername(),
					getPassword()));
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
		public String getPathPrefix() {
			return null;
		}

		@Override
		public String getName() {
			return "docker-compose-elasticsearch-%s".formatted(this.service.getName());
		}

		@Override
		public Origin getOrigin() {
			return this.service.getOrigin();
		}

		@Override
		public String toString() {
			return "DockerCompose[nodes=%s,username=%s]".formatted(getNodes(), getUsername());
		}

	}

}
