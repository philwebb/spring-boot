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

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.bson.UuidRepresentation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.mongo.MongoConnectionDetails.Host;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoPropertiesClientSettingsBuilderCustomizer}.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 */
class MongoPropertiesClientSettingsBuilderCustomizerTests {

	private MongoProperties properties;

	private MongoConnectionDetails serviceConnection;

	@BeforeEach
	void setUp() {
		this.properties = new MongoProperties();
		this.serviceConnection = null;
	}

	@Test
	void portCanBeCustomized() {
		this.properties.setPort(12345);
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(1);
		assertThat(allAddresses.get(0).getPort()).isEqualTo(12345);
	}

	@Test
	void portCanBeCustomizedWhenUsingServiceConnection() {
		this.properties.setPort(12345);
		this.serviceConnection = TestMongoServiceConnection.create().withPort(23456);
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(1);
		assertThat(allAddresses.get(0).getPort()).isEqualTo(23456);
	}

	@Test
	void hostCanBeCustomized() {
		this.properties.setHost("mongo.example.com");
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(1);
		assertThat(allAddresses.get(0).getHost()).isEqualTo("mongo.example.com");
	}

	@Test
	void hostCanBeCustomizedWhenUsingServiceConnection() {
		this.properties.setHost("mongo.example.com");
		this.serviceConnection = TestMongoServiceConnection.create().withHost("some-other-mongo.example.com");
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(1);
		assertThat(allAddresses.get(0).getHost()).isEqualTo("some-other-mongo.example.com");
	}

	@Test
	void additionalHostCanBeAdded() {
		this.properties.setHost("mongo.example.com");
		this.properties.setAdditionalHosts(Arrays.asList("mongo.example.com:33", "mongo.example2.com"));
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(3);
		assertServerAddress(allAddresses.get(0), "mongo.example.com", 27017);
		assertServerAddress(allAddresses.get(1), "mongo.example.com", 33);
		assertServerAddress(allAddresses.get(2), "mongo.example2.com", 27017);
	}

	@Test
	void additionalHostCanBeAddedWhenUsingServiceConnection() {
		this.properties.setHost("mongo.example.com");
		this.properties.setAdditionalHosts(Arrays.asList("mongo.example.com:33", "mongo.example2.com"));
		this.serviceConnection = TestMongoServiceConnection.create()
			.withPort(1337)
			.withHost("mongo1.example.com")
			.withAdditionalHosts(new Host("mongo2.example.com", 1234), new Host("mongo3.example.com", 2345));
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
		MongoClientSettings settings = customizeSettings();
		assertMongoCredential(settings.getCredential(), "user", "secret");
	}

	@Test
	void credentialsCanBeCustomizedWhenUsingServiceConnection() {
		this.properties.setUsername("user");
		this.properties.setPassword("secret".toCharArray());
		this.serviceConnection = TestMongoServiceConnection.create()
			.withUsername("some-user")
			.withPassword("some-password");
		MongoClientSettings settings = customizeSettings();
		assertMongoCredential(settings.getCredential(), "some-user", "some-password");
	}

	@Test
	void replicaSetCanBeCustomized() {
		this.properties.setReplicaSetName("test");
		MongoClientSettings settings = customizeSettings();
		assertThat(settings.getClusterSettings().getRequiredReplicaSetName()).isEqualTo("test");
	}

	@Test
	void replicaSetCanBeCustomizedWhenUsingServiceConnection() {
		this.properties.setReplicaSetName("test");
		this.serviceConnection = TestMongoServiceConnection.create().withReplicaSetName("some-replicate-set");
		MongoClientSettings settings = customizeSettings();
		assertThat(settings.getClusterSettings().getRequiredReplicaSetName()).isEqualTo("some-replicate-set");
	}

	@Test
	void databaseCanBeCustomized() {
		this.properties.setDatabase("foo");
		this.properties.setUsername("user");
		this.properties.setPassword("secret".toCharArray());
		MongoClientSettings settings = customizeSettings();
		assertMongoDatabase(settings.getCredential(), "foo");
	}

	@Test
	void databaseCanBeCustomizedWhenUsingServiceConnection() {
		this.properties.setDatabase("foo");
		this.properties.setUsername("user");
		this.properties.setPassword("secret".toCharArray());
		this.serviceConnection = TestMongoServiceConnection.create()
			.withDatabase("database-1")
			.withAuthenticationDatabase(null);
		MongoClientSettings settings = customizeSettings();
		assertMongoDatabase(settings.getCredential(), "database-1");
	}

	@Test
	void uuidRepresentationDefaultToJavaLegacy() {
		MongoClientSettings settings = customizeSettings();
		assertThat(settings.getUuidRepresentation()).isEqualTo(UuidRepresentation.JAVA_LEGACY);
	}

	@Test
	void uuidRepresentationCanBeCustomized() {
		this.properties.setUuidRepresentation(UuidRepresentation.STANDARD);
		MongoClientSettings settings = customizeSettings();
		assertThat(settings.getUuidRepresentation()).isEqualTo(UuidRepresentation.STANDARD);
	}

	@Test
	void authenticationDatabaseCanBeCustomized() {
		this.properties.setAuthenticationDatabase("foo");
		this.properties.setUsername("user");
		this.properties.setPassword("secret".toCharArray());
		MongoClientSettings settings = customizeSettings();
		assertMongoDatabase(settings.getCredential(), "foo");
	}

	@Test
	void authenticationDatabaseCanBeCustomizedWhenUsingServiceConnection() {
		this.properties.setAuthenticationDatabase("foo");
		this.properties.setUsername("user");
		this.properties.setPassword("secret".toCharArray());
		this.serviceConnection = TestMongoServiceConnection.create()
			.withAuthenticationDatabase("authentication-database-1");
		MongoClientSettings settings = customizeSettings();
		assertMongoDatabase(settings.getCredential(), "authentication-database-1");
	}

	@Test
	void uriHasNoEffectWhenServiceConnectionIsUsed() {
		this.properties.setUri("mongodb://mongo1.example.com:12345");
		this.serviceConnection = TestMongoServiceConnection.create()
			.withHost("some-other-mongo.example.com")
			.withPort(23456);
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "some-other-mongo.example.com", 23456);
	}

	@Test
	void onlyHostAndPortSetShouldUseThat() {
		this.properties.setHost("localhost");
		this.properties.setPort(27017);
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "localhost", 27017);
	}

	@Test
	void onlyUriSetShouldUseThat() {
		this.properties.setUri("mongodb://mongo1.example.com:12345");
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "mongo1.example.com", 12345);
	}

	@Test
	void noCustomAddressAndNoUriUsesDefaultUri() {
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "localhost", 27017);
	}

	@Test
	void uriCanBeCustomized() {
		this.properties.setUri("mongodb://user:secret@mongo1.example.com:12345,mongo2.example.com:23456/test");
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(2);
		assertServerAddress(allAddresses.get(0), "mongo1.example.com", 12345);
		assertServerAddress(allAddresses.get(1), "mongo2.example.com", 23456);
		assertMongoCredential(settings.getCredential(), "user", "secret");
		assertMongoDatabase(settings.getCredential(), "test");
	}

	@Test
	void uriOverridesUsernameAndPassword() {
		this.properties.setUri("mongodb://127.0.0.1:1234/mydb");
		this.properties.setUsername("user");
		this.properties.setPassword("secret".toCharArray());
		MongoClientSettings settings = customizeSettings();
		assertThat(settings.getCredential()).isNull();
	}

	@Test
	void uriOverridesDatabase() {
		this.properties.setUri("mongodb://secret:password@127.0.0.1:1234/mydb");
		this.properties.setDatabase("test");
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> allAddresses = getAllAddresses(settings);
		assertThat(allAddresses).hasSize(1);
		assertServerAddress(allAddresses.get(0), "127.0.0.1", 1234);
		assertThat(settings.getCredential().getSource()).isEqualTo("mydb");
	}

	@Test
	void uriOverridesHostAndPort() {
		this.properties.setUri("mongodb://127.0.0.1:1234/mydb");
		this.properties.setHost("localhost");
		this.properties.setPort(4567);
		MongoClientSettings settings = customizeSettings();
		List<ServerAddress> addresses = getAllAddresses(settings);
		assertThat(addresses.get(0).getHost()).isEqualTo("127.0.0.1");
		assertThat(addresses.get(0).getPort()).isEqualTo(1234);
	}

	@Test
	void retryWritesIsPropagatedFromUri() {
		this.properties.setUri("mongodb://localhost/test?retryWrites=false");
		MongoClientSettings settings = customizeSettings();
		assertThat(settings.getRetryWrites()).isFalse();
	}

	private MongoClientSettings customizeSettings() {
		MongoClientSettings.Builder settings = MongoClientSettings.builder();
		new MongoPropertiesClientSettingsBuilderCustomizer(this.properties, this.serviceConnection).customize(settings);
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

}
