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

import org.springframework.boot.devservices.dockercompose.database.DatabaseService;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;

/**
 * A MySQL service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class MySqlService extends DatabaseService {

	private static final int MYSQL_PORT = 3306;

	MySqlService(RunningService service) {
		super(service);
	}

	@Override
	public String getUsername() {
		if (this.service.env().containsKey("MYSQL_USER")) {
			return this.service.env().get("MYSQL_USER");
		}
		return "root";
	}

	@Override
	public String getPassword() {
		if (this.service.env().containsKey("MYSQL_PASSWORD")) {
			return this.service.env().get("MYSQL_PASSWORD");
		}
		if (this.service.env().containsKey("MYSQL_ROOT_PASSWORD")) {
			return this.service.env().get("MYSQL_ROOT_PASSWORD");
		}
		if (this.service.env().containsKey("MYSQL_ALLOW_EMPTY_PASSWORD")) {
			return "";
		}
		if (this.service.env().containsKey("MYSQL_RANDOM_ROOT_PASSWORD")) {
			throw new IllegalStateException("MYSQL_RANDOM_ROOT_PASSWORD is not supported");
		}
		throw new IllegalStateException("Can't find password for user");
	}

	@Override
	public String getDatabase() {
		if (this.service.env().containsKey("MYSQL_DATABASE")) {
			return this.service.env().get("MYSQL_DATABASE");
		}
		throw new IllegalStateException("No database name found. Use MYSQL_DATABASE to specify it");
	}

	@Override
	public int getPort() {
		return getMappedPortOrThrow(MYSQL_PORT);
	}

	static boolean matches(RunningService service) {
		return service.image().image().equals("mysql");
	}

}
