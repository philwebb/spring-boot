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

package org.springframework.boot.autoconfigure.r2dbc;

import java.util.function.Predicate;
import java.util.function.Supplier;

import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.ConnectionFactoryOptions.Builder;
import io.r2dbc.spi.Option;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.r2dbc.EmbeddedDatabaseConnection;
import org.springframework.util.StringUtils;

/**
 * Initialize a {@link Builder} based on {@link R2dbcProperties}.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 */
class ConnectionFactoryOptionsInitializer {

	/**
	 * Initialize a {@link Builder ConnectionFactoryOptions.Builder} using the specified
	 * properties.
	 * @param properties the properties to use to initialize the builder
	 * @param serviceConnection the service connection to use to initialize the builder or
	 * {@code null}
	 * @param embeddedDatabaseConnection the embedded connection to use as a fallback
	 * @return an initialized builder
	 * @throws ConnectionFactoryBeanCreationException if no suitable connection could be
	 * determined
	 */
	ConnectionFactoryOptions.Builder initialize(R2dbcProperties properties, R2dbcServiceConnection serviceConnection,
			Supplier<EmbeddedDatabaseConnection> embeddedDatabaseConnection) {
		String url = (serviceConnection != null) ? serviceConnection.getR2dbcUrl() : properties.getUrl();
		if (StringUtils.hasText(url)) {
			return initializeRegularOptions(url, properties, serviceConnection);
		}
		EmbeddedDatabaseConnection embeddedConnection = embeddedDatabaseConnection.get();
		if (embeddedConnection != EmbeddedDatabaseConnection.NONE) {
			return initializeEmbeddedOptions(properties, embeddedConnection);
		}
		throw connectionFactoryBeanCreationException("Failed to determine a suitable R2DBC Connection URL", properties,
				embeddedConnection);
	}

	private ConnectionFactoryOptions.Builder initializeRegularOptions(String url, R2dbcProperties properties,
			R2dbcServiceConnection serviceConnection) {
		ConnectionFactoryOptions urlOptions = ConnectionFactoryOptions.parse(url);
		Builder optionsBuilder = urlOptions.mutate();
		String username = (serviceConnection != null) ? serviceConnection.getUsername() : properties.getUsername();
		String password = (serviceConnection != null) ? serviceConnection.getPassword() : properties.getPassword();
		configureIf(optionsBuilder, urlOptions, ConnectionFactoryOptions.USER, () -> username, StringUtils::hasText);
		configureIf(optionsBuilder, urlOptions, ConnectionFactoryOptions.PASSWORD, () -> password,
				StringUtils::hasText);
		configureIf(optionsBuilder, urlOptions, ConnectionFactoryOptions.DATABASE,
				() -> determineDatabaseName(properties, serviceConnection), StringUtils::hasText);
		if (properties.getProperties() != null) {
			properties.getProperties().forEach((key, value) -> optionsBuilder.option(Option.valueOf(key), value));
		}
		return optionsBuilder;
	}

	private Builder initializeEmbeddedOptions(R2dbcProperties properties,
			EmbeddedDatabaseConnection embeddedDatabaseConnection) {
		String url = embeddedDatabaseConnection.getUrl(determineEmbeddedDatabaseName(properties));
		if (url == null) {
			throw connectionFactoryBeanCreationException("Failed to determine a suitable R2DBC Connection URL",
					properties, embeddedDatabaseConnection);
		}
		Builder builder = ConnectionFactoryOptions.parse(url).mutate();
		String username = determineEmbeddedUsername(properties);
		if (StringUtils.hasText(username)) {
			builder.option(ConnectionFactoryOptions.USER, username);
		}
		if (StringUtils.hasText(properties.getPassword())) {
			builder.option(ConnectionFactoryOptions.PASSWORD, properties.getPassword());
		}
		return builder;
	}

	private String determineDatabaseName(R2dbcProperties properties, R2dbcServiceConnection serviceConnection) {
		if (serviceConnection != null) {
			return null;
		}
		if (properties.isGenerateUniqueName()) {
			return properties.determineUniqueName();
		}
		if (StringUtils.hasLength(properties.getName())) {
			return properties.getName();
		}
		return null;
	}

	private String determineEmbeddedDatabaseName(R2dbcProperties properties) {
		String databaseName = determineDatabaseName(properties, null);
		return (databaseName != null) ? databaseName : "testdb";
	}

	private String determineEmbeddedUsername(R2dbcProperties properties) {
		String username = ifHasText(properties.getUsername());
		return (username != null) ? username : "sa";
	}

	private <T extends CharSequence> void configureIf(Builder optionsBuilder, ConnectionFactoryOptions originalOptions,
			Option<T> option, Supplier<T> valueSupplier, Predicate<T> setIf) {
		if (originalOptions.hasOption(option)) {
			return;
		}
		T value = valueSupplier.get();
		if (setIf.test(value)) {
			optionsBuilder.option(option, value);
		}
	}

	private ConnectionFactoryBeanCreationException connectionFactoryBeanCreationException(String message,
			R2dbcProperties properties, EmbeddedDatabaseConnection embeddedDatabaseConnection) {
		return new ConnectionFactoryBeanCreationException(message, properties, embeddedDatabaseConnection);
	}

	private String ifHasText(String candidate) {
		return (StringUtils.hasText(candidate)) ? candidate : null;
	}

	static class ConnectionFactoryBeanCreationException extends BeanCreationException {

		private final R2dbcProperties properties;

		private final EmbeddedDatabaseConnection embeddedDatabaseConnection;

		ConnectionFactoryBeanCreationException(String message, R2dbcProperties properties,
				EmbeddedDatabaseConnection embeddedDatabaseConnection) {
			super(message);
			this.properties = properties;
			this.embeddedDatabaseConnection = embeddedDatabaseConnection;
		}

		EmbeddedDatabaseConnection getEmbeddedDatabaseConnection() {
			return this.embeddedDatabaseConnection;
		}

		R2dbcProperties getProperties() {
			return this.properties;
		}

	}

}
