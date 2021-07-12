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

package org.springframework.boot.autoconfigure.quartz;

import java.util.Arrays;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;

/**
 * {@link DataSourceScriptDatabaseInitializer Initializer} for the Quartz Scheduler
 * database schema.
 *
 * @author Vedran Pavic
 * @author Andy Wilkinson
 * @since 2.6.0
 */
public class QuartzDataSourceScriptDatabaseInitializer extends DataSourceScriptDatabaseInitializer {

	private static final String PLATFORM_PLACEHOLDER = "@@platform@@";

	public QuartzDataSourceScriptDatabaseInitializer(DataSource dataSource, QuartzProperties properties) {
		super(dataSource, createSettings(dataSource, properties));
	}

	private static DatabaseInitializationSettings createSettings(DataSource dataSource, QuartzProperties properties) {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(Arrays.asList(determineSchemaLocation(dataSource, properties)));
		settings.setMode(properties.getJdbc().getInitializeSchema());
		settings.setContinueOnError(true);
		return settings;
	}

	private static String determineSchemaLocation(DataSource dataSource, QuartzProperties properties) {
		String schema = properties.getJdbc().getSchema();
		if (schema.contains(PLATFORM_PLACEHOLDER)) {
			String platform = determinePlatform(dataSource);
			schema = schema.replace(PLATFORM_PLACEHOLDER, platform);
		}
		return schema;
	}

	private static String determinePlatform(DataSource dataSource) {
		String platform = determineDriverId(dataSource);
		if ("db2".equals(platform)) {
			return "db2_v95";
		}
		if ("mysql".equals(platform) || "mariadb".equals(platform)) {
			return "mysql_innodb";
		}
		if ("postgresql".equals(platform)) {
			return "postgres";
		}
		if ("sqlserver".equals(platform)) {
			return "sqlServer";
		}
		return platform;
	}

	private static String determineDriverId(DataSource dataSource) {
		DatabaseDriver databaseDriver = DatabaseDriver.fromDataSource(dataSource);
		if (databaseDriver == DatabaseDriver.UNKNOWN) {
			throw new IllegalStateException("Unable to detect database type");
		}
		return databaseDriver.getId();
	}

}
