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

package org.springframework.boot.docker.compose.autoconfigure.r2dbc;

/**
 * @author pwebb
 */

import io.r2dbc.spi.ConnectionFactoryOptions;

import org.springframework.boot.docker.compose.service.RunningService;

public class ConnectionFactoryOptionsBuilder {

	private String driver;

	private int sourcePort;

	public ConnectionFactoryOptionsBuilder(String driver, int sourcePort) {
		this.driver = driver;
		this.sourcePort = sourcePort;
	}

	public ConnectionFactoryOptions build(RunningService service, String database, String user,
			String password) {
		return ConnectionFactoryOptions.builder()
			.option(ConnectionFactoryOptions.DRIVER, this.driver)
			.option(ConnectionFactoryOptions.HOST, service.host())
			.option(ConnectionFactoryOptions.PORT, service.getMappedPort(this.sourcePort).number())
			.option(ConnectionFactoryOptions.DATABASE, database)
			.option(ConnectionFactoryOptions.USER, user)
			.option(ConnectionFactoryOptions.PASSWORD, password)
			.build();
	}

	// FIXME String parameters =
	// source.labels().get("org.springframework.boot.r2dbc.parameters");

}
