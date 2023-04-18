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
 * A reference to a single key obtained via {@link SslStores}.
 *
 * @author Phillip Webb
 */
public final class SslKeyReference {

	public String getAlias() {
		return null;
	}

	public String getPassword() {
		return null;
	}

	/**
	 * @param keyAlias
	 * @param keyPassword
	 * @return
	 */
	public static SslKeyReference of(String keyAlias, String keyPassword) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
