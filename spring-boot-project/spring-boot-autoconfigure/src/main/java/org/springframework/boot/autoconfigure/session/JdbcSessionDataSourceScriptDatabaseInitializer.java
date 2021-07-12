/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.session;

import java.util.Arrays;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;

/**
 * {@link DataSourceScriptDatabaseInitializer Initializer} for the Spring Session JDBC
 * database schema.
 *
 * @author Dave Syer
 * @author Vedran Pavic
 * @author Andy Wilkinson
 * @since 2.6.0
 */
public class JdbcSessionDataSourceScriptDatabaseInitializer extends DataSourceScriptDatabaseInitializer {

	private static final String PLATFORM_PLACEHOLDER = "@@platform@@";

	public JdbcSessionDataSourceScriptDatabaseInitializer(DataSource dataSource, JdbcSessionProperties properties) {
		super(dataSource, createSettings(dataSource, properties));
	}

	private static DatabaseInitializationSettings createSettings(DataSource dataSource,
			JdbcSessionProperties properties) {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(Arrays.asList(determineSchemaLocation(dataSource, properties)));
		settings.setMode(properties.getInitializeSchema());
		settings.setContinueOnError(true);
		return settings;
	}

	private static String determineSchemaLocation(DataSource dataSource, JdbcSessionProperties properties) {
		String schema = properties.getSchema();
		if (schema.contains(PLATFORM_PLACEHOLDER)) {
			String platform = determineDriverId(dataSource);
			schema = schema.replace(PLATFORM_PLACEHOLDER, platform);
		}
		return schema;
	}

	private static String determineDriverId(DataSource dataSource) {
		DatabaseDriver databaseDriver = DatabaseDriver.fromDataSource(dataSource);
		if (databaseDriver == DatabaseDriver.UNKNOWN) {
			throw new IllegalStateException("Unable to detect database type");
		}
		return databaseDriver.getId();
	}

}
