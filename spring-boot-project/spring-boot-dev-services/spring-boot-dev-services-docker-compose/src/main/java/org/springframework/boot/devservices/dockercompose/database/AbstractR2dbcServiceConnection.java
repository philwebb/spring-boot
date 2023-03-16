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

import org.springframework.boot.autoconfigure.r2dbc.R2dbcServiceConnection;
import org.springframework.boot.origin.Origin;

/**
 * Abstract base class for {@link R2dbcServiceConnection} implementations.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public abstract class AbstractR2dbcServiceConnection implements R2dbcServiceConnection {

	protected final DatabaseService service;

	public AbstractR2dbcServiceConnection(DatabaseService service) {
		this.service = service;
	}

	@Override
	public String getR2dbcUrl() {
		String parameters = getParameters();
		parameters = (parameters != null) ? "?" + parameters : "";
		return "r2dbc:%s://%s:%d/%s%s".formatted(getR2dbcSubProtocol(), this.service.getHost(), this.service.getPort(),
				this.service.getDatabase(), parameters);
	}

	/**
	 * Returns the name of the R2DBC sub-protocol (the part after 'r2dbc:'), e.g. 'mysql'
	 * for MySQL or 'postgresql' for PostgreSQL.
	 * @return the name of the R2DBC sub-protocol
	 */
	protected abstract String getR2dbcSubProtocol();

	@Override
	public String getUsername() {
		return this.service.getUsername();
	}

	@Override
	public String getPassword() {
		return this.service.getPassword();
	}

	@Override
	public Origin getOrigin() {
		return this.service.getOrigin();
	}

	@Override
	public String toString() {
		return "DockerCompose[host='%s',port=%d,database='%s',username='%s']".formatted(this.service.getHost(),
				this.service.getPort(), this.service.getDatabase(), this.service.getUsername());
	}

	private String getParameters() {
		return this.service.getLabels().get("org.springframework.boot.r2dbc.parameters");
	}

}
