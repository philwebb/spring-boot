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

package org.springframework.boot.devservices.dockercompose.database.mysql;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnection;
import org.springframework.boot.devservices.dockercompose.RunningServiceServiceConnectionProvider;
import org.springframework.boot.devservices.dockercompose.database.AbstractJdbcServiceConnection;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.util.ClassUtils;

/**
 * Handles connections to a MySQL service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class MySqlConnectionProvider implements RunningServiceServiceConnectionProvider {

	private final boolean jdbcServiceConnectionPresent;

	MySqlConnectionProvider(ClassLoader classLoader) {
		this.jdbcServiceConnectionPresent = ClassUtils
			.isPresent("org.springframework.boot.autoconfigure.jdbc.JdbcServiceConnection", classLoader);
	}

	@Override
	public List<? extends ServiceConnection> provideServiceConnection(List<RunningService> services) {
		List<ServiceConnection> result = new ArrayList<>();
		for (RunningService service : services) {
			if (!MySqlService.matches(service)) {
				continue;
			}
			MySqlService mySqlService = new MySqlService(service);
			if (this.jdbcServiceConnectionPresent) {
				result.add(new DockerComposeMysqlDbJdbcServiceConnection(mySqlService));
			}
		}
		return result;
	}

	private static class DockerComposeMysqlDbJdbcServiceConnection extends AbstractJdbcServiceConnection {

		DockerComposeMysqlDbJdbcServiceConnection(MySqlService service) {
			super(service);
		}

		@Override
		protected String getJdbcSubProtocol() {
			return DatabaseDriver.MYSQL.getUrlPrefixes().iterator().next();
		}

		@Override
		public String getName() {
			return "docker-compose-mysql-jdbc-%s".formatted(this.service.getName());
		}

	}

}
