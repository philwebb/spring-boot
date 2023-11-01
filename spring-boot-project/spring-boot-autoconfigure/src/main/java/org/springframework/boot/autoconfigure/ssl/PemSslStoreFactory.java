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

package org.springframework.boot.autoconfigure.ssl;

import java.io.IOException;

import org.springframework.boot.ssl.pem.PemSslStore;

/**
 * Factory used to create a {@link PemSslStore} from store properties.
 *
 * @author Phillip Webb
 */
interface PemSslStoreFactory {

	/**
	 * Factory method used to get the {@link PemSslStore}.
	 * @param bundleName the bundle name
	 * @param storePropertyName the store property name
	 * @param storeProperties the store properties
	 * @return a {@link PemSslStore} instance
	 * @throws IOException on IO error
	 */
	PemSslStore getPemSslStore(String bundleName, String storePropertyName,
			PemSslBundleProperties.Store storeProperties) throws IOException;

}
