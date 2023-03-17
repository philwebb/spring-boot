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

package org.springframework.boot.autoconfigure.mongo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;

/**
 * A {@link MongoClientSettingsBuilderCustomizer} that applies properties from a
 * {@link MongoProperties} to a {@link MongoClientSettings}.
 *
 * @author Scott Frederick
 * @author Safeer Ansari
 * @author Moritz Halbritter
 * @since 2.4.0
 */
public class MongoPropertiesClientSettingsBuilderCustomizer implements MongoClientSettingsBuilderCustomizer, Ordered {

	private final MongoProperties properties;

	private final MongoConnectionDetails connectionDetails;

	private int order = 0;

	public MongoPropertiesClientSettingsBuilderCustomizer(MongoProperties properties,
			MongoConnectionDetails connectionDetails) {
		this.properties = properties;
		this.connectionDetails = connectionDetails;
	}

	@Override
	public void customize(MongoClientSettings.Builder settingsBuilder) {
		applyUuidRepresentation(settingsBuilder);
		applyHostAndPort(settingsBuilder);
		applyCredentials(settingsBuilder);
		applyReplicaSet(settingsBuilder);
	}

	private void applyUuidRepresentation(MongoClientSettings.Builder settingsBuilder) {
		settingsBuilder.uuidRepresentation(this.properties.getUuidRepresentation());
	}

	private void applyHostAndPort(MongoClientSettings.Builder settings) {
		if (this.connectionDetails == null && this.properties.getUri() != null) {
			settings.applyConnectionString(new ConnectionString(this.properties.getUri()));
			return;
		}
		if (this.connectionDetails == null && this.properties.getHost() == null && this.properties.getPort() == null) {
			settings.applyConnectionString(new ConnectionString(MongoProperties.DEFAULT_URI));
			return;
		}
		String host = (this.connectionDetails != null) ? this.connectionDetails.getHost()
				: getOrDefault(this.properties.getHost(), "localhost");
		int port = (this.connectionDetails != null) ? this.connectionDetails.getPort()
				: getOrDefault(this.properties.getPort(), MongoProperties.DEFAULT_PORT);
		List<ServerAddress> serverAddresses = new ArrayList<>();
		serverAddresses.add(new ServerAddress(host, port));
		List<ServerAddress> additionalServerAddresses = (this.connectionDetails != null)
				? getServerAddresses(this.connectionDetails) : getServerAddresses(this.properties);
		serverAddresses.addAll(additionalServerAddresses);
		settings.applyToClusterSettings((cluster) -> cluster.hosts(serverAddresses));
	}

	private List<ServerAddress> getServerAddresses(MongoConnectionDetails connectionDetails) {
		return connectionDetails.getAdditionalHosts()
			.stream()
			.map((h) -> new ServerAddress(h.host(), h.port()))
			.toList();
	}

	private List<ServerAddress> getServerAddresses(MongoProperties properties) {
		if (CollectionUtils.isEmpty(properties.getAdditionalHosts())) {
			return Collections.emptyList();
		}
		return properties.getAdditionalHosts().stream().map(ServerAddress::new).toList();
	}

	private void applyCredentials(MongoClientSettings.Builder builder) {
		if (this.connectionDetails == null && this.properties.getUri() != null) {
			return;
		}
		String username = (this.connectionDetails != null) ? this.connectionDetails.getUsername()
				: this.properties.getUsername();
		char[] password = (this.connectionDetails != null) ? getPasswordAsCharArray(this.connectionDetails)
				: this.properties.getPassword();
		if (username != null && password != null) {
			String authenticationDatabase = (this.connectionDetails != null)
					? this.connectionDetails.getAuthenticationDatabase() : this.properties.getAuthenticationDatabase();
			String database = (this.connectionDetails != null) ? this.connectionDetails.getDatabase()
					: this.properties.getMongoClientDatabase();
			String databaseToAuthAgainst = (authenticationDatabase != null) ? authenticationDatabase : database;
			builder.credential((MongoCredential.createCredential(username, databaseToAuthAgainst, password)));
		}
	}

	private char[] getPasswordAsCharArray(MongoConnectionDetails connectionDetails) {
		if (connectionDetails.getPassword() == null) {
			return null;
		}
		return connectionDetails.getPassword().toCharArray();
	}

	private void applyReplicaSet(MongoClientSettings.Builder builder) {
		String replicaSetName = (this.connectionDetails != null) ? this.connectionDetails.getReplicaSetName()
				: this.properties.getReplicaSetName();
		if (replicaSetName != null) {
			builder.applyToClusterSettings((cluster) -> cluster.requiredReplicaSetName(replicaSetName));
		}
	}

	private <V> V getOrDefault(V value, V defaultValue) {
		return (value != null) ? value : defaultValue;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Set the order value of this object.
	 * @param order the new order value
	 * @see #getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

}
