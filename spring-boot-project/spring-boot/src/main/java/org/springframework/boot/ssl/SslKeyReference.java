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

package org.springframework.boot.ssl;

import org.springframework.util.Assert;

/**
 * A reference to a single key obtained via {@link SslStores}.
 *
 * @author Phillip Webb
 */
public final class SslKeyReference {

	private final String alias;

	private final String password;

	private SslKeyReference(String alias, String password) {
		Assert.hasText(alias, "Alias must not be empty");
		this.alias = alias;
		this.password = password;
	}

	/**
	 * Return the alias of the key.
	 * @return the key alias
	 */
	public String getAlias() {
		return this.alias;
	}

	/**
	 * Return the password that should be used to access the key or {@code null} if no
	 * password is required.
	 * @return the key password
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * Create a new {@link SslKeyReference} instance.
	 * @param alias the alias of the key
	 * @param password the password used to access the key or {@code null}
	 * @return a new {@link SslKeyReference} instance
	 */
	public static SslKeyReference of(String alias, String password) {
		return new SslKeyReference(alias, password);
	}

}
