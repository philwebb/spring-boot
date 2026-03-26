/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.http.client;

/**
 * Exception thrown when a host was not matched by an {@link InetAddressMatcher}.
 *
 * @author Phillip Webb
 * @since 4.1.0
 */
public class UnmatchedHostException extends RuntimeException {

	private final String host;

	private final InetAddressMatcher matcher;

	UnmatchedHostException(String host, InetAddressMatcher matcher) {
		super("Unmatched host '%s'".formatted(host));
		this.host = host;
		this.matcher = matcher;
	}

	/**
	 * Return the host that was not matched.
	 * @return the unmatched host
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * Return the matcher that was used.
	 * @return the matcher that didn't match
	 */
	public InetAddressMatcher getMatcher() {
		return this.matcher;
	}

}
