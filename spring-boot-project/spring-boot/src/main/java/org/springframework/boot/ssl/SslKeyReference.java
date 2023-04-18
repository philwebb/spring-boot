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

/**
 * A reference to a single key obtained via {@link SslStoreBundle}.
 *
 * @author Phillip Webb
 * @since 3.1.0
 */
public interface SslKeyReference {

	/**
	 * {@link SslKeyReference} that returns no values.
	 */
	SslKeyReference NONE = of(null, null);

	/**
	 * Return the alias of the key.
	 * @return the key alias
	 */
	String getAlias();

	/**
	 * Return the password that should be used to access the key or {@code null} if no
	 * password is required.
	 * @return the key password
	 */
	String getPassword();

	/**
	 * Factory method to create a new {@link SslKeyReference} instance.
	 * @param alias the alias of the key
	 * @return a new {@link SslKeyReference} instance
	 */
	static SslKeyReference of(String alias) {
		return of(alias, null);
	}

	/**
	 * Factory method to create a new {@link SslKeyReference} instance.
	 * @param alias the alias of the key
	 * @param password the password used to access the key or {@code null}
	 * @return a new {@link SslKeyReference} instance
	 */
	static SslKeyReference of(String alias, String password) {
		return new SslKeyReference() {

			@Override
			public String getAlias() {
				return alias;
			}

			@Override
			public String getPassword() {
				return password;
			}

		};
	}

}
