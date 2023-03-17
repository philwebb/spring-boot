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

package org.springframework.boot.test.autoconfigure.mongo;

import java.net.URI;

import org.testcontainers.containers.MongoDBContainer;

import org.springframework.boot.autoconfigure.mongo.MongoConnectionDetails;
import org.springframework.boot.test.autoconfigure.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.test.autoconfigure.service.connection.ServiceConnectedContainer;

/**
 * {@link ContainerConnectionDetailsFactory} for {@link MongoConnection @MongoConnection}
 * annotated {@link MongoDBContainer} fields.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class MongoContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<MongoConnection, MongoConnectionDetails, MongoDBContainer> {

	@Override
	protected MongoConnectionDetails getContainerConnectionDetails(
			ServiceConnectedContainer<MongoConnection, MongoConnectionDetails, MongoDBContainer> source) {
		return new MongoContainerConnectionDetails(source);
	}

	/**
	 * {@link MongoConnectionDetails} backed by a {@link ServiceConnectedContainer}.
	 */
	private static class MongoContainerConnectionDetails extends ContainerConnectionDetails
			implements MongoConnectionDetails {

		private final URI uri;

		protected MongoContainerConnectionDetails(
				ServiceConnectedContainer<MongoConnection, MongoConnectionDetails, MongoDBContainer> source) {
			super(source);
			this.uri = URI.create(source.getContainer().getReplicaSetUrl());
		}

		@Override
		public String getHost() {
			return this.uri.getHost();
		}

		@Override
		public int getPort() {
			return this.uri.getPort();
		}

		@Override
		public String getDatabase() {
			return this.uri.getPath();
		}

	}

}
