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

package org.springframework.boot.autoconfigure.batch;

import java.util.Arrays;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.batch.BatchProperties.Jdbc;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;

/**
 * {@link DataSourceScriptDatabaseInitializer Initializer} for the Spring Batch database
 * schema.
 *
 * @author Dave Syer
 * @author Vedran Pavic
 * @author Andy Wilkinson
 * @since 2.6.0
 */
public class BatchDataSourceScriptDatabaseInitializer extends DataSourceScriptDatabaseInitializer {

	private static final String PLATFORM_PLACEHOLDER = "@@platform@@";

	public BatchDataSourceScriptDatabaseInitializer(DataSource dataSource, BatchProperties properties) {
		super(dataSource, createSettings(dataSource, properties.getJdbc()));
	}

	private static DatabaseInitializationSettings createSettings(DataSource dataSource, Jdbc properties) {
		DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
		settings.setSchemaLocations(Arrays.asList(determineSchemaLocation(dataSource, properties)));
		settings.setMode(properties.getInitializeSchema());
		settings.setContinueOnError(true);
		return settings;
	}

	private static String determineSchemaLocation(DataSource dataSource, Jdbc properties) {
		String schema = properties.getSchema();
		if (schema.contains(PLATFORM_PLACEHOLDER)) {
			String platform = determinePlatform(dataSource);
			schema = schema.replace(PLATFORM_PLACEHOLDER, platform);
		}
		return schema;
	}

	private static String determinePlatform(DataSource dataSource) {
		String driverId = determineDriverId(dataSource);
		if ("oracle".equals(driverId)) {
			return "oracle10g";
		}
		if ("mariadb".equals(driverId)) {
			return "mysql";
		}
		return driverId;
	}

	private static String determineDriverId(DataSource dataSource) {
		DatabaseDriver databaseDriver = DatabaseDriver.fromDataSource(dataSource);
		if (databaseDriver == DatabaseDriver.UNKNOWN) {
			throw new IllegalStateException("Unable to detect database type");
		}
		return databaseDriver.getId();
	}

}
