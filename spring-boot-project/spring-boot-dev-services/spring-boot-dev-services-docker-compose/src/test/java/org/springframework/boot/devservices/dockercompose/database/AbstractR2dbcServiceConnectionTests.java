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

package org.springframework.boot.devservices.dockercompose.database;

import org.junit.jupiter.api.Test;

import org.springframework.boot.devservices.dockercompose.interop.RunningService;
import org.springframework.boot.devservices.dockercompose.test.RunningServiceBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractR2dbcServiceConnection}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class AbstractR2dbcServiceConnectionTests {

	@Test
	void getR2dbcUrl() {
		DummyR2dbcServiceConnection serviceConnection = new DummyR2dbcServiceConnection(
				RunningServiceBuilder.create("service-1", "service:1").build());
		assertThat(serviceConnection.getUsername()).isEqualTo("username-1");
		assertThat(serviceConnection.getPassword()).isEqualTo("password-1");
		assertThat(serviceConnection.getR2dbcUrl()).isEqualTo("r2dbc:dummy://127.0.0.1:12345/database-1");
	}

	@Test
	void shouldUseParameters() {
		DummyR2dbcServiceConnection serviceConnection = new DummyR2dbcServiceConnection(
				RunningServiceBuilder.create("service-1", "service:1")
					.addLabel("org.springframework.boot.r2dbc.parameters", "ssl=true&defaultRowFetchSize=100")
					.build());
		assertThat(serviceConnection.getR2dbcUrl())
			.isEqualTo("r2dbc:dummy://127.0.0.1:12345/database-1?ssl=true&defaultRowFetchSize=100");
	}

	private static class DummyR2dbcServiceConnection extends AbstractR2dbcServiceConnection {

		DummyR2dbcServiceConnection(RunningService service) {
			super(new DummyDatabaseService(service));
		}

		@Override
		public String getName() {
			return "dummy";
		}

		@Override
		protected String getR2dbcSubProtocol() {
			return "dummy";
		}

	}

	private static class DummyDatabaseService extends DatabaseService {

		DummyDatabaseService(RunningService service) {
			super(service);
		}

		@Override
		public String getUsername() {
			return "username-1";
		}

		@Override
		public String getPassword() {
			return "password-1";
		}

		@Override
		public String getDatabase() {
			return "database-1";
		}

		@Override
		public int getPort() {
			return 12345;
		}

	}

}
