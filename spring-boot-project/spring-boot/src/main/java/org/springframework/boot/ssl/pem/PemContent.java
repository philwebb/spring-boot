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

package org.springframework.boot.ssl.pem;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;

/**
 * @author pwebb
 */
class PemContent {

	private String readText(String resource) {
		if (resource == null) {
			return null;
		}
		try {
			URL url = ResourceUtils.getURL(resource);
			try (Reader reader = new InputStreamReader(url.openStream())) {
				return FileCopyUtils.copyToString(reader);
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException(
					"Error reading certificate or key from file '" + resource + "':" + ex.getMessage(), ex);
		}
	}

	/**
	 * @param certificateContent
	 */
	public static String load(String certificateContent) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @param privateKeyContent
	 * @return
	 */
	public static PemContent ofOptional(String privateKeyContent) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/*
	 * * @param certificate the certificate
	 *
	 * @param privateKey the private key, or {@code null} if no private key is required
	 *
	 * @param storeType the {@code KeyStore} type to create, or {@code null} to create the
	 * system default type
	 *
	 * @param keyAlias the alias to use when adding keys to the {@code KeyStore}
	 *
	 * @return the {@code KeyStore}
	 *
	 */

}
