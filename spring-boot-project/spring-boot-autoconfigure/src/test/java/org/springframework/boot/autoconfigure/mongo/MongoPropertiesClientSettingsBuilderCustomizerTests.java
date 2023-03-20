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

import java.util.Arrays;
import java.util.List;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.bson.UuidRepresentation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoPropertiesClientSettingsBuilderCustomizer}.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 */
class MongoPropertiesClientSettingsBuilderCustomizerTests {

	private final TestMongoConnectionDetails connectionDetails = new TestMongoConnectionDetails();

	private MongoProperties properties = new MongoProperties();

	@BeforeEach
	void setUp() {
		this.properties = new MongoProperties();
	}

	@Test
	void portCanBeCustomized() {
		this.properties.setPort(12345);
		this.connectionDetails.setConnectionString("mongodb://localhost:23456");
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(1);
		assertThat(allAddresses.get(0).getPort()).isEqualTo(23456);
	}

	@Test
	void hostCanBeCustomized() {
		this.properties.setHost("mongo.example.com");
		this.connectionDetails.setConnectionString("mongodb://some-other-mongo.example.com");
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(1);
		assertThat(allAddresses.get(0).getHost()).isEqualTo("some-other-mongo.example.com");
	}

	@Test
	void additionalsHostCanBeAdded() {
		this.properties.setHost("mongo.example.com");
		this.properties.setAdditionalHosts(Arrays.asList("mongo.example.com:33", "mongo.example2.com"));
		this.connectionDetails
			.setConnectionString("mongodb://mongo1.example.com:1337,mongo2.example.com:1234,mongo3.example.com:2345");
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(3);
		assertServerAddress(allAddresses.get(0), "mongo1.example.com", 1337);
		assertServerAddress(allAddresses.get(1), "mongo2.example.com", 1234);
		assertServerAddress(allAddresses.get(2), "mongo3.example.com", 2345);
	}

	@Test
	void credentialsCanBeCustomized() {
		this.properties.setUsername("user");
		this.properties.setPassword("secret".toCharArray());
		this.connectionDetails.setConnectionString("mongodb://some-user:some-password@localhost");
		MongoClientSettings settings = customizeSettings();
		assertMongoCredential(settings.getCredential(), "some-user", "some-password");
	}

	@Test
	void replicaSetCanBeCustomized() {
		this.properties.setReplicaSetName("test");
		this.connectionDetails.setConnectionString("mongodb://localhost/?replicaSet=some-replica-set");
		MongoClientSettings settings = customizeSettings();
		assertThat(settings.getClusterSettings().getRequiredReplicaSetName()).isEqualTo("some-replica-set");
	}

	@Test
	void databaseCanBeCustomized() {
		this.properties.setDatabase("foo");
		this.properties.setUsername("user");
		this.properties.setPassword("secret".toCharArray());
		this.connectionDetails.setConnectionString("mongodb://some-user:some-password@localhost/database-1");
		MongoClientSettings settings = customizeSettings();
		assertMongoDatabase(settings.getCredential(), "database-1");
	}

	@Test
	// TODO database and authentication database are the same thing
	void authenticationDatabaseCanBeCustomized() {
		this.properties.setAuthenticationDatabase("foo");
		this.properties.setUsername("user");
		this.properties.setPassword("secret".toCharArray());
		this.connectionDetails
			.setConnectionString("mongodb://some-user:some-password@localhost/authentication-database-1");
		MongoClientSettings settings = customizeSettings();
		assertMongoDatabase(settings.getCredential(), "authentication-database-1");
	}

	@Test
	void uuidRepresentationDefaultToJavaLegacy() {
		this.connectionDetails.setConnectionString("mongodb://localhost");
		MongoClientSettings settings = customizeSettings();
		assertThat(settings.getUuidRepresentation()).isEqualTo(UuidRepresentation.JAVA_LEGACY);
	}

	@Test
	void uuidRepresentationCanBeCustomized() {
		this.properties.setUuidRepresentation(UuidRepresentation.STANDARD);
		this.connectionDetails.setConnectionString("mongodb://localhost");
		MongoClientSettings settings = customizeSettings();
		assertThat(settings.getUuidRepresentation()).isEqualTo(UuidRepresentation.STANDARD);
	}

	@Test
	void uriHasNoEffectWhenConnectionDetailsAreUsed() {
		this.properties.setUri("mongodb://mongo1.example.com:12345");
		this.connectionDetails.setConnectionString("mongodb://some-other-mongo.example.com:23456");
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "some-other-mongo.example.com", 23456);
	}

	@Test
	void retryWritesIsPropagatedFromConnectionString() {
		this.connectionDetails.setConnectionString("mongodb://localhost/?retryWrites=false");
		MongoClientSettings settings = customizeSettings();
		assertThat(settings.getRetryWrites()).isFalse();
	}

	private MongoClientSettings customizeSettings() {
		MongoClientSettings.Builder settings = MongoClientSettings.builder();
		new MongoPropertiesClientSettingsBuilderCustomizer(this.properties, this.connectionDetails).customize(settings);
		return settings.build();
	}

	private List<ServerAddress> getAllAddresses(MongoClientSettings settings) {
		return settings.getClusterSettings().getHosts();
	}

	private void assertServerAddress(ServerAddress serverAddress, String expectedHost, int expectedPort) {
		assertThat(serverAddress.getHost()).isEqualTo(expectedHost);
		assertThat(serverAddress.getPort()).isEqualTo(expectedPort);
	}

	private void assertMongoDatabase(MongoCredential credential, String expectedDatabase) {
		assertThat(credential.getSource()).isEqualTo(expectedDatabase);
	}

	private void assertMongoCredential(MongoCredential credentials, String expectedUsername, String expectedPassword) {
		assertThat(credentials.getUserName()).isEqualTo(expectedUsername);
		assertThat(credentials.getPassword()).isEqualTo(expectedPassword.toCharArray());
	}

	private static final class TestMongoConnectionDetails implements MongoConnectionDetails {

		private ConnectionString connectionString;

		@Override
		public ConnectionString getConnectionString() {
			return this.connectionString;
		}

		private void setConnectionString(String connectionString) {
			this.connectionString = new ConnectionString(connectionString);
		}

	}

}
