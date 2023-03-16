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

package org.springframework.boot.autoconfigure.elasticsearch;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnection;

/**
 * A connection to an Elasticsearch service.
 *
 * @author Moritz Halbritter
 * @since 3.1.0
 */
public interface ElasticsearchServiceConnection extends ServiceConnection {

	/**
	 * List of the Elasticsearch nodes to use.
	 * @return list of the Elasticsearch nodes to use
	 */
	List<Node> getNodes();

	/**
	 * Username for authentication with Elasticsearch.
	 * @return username for authentication with Elasticsearch or {@code null}
	 */
	String getUsername();

	/**
	 * Password for authentication with Elasticsearch.
	 * @return password for authentication with Elasticsearch or {@code null}
	 */
	String getPassword();

	/**
	 * Prefix added to the path of every request sent to Elasticsearch.
	 * @return prefix added to the path of every request sent to Elasticsearch or
	 * {@code null}
	 */
	String getPathPrefix();

	/**
	 * An elasticsearch node.
	 *
	 * @param hostname the hostname
	 * @param port the port
	 * @param protocol the protocol
	 * @param username the username or {@code null}
	 * @param password the password or {@code null}
	 */
	record Node(String hostname, int port, Protocol protocol, String username, String password) {
		public Node(String host, int port, Protocol protocol) {
			this(host, port, protocol, null, null);
		}

		boolean hasCredentials() {
			return this.username != null && this.password != null;
		}

		URI toUri() {
			String userInfo = (hasCredentials()) ? this.username + ":" + this.password : null;
			try {
				return new URI(this.protocol.getScheme(), userInfo, this.hostname, this.port, null, null, null);
			}
			catch (URISyntaxException ex) {
				throw new IllegalStateException("Can't construct URI", ex);
			}
		}

		/**
		 * Connection protocol.
		 */
		public enum Protocol {

			/**
			 * HTTP.
			 */
			HTTP("http"),
			/**
			 * HTTPS.
			 */
			HTTPS("https");

			private final String scheme;

			Protocol(String scheme) {
				this.scheme = scheme;
			}

			String getScheme() {
				return this.scheme;
			}

		}
	}

}
