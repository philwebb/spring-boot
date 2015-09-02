/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.data.neo4j;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.neo4j.server.Neo4jServer;
import org.springframework.data.neo4j.server.RemoteServer;

/**
 * Configuration properties for Neo4j.
 *
 * @author Michael Hunger
 * @author Phillip Webb
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.data.neo4j")
public class Neo4jProperties {

	/**
	 * Default URL used when the configuration url is not set.
	 */
	public static final String DEFAULT_URL = "http://localhost:7474";

	/**
	 * Neo4j database URL.
	 */
	private String url = DEFAULT_URL;

	/**
	 * Login user of the neo4j server.
	 */
	private String username;

	/**
	 * Login password of the neo4j server.
	 */
	private char[] password;

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public char[] getPassword() {
		return this.password;
	}

	public void setPassword(char[] password) {
		this.password = password;
	}

	public void clearPassword() {
		if (this.password == null) {
			return;
		}
		for (int i = 0; i < this.password.length; i++) {
			this.password[i] = 0;
		}
	}

	/**
	 * Creates a {@link Neo4jServer} representation.
	 * @return the Neo4j server representation
	 */
	public Neo4jServer createNeo4jServer() {
		try {
			if (this.username == null && this.password == null) {
				return new RemoteServer(this.url);
			}
			return new RemoteServer(this.url, this.username,
					String.valueOf(this.password));
		}
		finally {
			clearPassword();
		}
	}

}
