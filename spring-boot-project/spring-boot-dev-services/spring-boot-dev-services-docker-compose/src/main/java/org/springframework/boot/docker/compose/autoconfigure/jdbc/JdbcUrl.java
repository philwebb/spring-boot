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

package org.springframework.boot.docker.compose.autoconfigure.jdbc;

import org.springframework.boot.devservices.dockercompose.interop.Port;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;

/**
 * @author pwebb
 */
public class JdbcUrl {

	/**
	 * @param source
	 * @param mysqlPort
	 * @param extractDatabase
	 */
	public JdbcUrl(RunningService source, String thing, int mappedPort, String database) {
		Port port = source.getMappedPort(MYSQL_PORT);
		String parameters = source.labels().getOrDefault("org.springframework.boot.jdbc.parameters", "");
		this.jdbcUrl = "jdbc:%s://%s:%d/%s%s".formatted(thing, source.host(), port.number(), database, parameters);
	}

}
