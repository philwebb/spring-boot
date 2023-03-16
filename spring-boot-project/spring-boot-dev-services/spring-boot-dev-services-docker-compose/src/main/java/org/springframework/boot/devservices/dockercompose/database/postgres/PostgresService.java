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

package org.springframework.boot.devservices.dockercompose.database.postgres;

import org.springframework.boot.devservices.dockercompose.database.DatabaseService;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;

/**
 * A PostgreSQL service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class PostgresService extends DatabaseService {

	private static final int POSTGRES_PORT = 5432;

	PostgresService(RunningService service) {
		super(service);
	}

	@Override
	public String getUsername() {
		if (this.service.env().containsKey("POSTGRES_USER")) {
			return this.service.env().get("POSTGRES_USER");
		}
		return "postgres";
	}

	@Override
	public String getPassword() {
		if (this.service.env().containsKey("POSTGRES_PASSWORD")) {
			return this.service.env().get("POSTGRES_PASSWORD");
		}
		throw new IllegalStateException(
				"Can't find password for user. Use the POSTGRES_PASSWORD environment variable to set it.");
	}

	@Override
	public String getDatabase() {
		if (this.service.env().containsKey("POSTGRES_DB")) {
			return this.service.env().get("POSTGRES_DB");
		}
		return getUsername();
	}

	@Override
	public int getPort() {
		return getMappedPortOrThrow(POSTGRES_PORT);
	}

	static boolean matches(RunningService service) {
		return service.image().image().equals("postgres");
	}

}
