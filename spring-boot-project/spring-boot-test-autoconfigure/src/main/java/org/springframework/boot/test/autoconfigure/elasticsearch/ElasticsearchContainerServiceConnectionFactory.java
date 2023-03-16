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

package org.springframework.boot.test.autoconfigure.elasticsearch;

import java.util.List;

import org.testcontainers.elasticsearch.ElasticsearchContainer;

import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchServiceConnection;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchServiceConnection.Node.Protocol;
import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnection;
import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnectionFactory;
import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnectionSource;
import org.springframework.boot.origin.Origin;

/**
 * An adapter from an {@link ElasticsearchContainer} to an
 * {@link ElasticsearchServiceConnection}.
 *
 * @author Andy Wilkinson
 */
class ElasticsearchContainerServiceConnectionFactory
		implements ServiceConnectionFactory<ElasticsearchContainer, ElasticsearchServiceConnection> {

	@Override
	public ServiceConnection createServiceConnection(
			ServiceConnectionSource<ElasticsearchContainer, ElasticsearchServiceConnection> source) {
		return new ElasticsearchServiceConnection() {

			private static final int DEFAULT_PORT = 9200;

			@Override
			public String getName() {
				return source.name();
			}

			@Override
			public Origin getOrigin() {
				return source.origin();
			}

			@Override
			public List<Node> getNodes() {
				return List.of(new Node(source.input().getHost(), source.input().getMappedPort(DEFAULT_PORT),
						Protocol.HTTP, null, null));
			}

			@Override
			public String getUsername() {
				return null;
			}

			@Override
			public String getPassword() {
				return null;
			}

			@Override
			public String getPathPrefix() {
				return null;
			}

		};
	}

}
