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

package org.springframework.boot.grpc.server.autoconfigure;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import io.grpc.TlsServerCredentials.ClientAuth;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.Assert;

record ServerCredentials(KeyManagerFactory keyManager, TrustManagerFactory trustManager, ClientAuth clientAuth) {

	static ServerCredentials get(GrpcServerProperties properties, SslBundles bundles,
			TrustManagerFactory insecureTrustManager) {
		KeyManagerFactory keyManager = null;
		TrustManagerFactory trustManager = null;
		if (properties.getSsl().determineEnabled()) {
			String bundleName = properties.getSsl().getBundle();
			Assert.notNull(bundleName, () -> "SSL bundleName must not be null");
			SslBundle bundle = bundles.getBundle(bundleName);
			keyManager = bundle.getManagers().getKeyManagerFactory();
			trustManager = properties.getSsl().isSecure() ? bundle.getManagers().getTrustManagerFactory()
					: insecureTrustManager;
		}
		ClientAuth clientAuth = properties.getSsl().getClientAuth();
		return new ServerCredentials(keyManager, trustManager, clientAuth);
	}

}
