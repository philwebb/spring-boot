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

package org.springframework.boot.jdbc.init;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Utility class that can resolve placholder text with the actual {@link DatabaseDriver}
 * platform.
 *
 * @author Phillip Webb
 * @since 2.6.0
 */
public class PlaformPlaceholderDatabaseDriverResolver {

	private final String placeholder;

	private final Map<String, String> driverIdMappings;

	public PlaformPlaceholderDatabaseDriverResolver() {
		this("@@platform@@");
	}

	public PlaformPlaceholderDatabaseDriverResolver(String placeholder) {
		this.placeholder = placeholder;
		this.driverIdMappings = Collections.emptyMap();
	}

	private PlaformPlaceholderDatabaseDriverResolver(String placeholder, Map<String, String> driverIdMappings) {
		this.placeholder = placeholder;
		this.driverIdMappings = driverIdMappings;
	}

	public PlaformPlaceholderDatabaseDriverResolver withDriverIdMapping(String from, String to) {
		Map<String, String> driverIdMappings = new LinkedHashMap<>(this.driverIdMappings);
		driverIdMappings.put(from, to);
		return new PlaformPlaceholderDatabaseDriverResolver(this.placeholder, driverIdMappings);

	}

	public List<String> resolveAll(DataSource dataSource, String... values) {
		if (ObjectUtils.isEmpty(values)) {
			return Collections.emptyList();
		}
		List<String> resolved = new ArrayList<>(values.length);
		String platform = null;
		for (String value : values) {
			if (StringUtils.hasLength(value)) {
				if (value.contains(this.placeholder)) {
					platform = (platform != null) ? platform : determinePlatform(dataSource);
					value = value.replace(this.placeholder, platform);
				}
				resolved.add(value);
			}
		}
		return Collections.unmodifiableList(resolved);
	}

	private String determinePlatform(DataSource dataSource) {
		DatabaseDriver databaseDriver = getDatabaseDriver(dataSource);
		Assert.state(databaseDriver != DatabaseDriver.UNKNOWN, "Unable to detect database type");
		String driverId = databaseDriver.getId();
		if (this.driverIdMappings.containsKey(driverId)) {
			driverId = this.driverIdMappings.get(driverId);
		}
		return driverId;
	}

	DatabaseDriver getDatabaseDriver(DataSource dataSource) {
		return DatabaseDriver.fromDataSource(dataSource);
	}

}
